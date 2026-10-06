package com.diegogiron.biblioteca_catalogo.exception;

public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException(String email) {
        super("El email ya esta registrado: " + email);
    }
}
