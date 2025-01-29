package com.dat3m.dartagnan.program.memory;

public class ElementPointerVariable extends ScopedPointerVariable {
    private final ScopedPointerVariable aggregatePointer;

    public ElementPointerVariable(String id, MemoryObject address, ScopedPointerVariable aggregatePointer) {
        super(id, aggregatePointer.getScopeId(), aggregatePointer.getInnerType(), address);
        this.aggregatePointer = aggregatePointer;
    }

    public ScopedPointerVariable getAggregatePointer() {
        return aggregatePointer;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (aggregatePointer == null ? 0 : aggregatePointer.hashCode());
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
        ElementPointerVariable other = (ElementPointerVariable) obj;
        if (aggregatePointer == null) {
            return other.aggregatePointer == null;
        }
        return aggregatePointer.equals(other.aggregatePointer);
    }
}
