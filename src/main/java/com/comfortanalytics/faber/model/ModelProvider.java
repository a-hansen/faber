package com.comfortanalytics.faber.model;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.concurrent.TimeoutException;

public interface ModelProvider {

    @Nonnull
    String providerId();

    @Nonnull
    String generate(@Nonnull String prompt) throws TimeoutException;
}

