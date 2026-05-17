__kernel void test(global atomic_uint* x, global atomic_uint* y) {
    if (get_global_id(0) == 0) {
        atomic_store_explicit(x, 1, memory_order_release);
    } else if (get_global_id(0) == 1) {
        atomic_store_explicit(y, 1, memory_order_release);
    }
    work_group_barrier(CLK_GLOBAL_MEM_FENCE);
    if (get_global_id(0) == 0) {
        uint x1 = atomic_load_explicit(y, memory_order_acquire);
    } else if (get_global_id(0) == 1) {
        uint y1 = atomic_load_explicit(x, memory_order_acquire);
    }
}
