package com.dat3m.dartagnan.program;

import java.util.ArrayList;
import java.util.List;

public class ThreadGrid {

    protected final ScopeHierarchy scopeHierarchy;
    protected final List<Integer> sizes;

    public ThreadGrid(ScopeHierarchy scopeHierarchy, List<Integer> sizes) {
        this.scopeHierarchy = scopeHierarchy;
        if (sizes.size() != this.scopeHierarchy.scopes().size()) {
            throw new IllegalArgumentException("Scope sizes must be of length " + this.scopeHierarchy.scopes().size());
        }
        this.sizes = sizes;
    }

    public int getSize(int index) {
        return sizes.subList(index, scopeHierarchy.scopes().size())
                .stream()
                .reduce(1, (a, b) -> a * b);
    }

    public int getId(int tid, int index) {
        int sizeAtScope = index == scopeHierarchy.scopes().size() ? 1 : getSize(index);
        int sizeAtHigherScope = index == 0 ? getSize(0) + 1 : getSize(index - 1);
        return (tid % sizeAtHigherScope) / sizeAtScope;
    }

    public ScopeIds getIds(int tid) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < scopeHierarchy.scopes().size(); i++) {
            ids.add(getId(tid, i));
        }
        return new ScopeIds(scopeHierarchy, ids);
    }
}
