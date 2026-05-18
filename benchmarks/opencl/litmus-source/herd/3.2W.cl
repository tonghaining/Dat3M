

__kernel void test(global atomic_uint* x, global atomic_uint* y, global atomic_uint* z) {
    if (get_group_id(0) == 0 && get_local_id(0) == 0) {
        atomic_store(x, 2);
        atomic_store(y, 1);
    } else if (get_group_id(0) == 1 && get_local_id(0) == 0) {
        atomic_store_explicit(y, 2, memory_order_release, memory_scope_work_group);
        atomic_work_item_fence(CLK_LOCAL_MEM_FENCE,memory_order_seq_cst,memory_scope_work_group);
        atomic_store_explicit(z, 1, memory_order_release, memory_scope_device);
    } else if (get_group_id(0) == 1 && get_local_id(0) == 1) {
        atomic_store_explicit(z,2,memory_order_release, memory_scope_device);
        atomic_work_item_fence(CLK_GLOBAL_MEM_FENCE,memory_order_release,memory_scope_device);
        atomic_store_explicit(x,1,memory_order_release,memory_scope_work_group); // original mo: acq
    }
}
