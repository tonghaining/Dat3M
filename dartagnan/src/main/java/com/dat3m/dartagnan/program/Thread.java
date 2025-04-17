package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.expression.type.FunctionType;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.core.Load;
import com.dat3m.dartagnan.program.event.core.MemoryCoreEvent;
import com.dat3m.dartagnan.program.event.core.Store;
import com.dat3m.dartagnan.program.event.core.threading.ThreadStart;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Thread extends Function {

    private final List<Integer> dimensionIds;

    // Threads that are system-synchronized-with this thread
    private final Optional<Set<Thread>> syncSet;

    public Thread(String name, FunctionType funcType, List<String> parameterNames, int id, ThreadStart entry) {
        super(name, funcType, parameterNames, id, entry);
        Preconditions.checkArgument(id >= 0, "Invalid thread ID");
        Preconditions.checkNotNull(entry, "Thread entry event must be not null");
        this.dimensionIds = List.of(0, 0);
        this.syncSet = Optional.empty();
    }

    public Thread(String name, FunctionType funcType, List<String> parameterNames, int id, ThreadStart entry,
                  List<Integer> dimensionIds, Set<Thread> syncSet) {
        super(name, funcType, parameterNames, id, entry);
        Preconditions.checkArgument(id >= 0, "Invalid thread ID");
        Preconditions.checkNotNull(entry, "Thread entry event must be not null");
        Preconditions.checkNotNull(dimensionIds, "Thread dimension IDs must be not null");
        Preconditions.checkNotNull(syncSet, "Thread syncSet must be not null");
        this.dimensionIds = dimensionIds;
        this.syncSet = Optional.of(syncSet);
    }

    public boolean hasScope() {
        return dimensionIds.size() > 2;
    }

    public boolean hasSyncSet() {
        return syncSet.isPresent();
    }

    public boolean canSyncAtScope(Thread other, String scope) {
        if (this.dimensionIds.size() != other.dimensionIds.size()) {
            return false;
        }
        ScopeNames scopeNames = this.getProgram().getScopeNames();
        if (!scopeNames.getScopes().contains(scope)) {
            return false;
        }
        int index = scopeNames.getScopes().indexOf(scope);
        for (int i = index; i < this.dimensionIds.size(); i++) {
            if (this.dimensionIds.get(i) != other.dimensionIds.get(i)) {
                return false;
            }
        }
        return true;
    }

    public int getDimensionId(String scope) {
        ScopeNames scopeNames = this.getProgram().getScopeNames();
        if (!scopeNames.getScopes().contains(scope)) {
            return -1;
        }
        int index = scopeNames.getScopes().indexOf(scope);
        if (index >= this.dimensionIds.size()) {
            return -1;
        }
        return this.dimensionIds.get(index);
    }

    public Set<Thread> getSyncSet() {
        return syncSet.get();
    }

    @Override
    public ThreadStart getEntry() {
        return (ThreadStart) entry;
    }

    @Override
    public String toString() {
//        ScopeNames scopeNames = this.getProgram().getScopeNames();
//        List<String> dimensionNames = scopeNames.getScopes();
//        List<String> nameContainer = List.of();
//        for (int i = 0; i < dimensionNames.size(); i ++) {
//            String scopeAndName = dimensionNames.get(i) + ":" + dimensionIds.get(i);
//            if (i < dimensionNames.size() - 1) {
//                scopeAndName += ",";
//            }
//            nameContainer.add(scopeAndName);
//        }
//        String dimensionName = String.join("", nameContainer);
//        return String.format("T%d:%s", id, name + "@" + dimensionName);
        return String.format("T%d:%s", id, name);
    }

    public List<MemoryCoreEvent> getSpawningEvents() {
        final ThreadStart start = getEntry();
        if (!start.isSpawned()) {
            return List.of();
        }
        
        Event cur = start;
        while (!(cur instanceof Load startLoad)) { cur = cur.getSuccessor(); }
        cur = start.getCreator();
        while (!(cur instanceof Store startStore)) { cur = cur.getPredecessor(); }
        
        assert startStore.getAddress().equals(startLoad.getAddress());
        
        return List.of(startLoad, startStore);
    }
}
