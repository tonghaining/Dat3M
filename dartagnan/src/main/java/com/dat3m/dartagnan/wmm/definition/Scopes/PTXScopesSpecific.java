package com.dat3m.dartagnan.wmm.definition.Scopes;

import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;

import java.util.ArrayList;
import java.util.Arrays;

public class PTXScopesSpecific extends Definition {
    private String specificScope;
    private final ArrayList<String> scopes = new ArrayList<>(
            Arrays.asList(Tag.PTX.SYS, Tag.PTX.GPU, Tag.PTX.CTA));
    public PTXScopesSpecific(Relation r, String specificScope) {
        super(r);
        this.specificScope = specificScope;

    }
    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitSameSpecificScope(definedRelation, this.scopes, this.specificScope);
    }
}