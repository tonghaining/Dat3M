package com.dat3m.dartagnan.wmm.definition;

import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;

import static com.dat3m.dartagnan.wmm.RelationNameRepository.PARAMETRIC_CALL;
import static com.google.common.base.Preconditions.checkNotNull;

public class ParametricCall extends Definition {
    private Parametric parametric;
    private Object parameter;

    public ParametricCall(Relation relation, Parametric parametric, Object parameter) {
        super(relation, PARAMETRIC_CALL);
        this.parametric = checkNotNull(parametric);
        this.parameter = checkNotNull(parameter);
    }

    public Parametric getParametric() {
        return parametric;
    }

    public Object getParameter() {
        return parameter;
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitParametricCall(this);
    }

//    @Override
//    public List<Relation> getConstrainedRelations() {
//        return List.of(definedRelation, parameterRelation);
//    }
}
