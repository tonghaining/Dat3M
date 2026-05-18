__kernel void test(global uint* x, global atomic_uint* y, global uint* r) {
    if (get_group_id(0) == 0) {
        *x = 1;
        atomic_store_explicit(y, 1, memory_order_release, memory_scope_device);
    }
    if(get_group_id(0) == 1) {
        while(atomic_load_explicit(y, memory_order_acquire, memory_scope_device) == 0) {}
        *r = *x;
    }
} 
