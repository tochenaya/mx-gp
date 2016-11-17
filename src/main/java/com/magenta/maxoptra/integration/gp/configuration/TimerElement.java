package com.magenta.maxoptra.integration.gp.configuration;

public class TimerElement {
    public String second;
    public String minute;
    public String hour;
    public String month;
    public String year;

    @Override
    public String toString() {
        return "TimerElement{" +
                "second='" + second + '\'' +
                ", minute='" + minute + '\'' +
                ", hour='" + hour + '\'' +
                ", month='" + month + '\'' +
                ", year='" + year + '\'' +
                '}';
    }
}
