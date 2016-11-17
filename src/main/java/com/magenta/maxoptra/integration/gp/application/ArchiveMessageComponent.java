package com.magenta.maxoptra.integration.gp.application;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class ArchiveMessageComponent {
    private String message;

    public void add(String line1, String line2, String line3) {
        message = "\n-----------------\n" +
                line1 + "\n" +
                line2 + "\n" +
                line3 + "\n" +
                "-----------------\n";
    }

    public void add(String line1, String line2) {
        message = "\n-----------------\n" +
                line1 + "\n" +
                line2 + "\n" +
                "-----------------\n";
    }

    public void add(String line1) {
        message  = "\n-----------------\n" +
                line1 + "\n" +
                "-----------------\n";
    }

    public String getMessage() {
        return message;
    }
}
