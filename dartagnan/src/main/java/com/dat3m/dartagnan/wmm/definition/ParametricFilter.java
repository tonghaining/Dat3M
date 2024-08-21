package com.dat3m.dartagnan.wmm.definition;

import com.dat3m.dartagnan.program.filter.Filter;
import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;

import static com.dat3m.dartagnan.wmm.RelationNameRepository.*;
import static com.google.common.base.Preconditions.checkNotNull;

public class ParametricFilter extends Definition {
    private final Filter parameterFilter;
    private final String parameterName;

    public ParametricFilter(Relation relation, String parameterName, Filter parameterObject) {
        super(relation, PARAMETRIC_FILTER);
        this.parameterName = checkNotNull(parameterName);
        this.parameterFilter = checkNotNull(parameterObject);
    }

    public Filter getParameterFilter() {
        return parameterFilter;
    }

    public String getParameterName() {
        return parameterName;
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitParametricFilter(this);
    }
}
