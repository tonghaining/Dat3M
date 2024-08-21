package com.dat3m.dartagnan.wmm.definition;

import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;

import java.util.List;

import static com.dat3m.dartagnan.wmm.RelationNameRepository.PARAMETRIC_RELATION;
import static com.google.common.base.Preconditions.checkNotNull;

public class ParametricRelation extends Definition {
    private final Relation parameterRelation;
    private final String parameterName;

    public ParametricRelation(Relation relation, String parameterName, Relation parameterObject) {
        super(relation, PARAMETRIC_RELATION);
        this.parameterName = checkNotNull(parameterName);
        this.parameterRelation = checkNotNull(parameterObject);
    }

    public Relation getParameterRelation() {
        return parameterRelation;
    }

    public String getParameterName() {
        return parameterName;
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitParametricRelation(this);
    }

    @Override
    public List<Relation> getConstrainedRelations() {
        return List.of(definedRelation, parameterRelation);
    }
}
