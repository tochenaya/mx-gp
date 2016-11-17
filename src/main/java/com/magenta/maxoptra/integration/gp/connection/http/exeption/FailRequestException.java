package com.magenta.maxoptra.integration.gp.connection.http.exeption;

import java.io.IOException;

public class FailRequestException extends IOException {
    public FailRequestException(){}

    public FailRequestException(String message){
        super(message);
    }
}
