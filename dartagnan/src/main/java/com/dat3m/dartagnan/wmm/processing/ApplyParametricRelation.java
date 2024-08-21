package com.dat3m.dartagnan.wmm.processing;

import com.dat3m.dartagnan.program.filter.Filter;
import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.definition.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApplyParametricRelation implements WmmProcessor{
    private ApplyParametricRelation() {
    }

    public static WmmProcessor newInstance() {
        return new ApplyParametricRelation();
    }

    @Override
    public void run(Wmm wmm) {
        for (Relation rel : List.copyOf(wmm.getRelations())) {
            if (rel.getDefinition() instanceof ParametricCallRelation parametricCallRelation) {
                applyParametricRelation(wmm, parametricCallRelation);
            }
            if (rel.getDefinition() instanceof ParametricCallFilter parametricCallFilter) {
                applyParametricFilter(wmm, parametricCallFilter);
            }
        }
    }

    private void applyParametricRelation(Wmm wmm, ParametricCallRelation parametricCallRelation) {
        ParametricRelation parametricRelation = parametricCallRelation.getParametricRelation();
        Relation relationInput = parametricCallRelation.getRelationInput();
        Relation oldRelation = parametricRelation.getDefinedRelation();
        Relation oldParameter = parametricRelation.getParameterRelation();

    }

    private void applyParametricFilter(Wmm wmm, ParametricCallFilter parametricCallFilter) {
    }
}
