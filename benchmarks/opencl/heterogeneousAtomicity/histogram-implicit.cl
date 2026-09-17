// clang -x cl -cl-std=CL2.0 -target spir-unknown-unknown -cl-opt-disable \
//     -finline-functions -finline-hint-functions \
//     -emit-llvm -fno-discard-value-names -c histogram.cl
//     -o histogram.bc -DNUM_VAR=1 -DNUM_OUTPUT_VAR=1
// llvm-spirv histogram.bc -o histogram.spv
// spirv-dis histogram.spv > histogram.spvasm

#define HIST_BINS 2

__kernel void histo_main_kernel(global uint *sm_mappings,
                                global uint *global_histo)
{
    __local uint sub_histo[HIST_BINS];

    int tid = get_local_id(0);
    int gid = get_global_id(0);

    // Safe plain store because threads own distinct indices;
    sub_histo[tid] = 0;

    barrier(CLK_LOCAL_MEM_FENCE);

    // Multiple threads contend for the same bins;
    uint bin_index = sm_mappings[gid];
    atom_add(sub_histo + bin_index, 1);

    barrier(CLK_LOCAL_MEM_FENCE);

    // Read local result plain to flush to global;
    uint count = sub_histo[tid];
    if (count > 0)
        atom_add(global_histo + tid, count);
}