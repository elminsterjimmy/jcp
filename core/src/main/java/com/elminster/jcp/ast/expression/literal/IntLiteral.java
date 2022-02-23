package com.elminster.jcp.ast.expression.literal;

public interface IntLiteral extends Literal<Integer> {

    static IntLiteral of(Integer value) {
        return () -> value;
    }
}
