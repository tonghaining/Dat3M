package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.expression.ExpressionVisitor;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.type.ScopedPointerType;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;

public class PointerRegister extends Register{

    private final ScopedPointerType innerType;
    private ScopedPointerVariable pointer = null;

    public PointerRegister(String name, Function function, ScopedPointerType type) {
        super(name, function, type);
        this.innerType = type;
    }

    public Type getInnerType() {
        return innerType;
    }

    public void setPointer(ScopedPointerVariable pointer) {
        this.pointer = pointer;
    }

    public ScopedPointerVariable getPointer() {
        return pointer;
    }

    public String getScopeId() {
        return pointer.getScopeId();
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        if (pointer != null) {
            return pointer.accept(visitor);
        } else {
            return visitor.visitRegister(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        PointerRegister other = (PointerRegister) obj;
        if (pointer == null) {
            return other.pointer == null;
        }
        return pointer.equals(other.pointer);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (pointer == null ? 0 : pointer.hashCode());
        return result;
    }
}
