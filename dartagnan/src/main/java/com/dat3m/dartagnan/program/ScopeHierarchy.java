package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.program.event.Tag;

import java.util.List;

public record ScopeHierarchy(List<String> scopes) {

    public static ScopeHierarchy fromArch(Arch arch) {
        if (arch == Arch.VULKAN) {
            return new ScopeHierarchy(Tag.Vulkan.getScopeTags());
        } else if (arch == Arch.PTX) {
            return new ScopeHierarchy(Tag.PTX.getScopeTags());
        } else if (arch == Arch.OPENCL) {
            return new ScopeHierarchy(Tag.OpenCL.getScopeTags());
        } else {
            throw new IllegalArgumentException("Unsupported architecture: " + arch);
        }
    }

    public int getScopeLevel(String scope) {
        if (!scopes.contains(scope)) {
            throw new IllegalArgumentException("Scope '" + scope + "' not found in the hierarchy");
        }
        return scopes.indexOf(scope);
    }
}
