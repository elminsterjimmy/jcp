package com.elminster.jcp.ast.expression.literal;

public interface DoubleLiteral extends Literal<Double> {

    static DoubleLiteral of(Double value) {
        return () -> value;
    }
}
