// Litmus: PTX/Portability/RMW-add.litmus (paper fig:scope-inclusion).
// P0 (cta 0) stores x = 1 and then does a gpu-scoped RMW; P1 (cta 1) does a
// sys-scoped RMW on the same location. In PTX the RMWs are mutually atomic.
// Forbidden (PTX): r0 == 1 && r1 == 1.

// compile through chipStar: https://github.com/CHIP-SPV/chipStar
// hipcc rmw-add.cu -o a.out
// spirv-extractor a.out > rmw-add.spvasm

__global__ void test(int* x, int* r0_, int* r1_) {
    if (blockIdx.x == 0 && threadIdx.x == 0) {
        __scoped_atomic_store_n(x, 1, __ATOMIC_RELAXED, __MEMORY_SCOPE_DEVICE);
        int r0 = __scoped_atomic_fetch_add(x, 1, __ATOMIC_RELAXED, __MEMORY_SCOPE_DEVICE);
        *r0_ = r0;
    } else if (blockIdx.x == 1 && threadIdx.x == 0) {
        int r1 = __scoped_atomic_fetch_add(x, 1, __ATOMIC_RELAXED, __MEMORY_SCOPE_SYSTEM);
        *r1_ = r1;
    }
}

int main() {
    return 0;
}
