package com.dat3m.dartagnan.program.event.arch.opencl;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.common.RMWCmpXchgBase;
import com.dat3m.dartagnan.program.event.visitors.EventVisitor;

public class OpenCLAtomCAX extends RMWCmpXchgBase {

    public OpenCLAtomCAX(Register register, Expression object, Expression expected, Expression desired,
                         String successMo, String failureMo, String scope) {
        // The failure argument shall be no stronger than the success argument.
        // TODO: Fix encode
        super(register, object, expected, desired, true, successMo);
        this.addTags(Tag.OpenCL.ATOM, scope);
    }

    private OpenCLAtomCAX(OpenCLAtomCAX other) {
        super(other);
    }

    @Override
    public String defaultString() {
        return String.format("%s := atom_cax_%s(%s, %s, %s)", resultRegister, mo, address, expectedValue, storeValue);
    }

    @Override
    public OpenCLAtomCAX getCopy() {
        return new OpenCLAtomCAX(this);
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visitOpenCLRMW(this);
    }
}