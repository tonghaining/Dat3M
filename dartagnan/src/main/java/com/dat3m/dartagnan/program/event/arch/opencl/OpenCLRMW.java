package com.dat3m.dartagnan.program.event.arch.opencl;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.common.RMWXchgBase;
import com.dat3m.dartagnan.program.event.visitors.EventVisitor;

public class OpenCLRMW extends RMWXchgBase {

    public OpenCLRMW(Register register, Expression address, Expression value, String mo, String scope, String context) {
        super(register, address, value, mo);
        this.addTags(Tag.OpenCL.ATOM, scope, context);
    }

    private OpenCLRMW(OpenCLRMW other) {
        super(other);
    }

    @Override
    public String defaultString() {
        return String.format("%s := rmw[%s](%s, %s)", resultRegister, mo, storeValue, address);
    }

    @Override
    public OpenCLRMW getCopy() {
        return new OpenCLRMW(this);
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visitOpenCLRMW(this);
    }
}