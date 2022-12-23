package com.github.senicko.lox;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

class Value {
    static class Initialized extends Value {
        Object value;

        Initialized(Object value) {
            this.value = value;
        }
    }

    public static class Uninitialized extends Value {
    }
}

public class Environment {
    final Environment enclosing;
    private final Map<String, Value> values = new HashMap<>();

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        if (value == null) {
            values.put(name, new Value.Uninitialized());
            return;
        }

        values.put(name, new Value.Initialized(value));
    }

    Value.Initialized get(@NotNull Token name) {
        Value value = null;

        if (values.containsKey(name.lexeme())) value = values.get(name.lexeme());
        else if (enclosing != null) value = enclosing.get(name);


        if (value == null) throw new LoxRuntimeError(name, "Undefined variable '" + name.lexeme() + "'.");

        if (value instanceof Value.Uninitialized) throw new LoxRuntimeError(name, "Use of uninitialized variable.");
        else return (Value.Initialized) value;
    }

    void assign(@NotNull Token name, Object value) {
        if (values.containsKey(name.lexeme())) {
            values.put(name.lexeme(), new Value.Initialized(value));
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new LoxRuntimeError(name, "Undefined variable '" + name.lexeme() + "'.");
    }
}
