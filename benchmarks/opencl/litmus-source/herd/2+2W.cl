// clspv corr.cl --cl-std=CL2.0 --inline-entry-points --spv-version=1.6
// spirv-dis a.spv > corr.spvasm

__kernel void test(global atomic_uint* x, global atomic_uint* y) {
    if (get_group_id(0) == 0) {
        atomic_store(x, 2);
        atomic_store(y, 1);
    }
    if(get_group_id(0) == 1) {
        atomic_store(y, 2);
        atomic_store(x, 1);
    }
} 
