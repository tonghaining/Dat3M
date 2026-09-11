// compile through chipStar: https://github.com/CHIP-SPV/chipStar

// hipcc mp-cross-region.cu -o a.out
// spirv-extractor a.out > mp-cross-region.spvasm

__global__ void test(int* x, int* r0) {
    __shared__ int y;

    if (threadIdx.x == 0) {
        *x = 1;
        __scoped_atomic_store_n(&y, 1, MEM_RELEASE, 2);
    } else if (threadIdx.x == 1) {
        while (__scoped_atomic_load_n(&y, MEM_ACQUIRE, 2) == 0) {}
        int r0_ = *x;
        *r0 = r0_;
    }
}

int main() {
    return 0;
}