// Litmus: PTX/Portability/SB-barrier-cross-space.litmus. SB in one CTA with
// weak accesses to global x and y separated by __syncthreads().
// Forbidden (PTX): r0 == 0 && r1 == 0.
// chipStar v1.3.0 lowers __syncthreads() to a barrier on Workgroup memory
// only.

// compile through chipStar: https://github.com/CHIP-SPV/chipStar
// hipcc sb-barrier-cross-space.cu -o a.out
// spirv-extractor a.out > sb-barrier-cross-space.spvasm

__global__ void test(int* x, int* y, int* r0_, int* r1_) {
    if (threadIdx.x == 0) {
        *x = 1;
    } else if (threadIdx.x == 1) {
        *y = 1;
    }
    __syncthreads();
    if (threadIdx.x == 0) {
        int r0 = *y;
        *r0_ = r0;
    } else if (threadIdx.x == 1) {
        int r1 = *x;
        *r1_ = r1;
    }
}

int main() {
    return 0;
}
