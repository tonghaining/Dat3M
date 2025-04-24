package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.program.event.Tag;

import java.util.List;

public class ScopeSizesVulkan extends ScopeSizes {

    public ScopeSizesVulkan(List<Integer> sizes) {
        super(ScopeHierarchy.fromArch(Arch.VULKAN), sizes);
    }

    @Override
    public int dvSize() {
        return getSize(Tag.Vulkan.DEVICE);
    }

    @Override
    public int qfSize() {
        return getSize(Tag.Vulkan.QUEUE_FAMILY);
    }

    @Override
    public int wgSize() {
        return getSize(Tag.Vulkan.WORK_GROUP);
    }

    @Override
    public int sgSize() {
        return getSize(Tag.Vulkan.SUB_GROUP);
    }

    @Override
    public int dvId(int tid) {
        return getId(Tag.Vulkan.DEVICE, tid);
    }

    @Override
    public int qfId(int tid) {
        return getId(Tag.Vulkan.QUEUE_FAMILY, tid);
    }

    @Override
    public int wgId(int tid) {
        return getId(Tag.Vulkan.WORK_GROUP, tid);
    }

    @Override
    public int sgId(int tid) {
        return getId(Tag.Vulkan.SUB_GROUP, tid);
    }

    @Override
    public int thId(int tid) {
        return tid % sgSize();
    }
}
