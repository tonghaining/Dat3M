#ifdef DV2WG
#define scope memory_scope_work_group
#else
#define scope memory_scope_device
#endif

#ifdef LC2GB
#define flag CLK_GLOBAL_MEM_FENCE
#else
#define flag CLK_LOCAL_MEM_FENCE
#endif

__kernel void histo_main_kernel(__global uint *sm_mappings, __global uint *global_histo)
{
    __local uint sub_histo[2];

    int tid = get_local_id(0);
    int gid = get_global_id(0);

    sub_histo[tid] = 0;

    barrier(flag);

    uint bin_index = sm_mappings[gid];

    atomic_fetch_add_explicit((atomic_uint*)&sub_histo[bin_index], 1, memory_order_relaxed, memory_scope_work_group);

    barrier(flag);

    uint count = sub_histo[tid];
    if (count > 0)
        atomic_fetch_add_explicit((atomic_uint*)&global_histo[tid], count, memory_order_relaxed, scope);
}