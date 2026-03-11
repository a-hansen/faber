package com.comfortanalytics.faber;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.cli.FaberCli;

public final class Main {

    private Main() {
    }

    public static void main(@Nonnull String[] args) {
        FaberCli.main(args);
    }
}

