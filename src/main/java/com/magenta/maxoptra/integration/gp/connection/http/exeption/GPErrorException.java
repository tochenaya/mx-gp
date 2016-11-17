package com.magenta.maxoptra.integration.gp.connection.http.exeption;

import java.io.IOException;

public class GPErrorException extends IOException{
    public GPErrorException(String message){
        super(message);
    }
}
