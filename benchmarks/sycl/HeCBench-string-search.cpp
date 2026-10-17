// https://github.com/ORNL/HeCBench/blob/master/src/ss-sycl/main.cpp#L268-L286

// icpx -fsycl -fsycl-device-only -fno-sycl-instrument-device-code HeCBench-string-search.cpp -o HeCBench-string-search.bc
// llvm-spirv HeCBench-string-search.bc -o HeCBench-string-search.spv
// spirv-dis HeCBench-string-search.spv -o HeCBench-string-search.spvasm

#include <sycl/sycl.hpp>
#include <iostream>

#ifdef LC2GB
#define BAR_FENCE sycl::access::fence_space::global_space
#else
#define BAR_FENCE sycl::access::fence_space::local_space
#endif

class StringSearchKernel;

int main() {
    sycl::queue q;

    unsigned int arr[] = {0, 1, 1, 1};
    constexpr unsigned int G = 4, L = 2;  // G work-items in total, L work-items per work-group

    unsigned int* d_data = sycl::malloc_device<unsigned int>(G, q);
    unsigned int* d_res = sycl::malloc_device<unsigned int>(G, q);
    unsigned int* d_counts = sycl::malloc_device<unsigned int>(G / L, q);
    q.memcpy(d_data, arr, G * sizeof(unsigned int)).wait();

    q.submit([&](sycl::handler& cgh) {
        cgh.parallel_for<StringSearchKernel>(sycl::nd_range<1>(G, L), [=](sycl::nd_item<1> it) {
            // Work-group local counter, as "__local uint local_count" in OpenCL
            unsigned int& local_count =
                *sycl::ext::oneapi::group_local_memory_for_overwrite<unsigned int>(it.get_group());

            unsigned int tid = it.get_local_id(0);
            unsigned int gid = it.get_global_id(0);
            unsigned int group_id = it.get_group(0);
            unsigned int local_size = it.get_local_range(0);

            if (tid == 0) {
                local_count = 0;
            }

            it.barrier(BAR_FENCE);

            if (d_data[gid]) {
                sycl::atomic_ref<unsigned int,
                                 sycl::memory_order::relaxed,
                                 sycl::memory_scope::work_group,
                                 sycl::access::address_space::local_space> count_ref(local_count);
                unsigned int count = count_ref.fetch_add(1);
                d_res[group_id * local_size + count] = gid;
            }

            it.barrier(BAR_FENCE);

            if (tid == 0) {
                d_counts[group_id] = local_count;
            }
        });
    }).wait();

    unsigned int h_counts[G / L];
    unsigned int h_res[G];
    q.memcpy(h_counts, d_counts, (G / L) * sizeof(unsigned int)).wait();
    q.memcpy(h_res, d_res, G * sizeof(unsigned int)).wait();

    for (unsigned int g = 0; g < G / L; ++g) {
        std::cout << "Group " << g << " found " << h_counts[g] << " matches at indices:";
        for (unsigned int i = 0; i < h_counts[g]; ++i) {
            std::cout << " " << h_res[g * L + i];
        }
        std::cout << "\n";
    }

    sycl::free(d_data, q);
    sycl::free(d_res, q);
    sycl::free(d_counts, q);
    return 0;
}
