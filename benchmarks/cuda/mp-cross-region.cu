// compile through chipStar: https://github.com/CHIP-SPV/chipStar

// hipcc mp-cross-region.cu -o mp-cross-region.out
// spirv-extractor mp-cross-region.out > mp-cross-region.spvasm

extern __shared__ int x[];

__global__ void test(int* y, int* r0) {
    if (threadIdx.x == 0) {
        *x = 1;
        __scoped_atomic_store_n(y, 1, __ATOMIC_RELEASE, 2);
    } else if (threadIdx.x == 1) {
        while (__scoped_atomic_load_n(y, __ATOMIC_ACQUIRE, 2) == 0) {}
        int r0_ = *x;
        *r0 = r0_;
    }
}

int main() {
    return 0;
}