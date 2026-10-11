// Litmus: PTX/Portability/MP-rel-acq-cross-space-cta.litmus
// MP in one CTA: payload x in global memory, flag y in shared memory,
// release/acquire at cta scope. The OpenCL release/acquire on the local flag
// covers only Local memory, so the global payload is not ordered.
// Forbidden (PTX): r0 == 1 && r1 == 0 (spin-wait, so r0 == 1 always).

// compile through chipStar: https://github.com/CHIP-SPV/chipStar
// hipcc mp-rel-acq-cross-space-cta.cu -o a.out
// spirv-extractor a.out > mp-rel-acq-cross-space-cta.spvasm

__global__ void test(int* x, int* r1_) {
    __shared__ int y;

    if (threadIdx.x == 0) {
        y = 0;
    }
    __syncthreads();

    if (threadIdx.x == 0) {
        *x = 1;
        __scoped_atomic_store_n(&y, 1, __ATOMIC_RELEASE, __MEMORY_SCOPE_WRKGRP);
    } else if (threadIdx.x == 1) {
        while (__scoped_atomic_load_n(&y, __ATOMIC_ACQUIRE, __MEMORY_SCOPE_WRKGRP) == 0) {
        }
        int r1 = *x;
        *r1_ = r1;
    }
}

int main() {
    return 0;
}
