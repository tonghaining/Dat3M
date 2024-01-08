package com.dat3m.dartagnan.program.event.arch.opencl;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.op.IOpBin;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.common.RMWOpResultBase;
import com.dat3m.dartagnan.program.event.visitors.EventVisitor;

public class OpenCLAtomOp extends RMWOpResultBase {

    public OpenCLAtomOp(Register register, Expression address, IOpBin op, Expression operand, String mo, String scope, String context) {
        super(register, address, op, operand, mo);
        this.addTags(Tag.OpenCL.ATOM, scope, context);
    }

    private OpenCLAtomOp(OpenCLAtomOp other) {
        super(other);
    }

    @Override
    public String defaultString() {
        return String.format("%s := atomic_explicit_%s[%s](%s, %s)", resultRegister, operator.getName(), mo, operand, address);
    }

    @Override
    public OpenCLAtomOp getCopy() {
        return new OpenCLAtomOp(this);
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visitOpenCLRMWOp(this);
    }
}