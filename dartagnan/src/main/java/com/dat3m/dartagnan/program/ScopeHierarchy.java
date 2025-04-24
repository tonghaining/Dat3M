package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.program.event.Tag;

import java.util.List;

public class ScopeHierarchy {

    private final List<String> scopes;

    public ScopeHierarchy(Arch arch) {
        if (arch == Arch.VULKAN) {
            this.scopes = Tag.Vulkan.getScopeTags();
        } else if (arch == Arch.PTX) {
            this.scopes = Tag.PTX.getScopeTags();
        } else if (arch == Arch.OPENCL) {
            this.scopes = Tag.OpenCL.getScopeTags();
        } else {
            throw new IllegalArgumentException("Unsupported architecture: " + arch);
        }
    }

    public List<String> getScopes() {
        return scopes;
    }

    public int getScopeLevel(String scope) {
        if (!scopes.contains(scope)) {
            throw new IllegalArgumentException("Scope '" + scope + "' not found in the hierarchy");
        }
        return scopes.indexOf(scope);
    }
}
