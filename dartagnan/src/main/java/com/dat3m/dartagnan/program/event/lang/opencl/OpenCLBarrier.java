package com.dat3m.dartagnan.program.event.lang.opencl;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.EventVisitor;
import com.dat3m.dartagnan.program.event.core.FenceWithId;

public class OpenCLBarrier extends FenceWithId {

    public OpenCLBarrier(Expression fenceId, String fenceFlag) {
        super("work_group_barrier", fenceId);
        this.addTags(Tag.OpenCL.FenceFlag(fenceFlag));
    }

    protected OpenCLBarrier(OpenCLBarrier other) {
        super(other);
    }

    @Override
    public String defaultString() {
        return name;
    }

    @Override
    public OpenCLBarrier getCopy() {
        return new OpenCLBarrier(this);
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visitOpenCLBarrier(this);
    }
}
