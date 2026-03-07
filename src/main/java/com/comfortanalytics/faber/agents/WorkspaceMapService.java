package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            "public\\s+(?:final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)?(class|interface|record|enum)\\s+(\\w+)");
    private static final Pattern RECORD_PATTERN = Pattern.compile(
            "public\\s+(?:final\\s+)?record\\s+(\\w+)\\s*\\((.*?)\\)\\s*\\{",
            Pattern.DOTALL);
    private static final Pattern PUBLIC_MEMBER_PATTERN = Pattern.compile(
            "public\\s+[^{;=]+?\\([^{;]*\\)\\s*(?:throws\\s+[^{;]+)?(?=[{;])",
            Pattern.DOTALL);
    private static final List<String> EXCLUDED_DIRECTORY_NAMES = List.of(
            "build",
            ".gradle",
            ".idea",
            ".git",
            "out",
            "target");

    private final Path rootPath;

    public WorkspaceMapService(@Nonnull Path rootPath) {
        this.rootPath = Objects.requireNonNull(rootPath, "rootPath").toAbsolutePath().normalize();
    }

    @Nonnull
    public String loadWorkspaceMap() {
        try (Stream<Path> pathStream = Files.walk(rootPath)) {
            Map<String, List<TypeIndexEntry>> entriesByPackage = new LinkedHashMap<>();
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !isExcluded(path))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> indexFile(entriesByPackage, path));
            return format(entriesByPackage);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan workspace for Java source files under: " + rootPath, e);
        }
    }

    private void indexFile(Map<String, List<TypeIndexEntry>> entriesByPackage, Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String packageName = extractPackageName(content);
            Matcher typeMatcher = TYPE_PATTERN.matcher(content);
            if (!typeMatcher.find()) {
                return;
            }
            String kind = typeMatcher.group(1);
            String typeName = typeMatcher.group(2);
            ArrayList<String> signatures = new ArrayList<>();
            addRecordMembers(content, typeName, signatures);
            addPublicMembers(content, typeName, signatures);
            signatures.sort(String::compareTo);
            entriesByPackage
                    .computeIfAbsent(packageName, ignored -> new ArrayList<>())
                    .add(new TypeIndexEntry(typeName, kind, List.copyOf(signatures)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Java source file: " + path, e);
        }
    }

    private String extractPackageName(String content) {
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
        return packageMatcher.find() ? packageMatcher.group(1) : "<default>";
    }

    private void addRecordMembers(String content, String typeName, List<String> signatures) {
        Matcher recordMatcher = RECORD_PATTERN.matcher(content);
        if (!recordMatcher.find() || !typeName.equals(recordMatcher.group(1))) {
            return;
        }
        String componentBlock = recordMatcher.group(2).replaceAll("\\s+", " ").trim();
        if (!componentBlock.isEmpty()) {
            signatures.add("public " + typeName + "(" + componentBlock + ")");
        }
        for (String component : splitRecordComponents(componentBlock)) {
            String normalizedComponent = stripAnnotations(component).trim();
            Matcher componentMatcher = Pattern.compile("(.+?)\\s+(\\w+)$").matcher(normalizedComponent);
            if (componentMatcher.find()) {
                signatures.add("public " + componentMatcher.group(1).trim() + " " + componentMatcher.group(2) + "()");
            }
        }
    }

    private void addPublicMembers(String content, String typeName, List<String> signatures) {
        Matcher memberMatcher = PUBLIC_MEMBER_PATTERN.matcher(content);
        while (memberMatcher.find()) {
            String signature = normalizeSignature(memberMatcher.group());
            if (signature.startsWith("public class")
                    || signature.startsWith("public interface")
                    || signature.startsWith("public record")
                    || signature.startsWith("public enum")) {
                continue;
            }
            if (signature.equals("public " + typeName + "()")) {
                signatures.add(signature);
                continue;
            }
            if (!signatures.contains(signature)) {
                signatures.add(signature);
            }
        }
    }

    private boolean isExcluded(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        for (Path part : rootPath.relativize(normalized)) {
            if (EXCLUDED_DIRECTORY_NAMES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private String format(Map<String, List<TypeIndexEntry>> entriesByPackage) {
        if (entriesByPackage.isEmpty()) {
            return "No Java source types found in workspace.";
        }
        StringBuilder builder = new StringBuilder(entriesByPackage.size() * 128);
        entriesByPackage.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    builder.append("Package ")
                            .append(entry.getKey())
                            .append(System.lineSeparator());
                    entry.getValue().stream()
                            .sorted(Comparator.comparing(TypeIndexEntry::typeName))
                            .forEach(typeEntry -> {
                                builder.append("  - ")
                                        .append(typeEntry.typeName())
                                        .append(" (")
                                        .append(typeEntry.kind())
                                        .append(")")
                                        .append(System.lineSeparator());
                                for (String signature : typeEntry.signatures()) {
                                    builder.append("    • ")
                                            .append(signature)
                                            .append(System.lineSeparator());
                                }
                            });
                    builder.append(System.lineSeparator());
                });
        return builder.toString().stripTrailing();
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

    private record TypeIndexEntry(String typeName, String kind, List<String> signatures) {
    }
}

