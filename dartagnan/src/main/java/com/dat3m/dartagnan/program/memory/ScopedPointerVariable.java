package com.dat3m.dartagnan.program.memory;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.Type;

public class ScopedPointerVariable extends ScopedPointer {
    private ScopedPointerVariable aggregateSource = null;

    public ScopedPointerVariable(String id, String scopeId, Type innerType, MemoryObject address) {
        super(id, scopeId, innerType, address);
    }

    public void setAggregateSource(ScopedPointerVariable aggregateSource) {
        this.aggregateSource = aggregateSource;
    }

    public ScopedPointerVariable getAggregateSource() {
        return aggregateSource;
    }

    @Override
    public MemoryObject getAddress() {
        return (MemoryObject) super.getAddress();
    }

    public void setInitialValue(int offset, Expression value) {
        getAddress().setInitialValue(offset, value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (aggregateSource == null ? 0 : aggregateSource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        ScopedPointerVariable other = (ScopedPointerVariable) obj;
        if (aggregateSource == null) {
            return other.aggregateSource == null;
        }
        return aggregateSource.equals(other.aggregateSource);
    }
}
