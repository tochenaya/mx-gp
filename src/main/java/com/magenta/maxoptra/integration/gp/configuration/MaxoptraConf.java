package com.magenta.maxoptra.integration.gp.configuration;

import com.magenta.maxoptra.integration.settings.MaxoptraApiSettings;

public class MaxoptraConf extends MaxoptraApiSettings {
    public String aoc;
    public String dateTimePattern;

    @Override
    public String toString() {
        return "MaxoptraConf{" +
                "aoc='" + aoc + '\'' +
                ", host=" + host +
                '}';
    }
}
