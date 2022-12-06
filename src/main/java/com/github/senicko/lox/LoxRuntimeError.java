package com.github.senicko.lox;

public class LoxRuntimeError extends RuntimeException {
    final Token token;

    LoxRuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
