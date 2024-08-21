package com.dat3m.dartagnan.wmm.definition;

import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;

import static com.dat3m.dartagnan.wmm.RelationNameRepository.PARAMETRIC;
import static com.google.common.base.Preconditions.checkNotNull;

public class Parametric extends Definition {
    private final Object parameterFiller;
    private final String parameterName;
    private final boolean parameterIsRelation;

    public Parametric(Relation relation, String parameterName, Object parameterObject) {
        super(relation, PARAMETRIC);
        this.parameterName = checkNotNull(parameterName);
        this.parameterFiller = checkNotNull(parameterObject);
        this.parameterIsRelation = parameterObject instanceof Relation;
    }

    public Object getParameterFiller() {
        return parameterFiller;
    }

    public String getParameterName() {
        return parameterName;
    }

    public boolean isParameterRelation() {
        return parameterIsRelation;
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitParametric(this);
    }
}
