package com.dat3m.dartagnan.program.event.lang.opencl;

import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.common.FenceBase;
import com.dat3m.dartagnan.program.event.EventVisitor;

public class OpenCLFence extends FenceBase {

    public OpenCLFence(String fenceFlag, String mo, String scope) {
        super("atomic_work_item_fence", mo);
        this.addTags(Tag.OpenCL.FenceFlag(fenceFlag), Tag.OpenCL.MemoryScope(scope));
    }

    protected OpenCLFence(OpenCLFence other) {
        super(other);
    }

    @Override
    public String defaultString() {
        return name;
    }

    @Override
    public OpenCLFence getCopy() {
        return new OpenCLFence(this);
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visitOpenCLFence(this);
    }
}
