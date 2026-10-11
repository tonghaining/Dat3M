// Litmus: PTX/Portability/MP-cas-cross-space-cta.litmus (paper
// lst:threadfence-cuda). Reduced reduceSinglePass pattern: payload x in
// shared memory, __threadfence() around a relaxed RMW on a global flag y.
// chipStar lowers __threadfence() to a fence covering only
// CrossWorkgroupMemory, so the shared payload is not ordered.
// Forbidden (PTX): t == 1 && r1 == 0.

// compile through chipStar: https://github.com/CHIP-SPV/chipStar
// hipcc mp-cas-cross-space.cu -o a.out
// spirv-extractor a.out > mp-cas-cross-space.spvasm

__global__ void test(int* y, int* t_, int* r1_) {
    __shared__ int x;

    if (threadIdx.x == 0) {
        x = 0;
    }
    __syncthreads();

    if (threadIdx.x == 0) {
        x = 1;
        __threadfence();
        atomicExch(y, 1);
    } else if (threadIdx.x == 1) {
        int t = atomicCAS(y, 1, 0) == 1;
        *t_ = t;
        if (t) {
            __threadfence();
            int r1 = x;
            *r1_ = r1;
        }
    }
}

int main() {
    return 0;
}
