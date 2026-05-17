package fr.univrennes.istic.l2gen.application.core.services.table;

public final class MergeJoinCondition {

    private final String leftColumnName;
    private final String leftColumnSqlName;
    private final String rightColumnName;
    private final String rightColumnSqlName;

    public MergeJoinCondition(String leftColumnName, String leftColumnSqlName,
            String rightColumnName, String rightColumnSqlName) {
        this.leftColumnName = leftColumnName;
        this.leftColumnSqlName = leftColumnSqlName;
        this.rightColumnName = rightColumnName;
        this.rightColumnSqlName = rightColumnSqlName;
    }

    public String getLeftColumnName() {
        return leftColumnName;
    }

    public String getLeftColumnSqlName() {
        return leftColumnSqlName;
    }

    public String getRightColumnName() {
        return rightColumnName;
    }

    public String getRightColumnSqlName() {
        return rightColumnSqlName;
    }
}