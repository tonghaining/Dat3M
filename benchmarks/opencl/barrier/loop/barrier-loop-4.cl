// clspv barrier-loop-4.cl --cl-std=CL2.0 --inline-entry-points --spv-version=1.6
// spirv-dis a.spv > barrier-loop-4.spvasm

__kernel void test(global atomic_uint* x, global uint* r0) {
    uint tid = get_global_id(0);
    for (uint i = 0; i < 2; i++) {
        atomic_store_explicit(&(x[i * 2 + tid]), 1, memory_order_release);
        barrier(CLK_GLOBAL_MEM_FENCE);
        r0[i * 2 + tid] = atomic_load_explicit(&(x[i * 2 + 1 - tid]), memory_order_acquire);
    }
}
