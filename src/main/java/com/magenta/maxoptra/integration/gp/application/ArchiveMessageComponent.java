package com.magenta.maxoptra.integration.gp.application;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class ArchiveMessageComponent {
    private StringBuffer message = new StringBuffer();

    public void add(String line) {
        message.append("\n-----------------\n");
        message.append(line);
        message.append("\n-----------------\n");
    }

    public String getMessage() {
        return message.toString();
    }
}
