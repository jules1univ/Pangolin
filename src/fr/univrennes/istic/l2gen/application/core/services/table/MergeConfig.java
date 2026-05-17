package fr.univrennes.istic.l2gen.application.core.services.table;

import java.util.List;

public final class MergeConfig {

    private final MergeJoinType joinType;
    private final List<MergeJoinCondition> joinConditions;
    private final String resultName;

    public MergeConfig(MergeJoinType joinType, List<MergeJoinCondition> joinConditions, String resultName) {
        this.joinType = joinType;
        this.joinConditions = List.copyOf(joinConditions);
        this.resultName = resultName;
    }

    public MergeJoinType getJoinType() {
        return joinType;
    }

    public List<MergeJoinCondition> getJoinConditions() {
        return joinConditions;
    }

    public String getResultName() {
        return resultName;
    }
}