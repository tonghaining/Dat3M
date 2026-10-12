// https://github.com/ORNL/HeCBench/blob/master/src/ss-sycl/main.cpp#L268-L286

// clang -x cl -cl-std=CL2.0 -target spir-unknown-unknown -cl-opt-disable \
//     -finline-functions -finline-hint-functions \
//     -emit-llvm -fno-discard-value-names -c heCBench-ss.cl
//     -o heCBench-ss.bc -DNUM_VAR=1 -DNUM_OUTPUT_VAR=1
// llvm-spirv heCBench-ss.bc -o heCBench-ss.spv
// spirv-dis heCBench-ss.spv > heCBench-ss.spvasm

#ifdef LC2GB
#define bar_flag CLK_GLOBAL_MEM_FENCE
#else
#define bar_flag CLK_LOCAL_MEM_FENCE
#endif

__kernel void compact_features(__global uint* d_data,
                               __global uint* d_res,
                               __global uint* d_counts) {
    __local uint local_count;

    uint tid = get_local_id(0);
    uint gid = get_global_id(0);
    uint group_id = get_group_id(0);

    if (tid == 0) {
        local_count = 0;
    }

    barrier(CLK_LOCAL_MEM_FENCE);

    if (d_data[gid]) {
        uint count = atomic_fetch_add_explicit((atomic_uint*)&local_count, 1, memory_order_relaxed, memory_scope_work_group);
        d_res[group_id * get_local_size(0) + count] = gid;
    }

    barrier(CLK_LOCAL_MEM_FENCE);

    if (tid == 0) {
        d_counts[group_id] = local_count;
    }
}