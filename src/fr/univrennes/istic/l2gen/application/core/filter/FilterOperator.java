package fr.univrennes.istic.l2gen.application.core.filter;

public enum FilterOperator {
    EQUAL,
    NOT_EQUAL,
    GREATER,
    GREATER_EQUAL,
    LESS,
    LESS_EQUAL,
    LIKE,
    IS_NULL,
    NOT_NULL;

    public String getSQL() {
        return switch (this) {
            case EQUAL -> "=";
            case NOT_EQUAL -> "!=";
            case GREATER -> ">";
            case GREATER_EQUAL -> ">=";
            case LESS -> "<";
            case LESS_EQUAL -> "<=";
            case LIKE -> "LIKE";
            case IS_NULL -> "IS NULL";
            case NOT_NULL -> "IS NOT NULL";
        };
    }
}