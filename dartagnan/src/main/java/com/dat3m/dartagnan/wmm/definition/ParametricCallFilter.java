package com.dat3m.dartagnan.wmm.definition;

import com.dat3m.dartagnan.program.filter.Filter;
import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;

import static com.dat3m.dartagnan.wmm.RelationNameRepository.PARAMETRIC_CALL_FILTER;
import static com.google.common.base.Preconditions.checkNotNull;

public class ParametricCallFilter extends Definition {
    private final ParametricFilter parametricFilter;
    private final Filter filterInput;

    public ParametricCallFilter(Relation relation, ParametricFilter parametricFilter, Filter parameter) {
        super(relation, PARAMETRIC_CALL_FILTER);
        this.parametricFilter = checkNotNull(parametricFilter);
        this.filterInput = checkNotNull(parameter);
    }

    public ParametricFilter getParametricFilter() {
        return parametricFilter;
    }

    public Object getFilterInput() {
        return filterInput;
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitParametricCallFilter(this);
    }
}
