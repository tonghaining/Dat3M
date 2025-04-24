package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.program.event.Tag;

import java.util.List;

public class ScopeSizesOpenCL extends ScopeSizes {

    public ScopeSizesOpenCL(List<Integer> sizes) {
        super(ScopeHierarchy.fromArch(Arch.OPENCL), sizes);
    }

    @Override
    public int dvSize() {
        return getSize(Tag.OpenCL.DEVICE);
    }

    @Override
    public int qfSize() {
        return dvSize();
    }

    @Override
    public int wgSize() {
        return getSize(Tag.OpenCL.WORK_GROUP);
    }

    @Override
    public int sgSize() {
        return getSize(Tag.OpenCL.SUB_GROUP);
    }

    @Override
    public int dvId(int tid) {
        return getId(Tag.OpenCL.DEVICE, tid);
    }

    @Override
    public int qfId(int tid) {
        return dvId(tid);
    }

    @Override
    public int wgId(int tid) {
        return getId(Tag.OpenCL.WORK_GROUP, tid);
    }

    @Override
    public int sgId(int tid) {
        return getId(Tag.OpenCL.SUB_GROUP, tid);
    }

    @Override
    public int thId(int tid) {
        return tid % sgSize();
    }
}
