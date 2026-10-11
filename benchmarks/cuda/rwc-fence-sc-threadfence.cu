// Litmus: PTX/Portability/RWC-fence-sc.litmus (paper fig:rwc-fence-sc).
// P0, P1, P2 in cta 0, 1, 2. __threadfence() is fence.sc.gpu.
// Forbidden (PTX): r0 == 1 && r1 == 0 && r2 == 0.

// compile through chipStar: https://github.com/CHIP-SPV/chipStar
// hipcc rwc-fence-sc-threadfence.cu -o a.out
// spirv-extractor a.out > rwc-fence-sc-threadfence.spvasm

__global__ void test(int* x, int* y, int* r0_, int* r1_, int* r2_) {
    if (blockIdx.x == 0 && threadIdx.x == 0) {
        __scoped_atomic_store_n(x, 1, __ATOMIC_RELAXED, __MEMORY_SCOPE_DEVICE);
    } else if (blockIdx.x == 1 && threadIdx.x == 0) {
        int r0 = __scoped_atomic_load_n(x, __ATOMIC_RELAXED, __MEMORY_SCOPE_DEVICE);
        __threadfence();
        int r1 = __scoped_atomic_load_n(y, __ATOMIC_RELAXED, __MEMORY_SCOPE_DEVICE);
        *r0_ = r0;
        *r1_ = r1;
    } else if (blockIdx.x == 2 && threadIdx.x == 0) {
        __scoped_atomic_store_n(y, 1, __ATOMIC_RELAXED, __MEMORY_SCOPE_DEVICE);
        __threadfence();
        int r2 = __scoped_atomic_load_n(x, __ATOMIC_RELAXED, __MEMORY_SCOPE_DEVICE);
        *r2_ = r2;
    }
}

int main() {
    return 0;
}
