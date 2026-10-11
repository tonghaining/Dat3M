// Litmus: OPENCL/Portability/MP-rel-acq.litmus (paper fig:mp-avvis).
// MP in one work-group, x and y global, non-atomic payload, release/acquire
// at work-group scope without barriers. The read of x is guarded by the flag
// (the litmus test's filter).
// Forbidden (OpenCL): r == 0.

// clspv mp-rel-acq.cl --cl-std=CL2.0 --inline-entry-points --spv-version=1.6 -o mp-rel-acq.glsl450.spv
// spirv-opt --upgrade-memory-model mp-rel-acq.glsl450.spv -o mp-rel-acq.spv
// spirv-dis mp-rel-acq.spv > mp-rel-acq.spvasm

__kernel void test(global int* x, global atomic_int* y, global int* r_) {
    if (get_local_id(0) == 0) {
        *x = 42;
        atomic_store_explicit(y, 1, memory_order_release, memory_scope_work_group);
    } else if (get_local_id(0) == 1) {
        int r = -1;
        *r_ = r;
        if (atomic_load_explicit(y, memory_order_acquire, memory_scope_work_group) == 1) {
            r = *x;
            *r_ = r;
        }
    }
}
