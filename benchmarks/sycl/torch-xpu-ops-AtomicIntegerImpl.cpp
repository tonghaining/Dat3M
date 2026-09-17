// https://github.com/intel/torch-xpu-ops/blob/main/src/ATen/native/xpu/sycl/Atomics.h

// icpx -fsycl -fsycl-device-only -fno-sycl-instrument-device-code torch-xpu-ops-AtomicIntegerImpl.cpp -o torch-xpu-ops-AtomicIntegerImpl.bc
// llvm-spirv torch-xpu-ops-AtomicIntegerImpl.bc -o torch-xpu-ops-AtomicIntegerImpl.spv
// spirv-dis torch-xpu-ops-AtomicIntegerImpl.spv -o torch-xpu-ops-AtomicIntegerImpl.spvasm

#include <sycl/sycl.hpp>
#include <cstdint>

constexpr auto sycl_mem_odr_rlx   = sycl::memory_order::relaxed;
constexpr auto sycl_mem_scp_dev   = sycl::memory_scope::device;
constexpr auto sycl_global_space  = sycl::access::address_space::global_space;

template <typename T>
using sycl_atomic_ref_rlx_dev_global_t =
    sycl::atomic_ref<T, sycl_mem_odr_rlx, sycl_mem_scp_dev, sycl_global_space>;

template <typename T, int size = sizeof(T)>
struct AtomicIntegerImpl;

template <typename T>
struct AtomicIntegerImpl<T, 4> {
    template <typename func_t>
    inline void operator()(T* address, T val, const func_t& func) const {
    uint32_t* address_as_ui = (uint32_t*)(address);
#ifdef FIX
    sycl_atomic_ref_rlx_dev_global_t<uint32_t> target(*address_as_ui);
    uint32_t assumed = target.load();
#else
    uint32_t assumed = *address_as_ui;
    sycl_atomic_ref_rlx_dev_global_t<uint32_t> target(*address_as_ui);
#endif
    uint32_t newval;
    do {
        newval = static_cast<uint32_t>(func(val, static_cast<T>(assumed)));
    } while (!target.compare_exchange_strong(assumed, newval));
  }
};

class TestKernel;

void verify_atomic_cas(uint32_t* ptr, uint32_t val) {
    sycl::queue q;
    q.submit([&](sycl::handler& h) {
        sycl::nd_range<1> exact_range(sycl::range<1>(4), sycl::range<1>(2));
        h.parallel_for<TestKernel>(exact_range, [=](sycl::nd_item<1> item) {
            auto add_func = [](uint32_t val, uint32_t old_val) {
                return old_val + val;
            };
            AtomicIntegerImpl<uint32_t, 4> atomic_op;
            atomic_op(ptr, val, add_func);
        });
    });

    q.wait();
}