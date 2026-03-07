package com.comfortanalytics.faber.memory;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.List;

@FunctionalInterface
public interface MemoryTokenEstimator {

    int estimate(@Nonnull List<MemoryMessage> messages);
}

