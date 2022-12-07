package com.github.senicko.lox;

public record Token(TokenType type, String lexeme, Object literal, int line) {
}



