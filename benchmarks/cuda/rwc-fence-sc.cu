// litmus/PTX/Portability/RWC-fence-sc.litmus

// compile through chipStar: https://github.com/CHIP-SPV/chipStar
// hipcc rwc-fence-sc.cu -o a.out
// spirv-extractor a.out > rwc-fence-sc.spvasm

__global__ void test(int* x, int* y, int* r0, int* r1, int* r2) {

    if (blockIdx.x == 0) {
        __scoped_atomic_store_n(x, 1, __ATOMIC_RELAXED, 1);
    } else if (blockIdx.x == 1) {
        int r0_ = __scoped_atomic_load_n(x, __ATOMIC_RELAXED, 1);
        __scoped_atomic_thread_fence(__ATOMIC_SEQ_CST, 1);
        int r1_ = __scoped_atomic_load_n(y, __ATOMIC_RELAXED, 1);
        *r0 = r0_;
        *r1 = r1_;
    } else if (blockIdx.x == 2) {
        __scoped_atomic_store_n(y, 1, __ATOMIC_RELAXED, 1);
        __scoped_atomic_thread_fence(__ATOMIC_SEQ_CST, 1);
        int r2_ = __scoped_atomic_load_n(x, __ATOMIC_RELAXED, 1);
        *r2 = r2_;
    }
}

int main() {
    return 0;
}
