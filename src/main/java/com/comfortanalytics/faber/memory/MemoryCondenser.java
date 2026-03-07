package com.comfortanalytics.faber.memory;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface MemoryCondenser {

    @Nonnull
    MemoryMessage condense(@Nonnull List<MemoryMessage> messages) throws TimeoutException;
}

