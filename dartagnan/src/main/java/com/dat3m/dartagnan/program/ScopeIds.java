package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.configuration.Arch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScopeIds {

    private final ScopeHierarchy scopeHierarchy;
    private final List<Integer> ids;

    public ScopeIds(Arch arch, List<Integer> ids) {
        this.scopeHierarchy = new ScopeHierarchy(arch);
        if (ids.size() != scopeHierarchy.getScopes().size()) {
            throw new IllegalArgumentException("Scope ids must be of length " + scopeHierarchy.getScopes().size());
        }
        this.ids = new ArrayList<>(ids);
    }

    public List<String> getScopes() {
        return new ArrayList<>(scopeHierarchy.getScopes());
    }

    public int getScopeId(String scope) {
        int index = scopeHierarchy.getScopeLevel(scope);
        return ids.get(index);
    }

    // For any scope higher than the given one, we check both threads have the same scope id.
    public boolean canSyncAtScope(ScopeIds other, String scope) {
        if (!this.getScopes().contains(scope)) {
            return false;
        }

        List<String> scopes = this.getScopes();
        int validIndex = scopes.indexOf(scope);
        // scopes(0) is highest in hierarchy
        // i = 0 is global, every thread will always have the same id, so start from i = 1
        for (int i = 1; i <= validIndex; i++) {
            if (!atSameScopeId(other, scopes.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean atSameScopeId(ScopeIds other, String scope) {
        int thisId = this.getScopeId(scope);
        int otherId = other.getScopeId(scope);
        return (thisId == otherId && thisId != -1);
    }

    @Override
    public String toString() {
        return scopeHierarchy.getScopes().stream()
                .map(scope -> scope + ":" + ids.get(scopeHierarchy.getScopes().indexOf(scope)))
                .collect(Collectors.joining(",", "[", "]"));
    }
}
