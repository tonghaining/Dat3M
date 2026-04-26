// https://github.com/ORNL/HeCBench/blob/master/src/ss-sycl/main.cpp#L268-L286

#include <sycl/sycl.hpp>
#include <iostream>

class FindFiveKernel;

int main() {
    sycl::queue q;

    unsigned int arr[] = {1, 5, 2, 5};
    unsigned int N = 4, L = 2, G = 4;  // N = 8 items, L = 4 items per group, G = 8 total threads

    // 1. Allocate strictly on the DEVICE
    unsigned int* d_data = sycl::malloc_device<unsigned int>(G, q);
    unsigned int* d_res = sycl::malloc_device<unsigned int>(G, q);
    unsigned int* d_counts = sycl::malloc_device<unsigned int>(G / L, q);

    // 2. EXPLICITLY copy the array from Host to Device and WAIT
    q.memcpy(d_data, arr, N * sizeof(unsigned int)).wait();

    q.submit([&](sycl::handler& cgh) {
        sycl::local_accessor<unsigned int, 1> local_cnt(1, cgh);

        cgh.parallel_for<FindFiveKernel>(sycl::nd_range<1>(G, L), [=](sycl::nd_item<1> it) {
            unsigned int g_id = it.get_global_id(0);
            unsigned int l_id = it.get_local_id(0);
            unsigned int grp_id = it.get_group(0);

            if (l_id == 0) local_cnt[0] = 0;
            it.barrier(sycl::access::fence_space::local_space);
            sycl::atomic_ref<unsigned int,
                 sycl::memory_order::relaxed,
                 sycl::memory_scope::work_group,
                 sycl::access::address_space::local_space> atomic_cnt(local_cnt[0]);

            if (g_id < N && d_data[g_id] == 5) {
                unsigned int count = atomic_cnt.fetch_add(1);
                d_res[grp_id * L + count] = g_id;
            }
            it.barrier(sycl::access::fence_space::local_space);
            if (l_id == 0) d_counts[grp_id] = local_cnt[0];
        });
    }).wait();

    // Allocate standard CPU memory to hold the results
    unsigned int h_counts[2]; // G / L = 2
    unsigned int h_res[8];    // G = 8

    // 4. EXPLICITLY copy the results from Device back to Host and WAIT
    q.memcpy(h_counts, d_counts, (G / L) * sizeof(unsigned int)).wait();
    q.memcpy(h_res, d_res, G * sizeof(unsigned int)).wait();

    // 5. Print matches using the HOST arrays, not the device pointers
    for (unsigned int g = 0; g < G / L; ++g) {
        if (h_counts[g] > 0) {
            std::cout << "Group " << g << " found 5 at indices: ";
            for (unsigned int i = 0; i < h_counts[g]; ++i) {
                std::cout << h_res[g * L + i] << " ";
            }
            std::cout << "\n";
        }
    }

    sycl::free(d_data, q);
    sycl::free(d_res, q);
    sycl::free(d_counts, q);
    return 0;
}