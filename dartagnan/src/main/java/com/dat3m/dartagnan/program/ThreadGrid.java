package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.program.event.Tag;

import java.util.HashMap;
import java.util.Map;

public class ThreadGrid {

    protected final Map<String, Integer> scopeSizes = new HashMap<>();
    private final Arch arch;

    public ThreadGrid(Arch arch) {
        this.arch = arch;
    }

    public static ThreadGrid ThreadGridForVulkan(int sg, int wg, int qf, int dv) {
        if (sg <= 0 || wg <= 0 || qf <= 0 || dv <= 0) {
            throw new ParsingException("Thread grid dimensions must be positive");
        }
        ThreadGrid tg = new ThreadGrid(Arch.VULKAN);
        tg.scopeSizes.put(Tag.Vulkan.DEVICE, dv);
        tg.scopeSizes.put(Tag.Vulkan.QUEUE_FAMILY, qf);
        tg.scopeSizes.put(Tag.Vulkan.WORK_GROUP, wg);
        tg.scopeSizes.put(Tag.Vulkan.SUB_GROUP, sg);
        return tg;
    }

    public static ThreadGrid ThreadGridForOpenCL(int sg, int wg, int dv) {
        if (sg <= 0 || wg <= 0 || dv <= 0) {
            throw new ParsingException("Thread grid dimensions must be positive");
        }
        ThreadGrid tg = new ThreadGrid(Arch.OPENCL);
        tg.scopeSizes.put(Tag.OpenCL.DEVICE, dv);
        tg.scopeSizes.put(Tag.OpenCL.WORK_GROUP, wg);
        tg.scopeSizes.put(Tag.OpenCL.SUB_GROUP, sg);
        return tg;
    }

    public Arch getArch() {
        return arch;
    }

    public int getSize(String scope) {
        if (arch.equals(Arch.VULKAN)) {
            return getVulkanSize(scope);
        } else if (arch.equals(Arch.OPENCL)) {
            return getOpenCLSize(scope);
        } else {
            throw new ParsingException("Thread grid not supported for architecture: " + arch);
        }
    }

    private int getVulkanSize(String scope) {
        switch (scope) {
            case Tag.Vulkan.DEVICE -> {
                return scopeSizes.get(Tag.Vulkan.DEVICE)
                        * scopeSizes.get(Tag.Vulkan.QUEUE_FAMILY)
                        * scopeSizes.get(Tag.Vulkan.WORK_GROUP)
                        * scopeSizes.get(Tag.Vulkan.SUB_GROUP);
            }
            case Tag.Vulkan.QUEUE_FAMILY -> {
                return scopeSizes.get(Tag.Vulkan.QUEUE_FAMILY)
                        * scopeSizes.get(Tag.Vulkan.WORK_GROUP)
                        * scopeSizes.get(Tag.Vulkan.SUB_GROUP);
            }
            case Tag.Vulkan.WORK_GROUP -> {
                return scopeSizes.get(Tag.Vulkan.WORK_GROUP)
                        * scopeSizes.get(Tag.Vulkan.SUB_GROUP);
            }
            case Tag.Vulkan.SUB_GROUP -> {
                return scopeSizes.get(Tag.Vulkan.SUB_GROUP);
            }
            default -> throw new ParsingException("Thread grid not supported for scope: " + scope);
        }
    }

    private int getOpenCLSize(String scope) {
        switch (scope) {
            case Tag.OpenCL.DEVICE -> {
                return scopeSizes.get(Tag.OpenCL.DEVICE)
                        * scopeSizes.get(Tag.OpenCL.WORK_GROUP)
                        * scopeSizes.get(Tag.OpenCL.SUB_GROUP);
            }
            case Tag.OpenCL.WORK_GROUP -> {
                return scopeSizes.get(Tag.OpenCL.WORK_GROUP)
                        * scopeSizes.get(Tag.OpenCL.SUB_GROUP);
            }
            case Tag.OpenCL.SUB_GROUP -> {
                return scopeSizes.get(Tag.OpenCL.SUB_GROUP);
            }
            default -> throw new ParsingException("Thread grid not supported for scope: " + scope);
        }
    }

    public int getId(String scope, int tid) {
        if (arch.equals(Arch.VULKAN)) {
            return getVulkanId(scope, tid);
        } else if (arch.equals(Arch.OPENCL)) {
            return getOpenCLId(scope, tid);
        } else {
            throw new ParsingException("Thread grid not supported for architecture: " + arch);
        }
    }

    private int getVulkanId(String scope, int tid) {
        switch (scope) {
            case Tag.Vulkan.DEVICE -> {
                return tid / getSize(Tag.Vulkan.DEVICE);
            }
            case Tag.Vulkan.QUEUE_FAMILY -> {
                return (tid % getSize(Tag.Vulkan.DEVICE)) / getSize(Tag.Vulkan.QUEUE_FAMILY);
            }
            case Tag.Vulkan.WORK_GROUP -> {
                return (tid % getSize(Tag.Vulkan.QUEUE_FAMILY)) / getSize(Tag.Vulkan.WORK_GROUP);
            }
            case Tag.Vulkan.SUB_GROUP -> {
                return (tid % getSize(Tag.Vulkan.WORK_GROUP)) / getSize(Tag.Vulkan.SUB_GROUP);
            }
            case Tag.Vulkan.INVOCATION -> {
                return tid % getSize(Tag.Vulkan.SUB_GROUP);
            }
            default -> throw new ParsingException("Thread grid not supported for scope: " + scope);
        }
    }

    private int getOpenCLId(String scope, int tid) {
        switch (scope) {
            case Tag.OpenCL.DEVICE -> {
                return tid / getSize(Tag.OpenCL.DEVICE);
            }
            case Tag.OpenCL.WORK_GROUP -> {
                return (tid % getSize(Tag.OpenCL.DEVICE)) / getSize(Tag.OpenCL.WORK_GROUP);
            }
            case Tag.OpenCL.SUB_GROUP -> {
                return (tid % getSize(Tag.OpenCL.WORK_GROUP)) / getSize(Tag.OpenCL.SUB_GROUP);
            }
            default -> throw new ParsingException("Thread grid not supported for scope: " + scope);
        }
    }

    public ScopeHierarchy getScoreHierarchy(int tid) {
        if (arch.equals(Arch.VULKAN)) {
            return ScopeHierarchy.ScopeHierarchyForVulkan(
                    getId(Tag.Vulkan.QUEUE_FAMILY, tid),
                    getId(Tag.Vulkan.WORK_GROUP, tid),
                    getId(Tag.Vulkan.SUB_GROUP, tid));
        }
        if (arch.equals(Arch.OPENCL)) {
            return ScopeHierarchy.ScopeHierarchyForOpenCL(
                    getId(Tag.OpenCL.DEVICE, tid),
                    getId(Tag.OpenCL.WORK_GROUP, tid));
        }
        throw new ParsingException("Thread grid not supported for architecture: " + arch);
    }
}
