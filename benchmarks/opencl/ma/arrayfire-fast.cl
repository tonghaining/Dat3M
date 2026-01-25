// clang -x cl -cl-std=CL2.0 -target spir-unknown-unknown -cl-opt-disable \
//     -finline-functions -finline-hint-functions \
//     -emit-llvm -fno-discard-value-names -c arrayfire-fast.cl
//     -o arrayfire-fast.bc -DNUM_VAR=1 -DNUM_OUTPUT_VAR=1
// llvm-spirv arrayfire-fast.bc -o arrayfire-fast.spv
// spirv-dis arrayfire-fast.spv > arrayfire-fast.spvasm

#ifdef DV2WG
#define scope memory_scope_work_group
#else
#define scope memory_scope_device
#endif

#ifdef LC2GB
#define bar_flag CLK_GLOBAL_MEM_FENCE
#else
#define bar_flag CLK_LOCAL_MEM_FENCE
#endif

__kernel void compact_features(__global int* flags,
                               __global int* out_indices,
                               uint group_offset) {
    __local uint s_idx;

    int tid = get_local_id(0);
    int gid = get_global_id(0);

    // The work-group leader initializes the index using a plain store.
    if (tid == 0) {
        s_idx = group_offset;
    }

    barrier(bar_flag);

    // Threads filter data and contend for slots in the output list.
    if (flags[gid]) {
        uint dst = atomic_fetch_add_explicit((atomic_uint*)&s_idx, 1, memory_order_relaxed, scope);
        out_indices[dst] = gid;
    }
}