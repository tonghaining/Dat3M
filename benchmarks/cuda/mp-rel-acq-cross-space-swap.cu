// Litmus: PTX/Portability/MP-rel-acq-cross-space.litmus, OpenCL variant
// MP-rel-acq-cross-space-swap (paper fig:mp-lock). Spin-wait MP in one CTA:
// payload x in shared memory, flag y in global memory, cta scope.
// Forbidden (PTX): r == 0.

// compile through chipStar: https://github.com/CHIP-SPV/chipStar
// hipcc mp-rel-acq-cross-space-swap.cu -o a.out
// spirv-extractor a.out > mp-rel-acq-cross-space-swap.spvasm

__global__ void test(int* y, int* r_) {
    __shared__ int x;

    if (threadIdx.x == 0) {
        x = 0;
    }
    __syncthreads();

    if (threadIdx.x == 0) {
        x = 1;
        __scoped_atomic_store_n(y, 1, __ATOMIC_RELEASE, __MEMORY_SCOPE_WRKGRP);
    } else if (threadIdx.x == 1) {
        while (__scoped_atomic_load_n(y, __ATOMIC_ACQUIRE, __MEMORY_SCOPE_WRKGRP) == 0) {
        }
        int r = x;
        *r_ = r;
    }
}

int main() {
    return 0;
}
