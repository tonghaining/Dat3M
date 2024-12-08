package com.dat3m.dartagnan.program.memory;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionVisitor;
import com.dat3m.dartagnan.expression.Type;

public class ScopedPointerVariable extends ScopedPointer {

    public ScopedPointerVariable(String id, String scopeId, Type innerType, MemoryObject address) {
        super(id, scopeId, innerType, address);
        address.addFeatureTag(scopeId);
    }

    @Override
    public MemoryObject getAddress() {
        return (MemoryObject) super.getAddress();
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitScopedPointerVariable(this);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)* %s", getInnerType().toString(), getScopeId(), getId());
    }

    public void setInitialValue(int offset, Expression value) {
        getAddress().setInitialValue(offset, value);
    }
}
