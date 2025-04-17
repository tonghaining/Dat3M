package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.program.event.Tag;

import java.util.List;

public enum ScopeNames {
    NONE(List.of(Tag.LOCAL, Tag.GLOBAL)),
    PTX(List.of(Tag.PTX.THD, Tag.PTX.CTA, Tag.PTX.GPU, Tag.PTX.SYS)),
    VULKAN(List.of(Tag.Vulkan.INVOCATION, Tag.Vulkan.SUB_GROUP, Tag.Vulkan.WORK_GROUP, Tag.Vulkan.QUEUE_FAMILY, Tag.Vulkan.DEVICE)),
    OPENCL(List.of(Tag.OpenCL.WORK_ITEM, Tag.OpenCL.SUB_GROUP, Tag.OpenCL.WORK_GROUP, Tag.OpenCL.DEVICE, Tag.OpenCL.ALL));

    private final List<String> scopes;

    ScopeNames(List<String> scopes) {
        this.scopes = scopes;
    }

    public static ScopeNames getDefault() {
        return NONE;
    }

    public static ScopeNames getByArch(Arch arch) {
        return switch (arch) {
            case PTX -> PTX;
            case VULKAN -> VULKAN;
            case OPENCL -> OPENCL;
            default -> NONE;
        };
    }

    public List<String> getScopes() {
        return scopes;
    }
}