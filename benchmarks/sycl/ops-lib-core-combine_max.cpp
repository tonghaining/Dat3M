// https://github.com/OP-DSL/OPS/blob/develop/ops/c/include/ops_lib_core.h

#include <sycl/sycl.hpp>
#include <cstdint>

#ifdef DV2WG
#define ATOMIC_SCOPE sycl::memory_scope::work_group
#else
#define ATOMIC_SCOPE sycl::memory_scope::device
#endif

constexpr auto sycl_mem_odr_rlx   = sycl::memory_order::relaxed;
constexpr auto sycl_mem_scp_dev   = ATOMIC_SCOPE;
constexpr auto sycl_global_space  = sycl::access::address_space::global_space;

template <typename T>
using sycl_atomic_ref_rlx_dev_global_t =
    sycl::atomic_ref<T, sycl_mem_odr_rlx, sycl_mem_scp_dev, sycl_global_space>;

template <typename T>
struct OpsAtomicMaxImpl {
  inline void operator()(T* address, T val) const {
    sycl_atomic_ref_rlx_dev_global_t<T> target(*address);
    T old = target.load();
    while (old < val && !target.compare_exchange_weak(old, val)) {}
  }
};

class TestKernel;

void verify_ops_atomic_max(int* ptr, int val) {
    sycl::queue q;
    q.submit([&](sycl::handler& h) {
        sycl::nd_range<1> exact_range(sycl::range<1>(4), sycl::range<1>(2));
        h.parallel_for<TestKernel>(exact_range, [=](sycl::nd_item<1> item) {
            OpsAtomicMaxImpl<int> atomic_max_op;
            atomic_max_op(ptr, val);
        });
    });
    q.wait();
}