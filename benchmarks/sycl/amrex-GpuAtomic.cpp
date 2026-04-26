// https://github.com/AMReX-Codes/amrex/blob/development/Src/Base/AMReX_GpuAtomic.H

#include <sycl/sycl.hpp>
#include <cstdint>

#ifdef DV2WG
#define ATOMIC_SCOPE sycl::memory_scope::work_group
#else
#define ATOMIC_SCOPE sycl::memory_scope::device
#endif

template <typename F>
uint32_t minimal_atomic_op(uint32_t* const address, uint32_t const val, F const f) noexcept {
    constexpr auto mo = sycl::memory_order::relaxed;
    constexpr auto ms = ATOMIC_SCOPE;
    constexpr auto as = sycl::access::address_space::global_space;
    sycl::atomic_ref<uint32_t, mo, ms, as> a{*address};
    uint32_t old_val = a.load();
    uint32_t new_val;
    do {
        new_val = f(old_val, val);
    } while (!a.compare_exchange_strong(old_val, new_val));
    return old_val;
}

class TestKernel; // Single SPIR-V entry point

int main() {
    sycl::queue q;
    uint32_t* data = sycl::malloc_shared<uint32_t>(1, q);
    *data = 0;
    q.submit([&](sycl::handler& h) {
        sycl::nd_range<1> exact_range(sycl::range<1>(4), sycl::range<1>(2));
        h.parallel_for<TestKernel>(exact_range, [=](sycl::nd_item<1> item) {
            auto add_func = [](uint32_t current, uint32_t operand) {
                return current + operand;
            };
            minimal_atomic_op(data, 1u, add_func);
        });
    });
    q.wait();
    sycl::free(data, q);
    return 0;
}