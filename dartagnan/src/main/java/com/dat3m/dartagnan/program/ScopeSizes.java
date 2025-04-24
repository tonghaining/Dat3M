package com.dat3m.dartagnan.program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScopeSizes {

    protected final ScopeHierarchy scopeHierarchy;
    protected final List<Integer> sizes;

    public ScopeSizes(ScopeHierarchy scopeHierarchy, List<Integer> sizes) {
        this.scopeHierarchy = scopeHierarchy;
        if (sizes.size() != this.scopeHierarchy.scopes().size()) {
            throw new IllegalArgumentException("Scope sizes must be of length " + this.scopeHierarchy.scopes().size());
        }
        this.sizes = sizes;
    }

    private int getSizeAtScope(int index) {
        int container = 1;
        for (int i = index; i < scopeHierarchy.scopes().size(); i++) {
            container *= sizes.get(i);
        }
        return container;
    }

    private int getIdAtScope(int tid, int index) {
        int sizeAtScope = getSizeAtScope(index);
        int sizeAtHigherScope = index == 0 ? getSizeAtScope(0) + 1 : getSizeAtScope(index - 1);
        return (tid % sizeAtHigherScope) / sizeAtScope;
    }

    public int getSize(String scope) {
        int index = scopeHierarchy.getScopeLevel(scope);
        return getSizeAtScope(index);
    }

    public int getId(String scope, int tid) {
        int index = scopeHierarchy.getScopeLevel(scope);
        return getIdAtScope(tid, index);
    }

    public ScopeIds getScopeIds(int tid) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < scopeHierarchy.scopes().size(); i++) {
            ids.add(getIdAtScope(tid, i));
        }
        return new ScopeIds(scopeHierarchy, ids);
    }

    // Sizes of the scopes in the hierarchy
    public int totalSize() {
        return sizes.stream().reduce(1, (a, b) -> a * b);
    }

    public int dvSize() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int qfSize() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int wgSize() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int sgSize() {
        throw new UnsupportedOperationException("Not implemented");
    }

    // Ids of the scopes in the hierarchy
    public int dvId(int tid) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int qfId(int tid) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int wgId(int tid) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int sgId(int tid) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int thId(int tid) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
