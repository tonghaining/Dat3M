package com.dat3m.dartagnan.wmm.definition;

import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;

import java.util.List;

import static com.dat3m.dartagnan.wmm.RelationNameRepository.PARAMETRIC_CALL_RELATION;
import static com.google.common.base.Preconditions.checkNotNull;

public class ParametricCallRelation extends Definition {
    private final ParametricRelation parametricRelation;
    private final Relation relationInput;

    public ParametricCallRelation(Relation relation, ParametricRelation parametricRelation, Relation parameter) {
        super(relation, PARAMETRIC_CALL_RELATION);
        this.parametricRelation = checkNotNull(parametricRelation);
        this.relationInput = checkNotNull(parameter);
    }

    public ParametricRelation getParametricRelation() {
        return parametricRelation;
    }

    public Relation getRelationInput() {
        return relationInput;
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitParametricCallRelation(this);
    }

    @Override
    public List<Relation> getConstrainedRelations() {
        return List.of(definedRelation, relationInput);
    }
}
