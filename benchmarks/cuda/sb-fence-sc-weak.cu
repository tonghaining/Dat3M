// Litmus: PTX/Portability/SB-fence-sc-weak.litmus. Racy SB across CTAs with
// weak accesses separated by fence.sc.gpu (__threadfence()).
// Forbidden (PTX): r0 == 0 && r1 == 0.

// compile through chipStar: https://github.com/CHIP-SPV/chipStar
// hipcc sb-fence-sc-weak.cu -o a.out
// spirv-extractor a.out > sb-fence-sc-weak.spvasm

__global__ void test(int* x, int* y, int* r0_, int* r1_) {
    if (blockIdx.x == 0 && threadIdx.x == 0) {
        *x = 1;
        __threadfence();
        int r0 = *y;
        *r0_ = r0;
    } else if (blockIdx.x == 1 && threadIdx.x == 0) {
        *y = 1;
        __threadfence();
        int r1 = *x;
        *r1_ = r1;
    }
}

int main() {
    return 0;
}
