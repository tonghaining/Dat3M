package com.dat3m.dartagnan.program.event.common;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.integers.IntCmpOp;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.EventVisitor;
import com.dat3m.dartagnan.program.event.Tag;

public class RMWExtremum extends RMWXchgBase {

    protected final IntCmpOp operator;

    public RMWExtremum(Register register, Expression address, IntCmpOp op, Expression value, String mo) {
        super(register, address, value, mo);
        this.operator = op;
    }

    protected RMWExtremum(RMWExtremum other) {
        super(other);
        this.operator = other.operator;
    }

    public IntCmpOp getOperator() {
        return operator;
    }

    @Override
    public String defaultString() {
        return String.format("%s := rmw ext_%s(%s, %s, %s)", resultRegister, mo, storeValue, address, operator.getName());
    }

    @Override
    public RMWExtremum getCopy() {
        return new RMWExtremum(this);
    }
}