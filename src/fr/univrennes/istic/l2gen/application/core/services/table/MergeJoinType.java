package fr.univrennes.istic.l2gen.application.core.services.table;

public enum MergeJoinType {
    INNER("INNER JOIN"),
    LEFT("LEFT JOIN"),
    RIGHT("RIGHT JOIN"),
    FULL_OUTER("FULL OUTER JOIN"),
    UNION("UNION ALL");

    private final String sql;

    MergeJoinType(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public String toString() {
        return switch (this) {
            case INNER -> "INNER JOIN";
            case LEFT -> "LEFT JOIN";
            case RIGHT -> "RIGHT JOIN";
            case FULL_OUTER -> "FULL OUTER JOIN";
            case UNION -> "UNION";
        };
    }
}