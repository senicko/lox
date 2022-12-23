package com.github.senicko.lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (LoxRuntimeError error) {
            Main.runtimeError(error);
        }
    }

    // This method is used in REPL to display resulting values of raw expressions.
    void printExpression(Stmt.Expression expr) {
        Object value = evaluate(expr.expression);
        System.out.println(value);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if(expr.operator.type() == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else if (expr.operator.type() == TokenType.AND) {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        return switch (expr.operator.type()) {
            case MINUS -> {
                assertNumberOperand(expr.operator, right);
                yield -(double) right;
            }
            case BANG -> !isTruthy(right);

            // Unreachable
            default -> null;
        };
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        return switch (expr.operator.type()) {
            case PLUS -> {
                if (left instanceof Double lh && right instanceof Double rh)
                    yield lh + rh;

                if (left instanceof String lh && right instanceof String rh)
                    yield lh + rh;

                if (left instanceof String lh)
                    yield lh + this.stringify(right);

                if (right instanceof String rh)
                    yield this.stringify(left) + rh;

                throw new LoxRuntimeError(expr.operator, "Operands must be either Strings or Numbers.");
            }
            case MINUS -> {
                assertNumberOperands(expr.operator, left, right);
                yield (Double) left - (Double) right;
            }
            case SLASH -> {
                assertNumberOperands(expr.operator, left, right);

                if ((Double) right == 0)
                    throw new LoxRuntimeError(expr.operator, "Attempt to divide by zero.");

                yield (Double) left / (Double) right;
            }
            case STAR -> {
                assertNumberOperands(expr.operator, left, right);
                yield (Double) left * (Double) right;
            }
            case GREATER -> {
                assertNumberOperands(expr.operator, left, right);
                yield (Double) left > (Double) right;
            }
            case GREATER_EQUAL -> {
                assertNumberOperands(expr.operator, left, right);
                yield (Double) left >= (Double) right;
            }
            case LESS -> {
                assertNumberOperands(expr.operator, left, right);
                yield (Double) left < (Double) right;
            }
            case LESS_EQUAL -> {
                assertNumberOperands(expr.operator, left, right);
                yield (Double) left <= (Double) right;
            }
            case SPACESHIP -> {
                if (left instanceof String lh && right instanceof String rh)
                    yield lh.compareTo(rh);

                if (left instanceof Double lh && right instanceof Double rh)
                    yield lh.compareTo(rh);

                throw new LoxRuntimeError(expr.operator, "Operands must be either Strings or Numbers.");
            }
            case BANG_EQUAL -> !isEqual(left, right);
            case EQUAL_EQUAL -> isEqual(left, right);

            // Unreachable
            default -> null;
        };
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object condition = evaluate(expr.condition);

        if (isTruthy(condition)) return evaluate(expr.truthy);
        return evaluate(expr.falsy);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name).value;
    }

    @Override
    public Object visitAssignmentExpr(Expr.Assignment expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    private void assertNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new LoxRuntimeError(operator, "Operand must be a number.");
    }

    private void assertNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new LoxRuntimeError(operator, "Operands must be numbers.");
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;

        try {
            this.environment = environment;

            for(Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;

        if(stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme(), value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }

        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null & b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }
}
