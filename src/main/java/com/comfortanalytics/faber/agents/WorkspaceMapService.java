package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class WorkspaceMapService {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^package\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "public\\s+(?:(?:static|final|abstract|sealed|non-sealed)\\s+)*(class|interface|record|enum)\\s+(\\w+)");
    private static final Pattern PUBLIC_MEMBER_PATTERN = Pattern.compile(
            "public\\s+[^{;=]+?\\([^{;]*\\)\\s*(?:throws\\s+[^{;]+)?(?=[{;])",
            Pattern.DOTALL);
    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    private final Path rootPath;
    private final Path sourceRoot;
    private CachedWorkspaceMap cachedWorkspaceMap;

    public WorkspaceMapService(@Nonnull Path rootPath) {
        this.rootPath = Objects.requireNonNull(rootPath, "rootPath").toAbsolutePath().normalize();
        this.sourceRoot = this.rootPath.resolve(SOURCE_ROOT).normalize();
    }

    @Nonnull
    public synchronized String loadWorkspaceMap() {
        WorkspaceSnapshot snapshot = snapshotWorkspace();
        if (cachedWorkspaceMap != null && cachedWorkspaceMap.snapshot().equals(snapshot)) {
            return cachedWorkspaceMap.workspaceMap();
        }
        Map<String, List<TypeIndexEntry>> entriesByPackage = new LinkedHashMap<>();
        for (FileSnapshot fileSnapshot : snapshot.files()) {
            indexFile(entriesByPackage, fileSnapshot.path());
        }
        String workspaceMap = format(entriesByPackage);
        cachedWorkspaceMap = new CachedWorkspaceMap(snapshot, workspaceMap);
        return workspaceMap;
    }

    private WorkspaceSnapshot snapshotWorkspace() {
        if (!Files.isDirectory(sourceRoot)) {
            return new WorkspaceSnapshot(List.of());
        }
        try (Stream<Path> pathStream = Files.walk(sourceRoot)) {
            List<FileSnapshot> files = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path -> sourceRoot.relativize(path).toString()))
                    .map(this::toFileSnapshot)
                    .toList();
            return new WorkspaceSnapshot(files);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan workspace for Java source files under: " + sourceRoot, e);
        }
    }

    private FileSnapshot toFileSnapshot(Path path) {
        try {
            Path normalized = path.toAbsolutePath().normalize();
            FileTime lastModifiedTime = Files.getLastModifiedTime(normalized);
            return new FileSnapshot(normalized, lastModifiedTime.toMillis(), Files.size(normalized));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Java source file metadata: " + path, e);
        }
    }

    private void indexFile(Map<String, List<TypeIndexEntry>> entriesByPackage, Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String packageName = extractPackageName(content);
            List<TypeDeclaration> typeDeclarations = extractTypeDeclarations(content);
            if (typeDeclarations.isEmpty()) {
                return;
            }
            List<TypeIndexEntry> packageEntries = entriesByPackage.computeIfAbsent(
                    packageName,
                    ignored -> new ArrayList<>(typeDeclarations.size()));
            for (TypeDeclaration typeDeclaration : typeDeclarations) {
                ArrayList<String> signatures = new ArrayList<>();
                addRecordMembers(typeDeclaration, signatures);
                addPublicMembers(content, typeDeclaration, signatures);
                signatures.sort(String::compareTo);
                packageEntries.add(new TypeIndexEntry(typeDeclaration.displayName(), typeDeclaration.kind(), List.copyOf(signatures)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Java source file: " + path, e);
        }
    }

    private List<TypeDeclaration> extractTypeDeclarations(String content) {
        Matcher typeMatcher = TYPE_PATTERN.matcher(content);
        ArrayList<TypeDeclaration> declarations = new ArrayList<>();
        while (typeMatcher.find()) {
            int bodyStart = content.indexOf('{', typeMatcher.end());
            if (bodyStart < 0) {
                continue;
            }
            int bodyEnd = findMatchingBrace(content, bodyStart);
            if (bodyEnd < 0) {
                continue;
            }
            declarations.add(new TypeDeclaration(
                    typeMatcher.group(2),
                    typeMatcher.group(1),
                    typeMatcher.start(),
                    bodyStart,
                    bodyEnd,
                    content.substring(typeMatcher.start(), bodyStart)));
        }
        if (declarations.isEmpty()) {
            return List.of();
        }

        ArrayList<TypeDeclaration> resolved = new ArrayList<>(declarations.size());
        ArrayList<TypeDeclaration> parentStack = new ArrayList<>();
        for (TypeDeclaration declaration : declarations) {
            while (!parentStack.isEmpty() && declaration.startIndex() > parentStack.get(parentStack.size() - 1).bodyEndIndex()) {
                parentStack.remove(parentStack.size() - 1);
            }
            String displayName = parentStack.isEmpty()
                    ? declaration.typeName()
                    : parentStack.get(parentStack.size() - 1).displayName() + "." + declaration.typeName();
            TypeDeclaration resolvedDeclaration = declaration.withDisplayName(displayName);
            resolved.add(resolvedDeclaration);
            parentStack.add(resolvedDeclaration);
        }
        return resolved;
    }

    private String extractPackageName(String content) {
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
        return packageMatcher.find() ? packageMatcher.group(1) : "<default>";
    }

    private void addRecordMembers(TypeDeclaration typeDeclaration, List<String> signatures) {
        if (!"record".equals(typeDeclaration.kind())) {
            return;
        }
        String componentBlock = extractRecordComponentBlock(typeDeclaration.header());
        if (componentBlock.isBlank()) {
            return;
        }
        signatures.add("public " + typeDeclaration.typeName() + "(" + componentBlock + ")");
        for (String component : splitRecordComponents(componentBlock)) {
            String normalizedComponent = stripAnnotations(component).trim();
            Matcher componentMatcher = Pattern.compile("(.+?)\\s+(\\w+)$").matcher(normalizedComponent);
            if (componentMatcher.find()) {
                signatures.add("public " + componentMatcher.group(1).trim() + " " + componentMatcher.group(2) + "()");
            }
        }
    }

    private void addPublicMembers(String content, TypeDeclaration typeDeclaration, List<String> signatures) {
        String typeBody = content.substring(typeDeclaration.bodyStartIndex() + 1, typeDeclaration.bodyEndIndex());
        Matcher memberMatcher = PUBLIC_MEMBER_PATTERN.matcher(typeBody);
        while (memberMatcher.find()) {
            if (braceDepthAt(typeBody, memberMatcher.start()) != 0) {
                continue;
            }
            String signature = normalizeSignature(memberMatcher.group());
            if (signature.startsWith("public class")
                    || signature.startsWith("public interface")
                    || signature.startsWith("public record")
                    || signature.startsWith("public enum")) {
                continue;
            }
            if (!signatures.contains(signature)) {
                signatures.add(signature);
            }
        }
    }

    private String format(Map<String, List<TypeIndexEntry>> entriesByPackage) {
        if (entriesByPackage.isEmpty()) {
            return "No Java source types found in workspace.";
        }
        StringBuilder builder = new StringBuilder(entriesByPackage.size() * 96);
        entriesByPackage.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    builder.append("pkg ")
                            .append(entry.getKey())
                            .append(System.lineSeparator());
                    entry.getValue().stream()
                            .sorted(Comparator.comparing(TypeIndexEntry::typeName))
                            .forEach(typeEntry -> {
                                builder.append("- ")
                                        .append(typeEntry.typeName())
                                        .append("[")
                                        .append(typeEntry.kind())
                                        .append("]");
                                if (!typeEntry.signatures().isEmpty()) {
                                    builder.append(": ")
                                            .append(String.join("; ", typeEntry.signatures()));
                                }
                                builder.append(System.lineSeparator());
                            });
                });
        return builder.toString().stripTrailing();
    }

    private static String extractRecordComponentBlock(String recordHeader) {
        int componentStart = recordHeader.indexOf('(');
        if (componentStart < 0) {
            return "";
        }
        int depth = 0;
        for (int index = componentStart; index < recordHeader.length(); index++) {
            char current = recordHeader.charAt(index);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return recordHeader.substring(componentStart + 1, index).replaceAll("\\s+", " ").trim();
                }
            }
        }
        return "";
    }

    private static int findMatchingBrace(String content, int openingBraceIndex) {
        int depth = 0;
        for (int index = openingBraceIndex; index < content.length(); index++) {
            char current = content.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static int braceDepthAt(String content, int limitExclusive) {
        int depth = 0;
        for (int index = 0; index < limitExclusive; index++) {
            char current = content.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth = Math.max(0, depth - 1);
            }
        }
        return depth;
    }

    private static String normalizeSignature(String rawSignature) {
        return rawSignature.replaceAll("\\s+", " ").trim();
    }

    private static String stripAnnotations(String rawComponent) {
        return rawComponent.replaceAll("@\\w+(?:\\([^)]*\\))?\\s*", "").trim();
    }

    private static List<String> splitRecordComponents(String componentBlock) {
        if (componentBlock.isBlank()) {
            return List.of();
        }
        ArrayList<String> components = new ArrayList<>();
        int start = 0;
        int depth = 0;
        for (int index = 0; index < componentBlock.length(); index++) {
            char current = componentBlock.charAt(index);
            if (current == '<') {
                depth++;
            } else if (current == '>') {
                depth = Math.max(0, depth - 1);
            } else if (current == ',' && depth == 0) {
                components.add(componentBlock.substring(start, index).trim());
                start = index + 1;
            }
        }
        components.add(componentBlock.substring(start).trim());
        return components;
    }

    private record CachedWorkspaceMap(WorkspaceSnapshot snapshot, String workspaceMap) {
    }

    private record WorkspaceSnapshot(List<FileSnapshot> files) {
    }

    private record FileSnapshot(Path path, long lastModifiedMillis, long size) {
    }

    private record TypeDeclaration(
            String typeName,
            String kind,
            int startIndex,
            int bodyStartIndex,
            int bodyEndIndex,
            String header,
            String displayName) {

        private TypeDeclaration(String typeName, String kind, int startIndex, int bodyStartIndex, int bodyEndIndex, String header) {
            this(typeName, kind, startIndex, bodyStartIndex, bodyEndIndex, header, typeName);
        }

        private TypeDeclaration withDisplayName(String value) {
            return new TypeDeclaration(typeName, kind, startIndex, bodyStartIndex, bodyEndIndex, header, value);
        }
    }

    private record TypeIndexEntry(String typeName, String kind, List<String> signatures) {
    }
}

