// Litmus: PTX/Portability/MP-fence-sc-weak.litmus. Racy MP across CTAs with
// weak accesses ordered by fence.sc.gpu (__threadfence()).
// PTX still constrains this program; OpenCL gives it undefined behavior.
// Forbidden (PTX): r0 == 1 && r1 == 0.

// compile through chipStar: https://github.com/CHIP-SPV/chipStar
// hipcc mp-fence-sc-weak.cu -o a.out
// spirv-extractor a.out > mp-fence-sc-weak.spvasm

__global__ void test(int* x, int* y, int* r0_, int* r1_) {
    if (blockIdx.x == 0 && threadIdx.x == 0) {
        *x = 1;
        __threadfence();
        *y = 1;
    } else if (blockIdx.x == 1 && threadIdx.x == 0) {
        int r0 = *y;
        __threadfence();
        int r1 = *x;
        *r0_ = r0;
        *r1_ = r1;
    }
}

int main() {
    return 0;
}
