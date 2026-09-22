package com.dat3m.dartagnan.wmm.analysis;

import com.dat3m.dartagnan.wmm.Relation;

import java.util.Map;

/**
 * Immutable snapshot of {@link RelationAnalysis} knowledge (may/must sets) as computed directly
 * from the relation definitions of the program encoding, i.e. before any extended analysis
 * back-propagates information from the memory model's constraints (axioms) into the may/must
 * sets. Captured right after {@link RelationAnalysis#run()} and before
 * {@link RelationAnalysis#runExtended()} mutates the underlying knowledge in place.
 */
public final class RawRelationAnalysis {

    private final Map<Relation, RelationAnalysis.Knowledge> knowledgeMap;

    RawRelationAnalysis(Map<Relation, RelationAnalysis.Knowledge> knowledgeMap) {
        this.knowledgeMap = knowledgeMap;
    }

    public RelationAnalysis.Knowledge getKnowledge(Relation relation) {
        return knowledgeMap.get(relation);
    }
}
