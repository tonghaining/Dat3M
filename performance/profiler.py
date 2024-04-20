import sys
import csv
from tabulate import tabulate

RELEVANT_TESTS = [
    "LitmusPTXv6_0Test",
    "LitmusPTXv7_5Test",
    "LitmusPTXv6_0LivenessTest",
    "LitmusPTXv7_5LivenessTest",
    "LitmusVulkanTest",
    "LitmusVulkanNochainsTest" ,
    "LitmusVulkanRacesTest",
    "LitmusVulkanRacesNochainsTest",
    "LitmusVulkanLivenessTest",
]

PTX_TESTS = [
    "LitmusPTXv6_0Test",
    "LitmusPTXv7_5Test",
    "LitmusPTXv6_0LivenessTest",
    "LitmusPTXv7_5LivenessTest",
]

PTX60_SAFETY_TESTS = [
    "LitmusPTXv6_0Test",
]

PTX75_SAFETY_TESTS = [
    "LitmusPTXv7_5Test",
]

PTX60_LIVENESS_TESTS = [
    "LitmusPTXv6_0LivenessTest",
]

PTX75_LIVENESS_TESTS = [
    "LitmusPTXv7_5LivenessTest",
]

VMM_TESTS = [
    "LitmusVulkanTest",
    "LitmusVulkanNochainsTest",
    "LitmusVulkanRacesTest",
    "LitmusVulkanRacesNochainsTest",
    "LitmusVulkanLivenessTest",
]

VMM_SAFETY_TESTS = [
    "LitmusVulkanTest",
    "LitmusVulkanNochainsTest",
]

VMM_LIVENESS_TESTS = [
    "LitmusVulkanLivenessTest",
]

VMM_DRF_TESTS = [
    "LitmusVulkanRacesNochainsTest",
    "LitmusVulkanRacesTest",
]


def get_alloy_performance(alloy_log):
    with open(alloy_log, "r") as f:
        content = f.read()
        lines = content.split("\n")
        for line in lines:
            line = line.strip()
            if line.startswith("Time:"):
                time = line.split(" ")[-1]
            if line.startswith("Safety Check:"):
                safety_check = line.split(" ")[-1]
            if line.startswith("DR Check:"):
                dr_check = line.split(" ")[-1]
            if line.startswith("Average:"):
                average = line.split(" ")[-1]
    return safety_check, dr_check, time, average


def get_dat3m_performance(dat3m_log, tests=RELEVANT_TESTS):
    result = []
    with open(dat3m_log, "r") as f:
        content = f.read()
        lines = content.split("\n")
        for line in lines:
            # check if line contains any of the relevant tests
            if any(test in line for test in tests) and ("Running" not in line):
                result.append(line)

    # print("\n".join(result))

    test_size = 0
    test_time = 0
    for res in result:
        cols = res.split(",")
        colored_current_size = cols[0].split(" ")[-1]
        current_time = cols[-1].split(" ")[3]
        # remove color codes
        current_size = colored_current_size.replace("\x1b[0;1;32m", "").replace(
            "\x1b[m", ""
        )
        test_size += int(current_size)
        test_time += float(current_time)

    test_average = test_time / test_size

    return test_size, test_time, test_average


def main():
    if len(sys.argv) != 5:
        print(f"sys.argv: {sys.argv}")
        print("Usage: profiler.py <dat3m_log> <ptx_log> <vmm_log> <result_csv>")
        sys.exit(1)

    dat3m_log = sys.argv[1]
    ptx_log = sys.argv[2]
    vmm_log = sys.argv[3]
    result_csv = sys.argv[4]

    dat3m_ptx60_size_safety, dat3m_ptx60_time_safety, _ = get_dat3m_performance(dat3m_log, tests=PTX60_SAFETY_TESTS)
    dat3m_ptx75_size_safety, dat3m_ptx75_time_safety, _ = get_dat3m_performance(dat3m_log, tests=PTX75_SAFETY_TESTS)
    dat3m_ptx60_size_liveness, dat3m_ptx60_time_liveness, _ = get_dat3m_performance(dat3m_log, tests=PTX60_LIVENESS_TESTS)
    dat3m_ptx75_size_liveness, dat3m_ptx75_time_liveness, _ = get_dat3m_performance(dat3m_log, tests=PTX75_LIVENESS_TESTS)
    dat3m_ptx60_size_total = dat3m_ptx60_size_safety + dat3m_ptx60_size_liveness
    dat3m_ptx60_time_total = dat3m_ptx60_time_safety + dat3m_ptx60_time_liveness
    dat3m_ptx60_average_total = dat3m_ptx60_time_total / dat3m_ptx60_size_total
    dat3m_ptx75_size_total = dat3m_ptx75_size_safety + dat3m_ptx75_size_liveness
    dat3m_ptx75_time_total = dat3m_ptx75_time_safety + dat3m_ptx75_time_liveness
    dat3m_ptx75_average_total = dat3m_ptx75_time_total / dat3m_ptx75_size_total
    
    dat3m_vmm_size_safety, dat3m_vmm_time_safety, _ = get_dat3m_performance(dat3m_log, tests=VMM_SAFETY_TESTS)
    dat3m_vmm_size_liveness, dat3m_vmm_time_liveness, _ = get_dat3m_performance(dat3m_log, tests=VMM_LIVENESS_TESTS)
    dat3m_vmm_size_drf, dat3m_vmm_time_drf, _ = get_dat3m_performance(dat3m_log, tests=VMM_DRF_TESTS)
    dat3m_vmm_size_total = dat3m_vmm_size_safety + dat3m_vmm_size_liveness + dat3m_vmm_size_drf
    dat3m_vmm_time_total = dat3m_vmm_time_safety + dat3m_vmm_time_liveness + dat3m_vmm_time_drf
    dat3m_vmm_average_total = dat3m_vmm_time_total / dat3m_vmm_size_total

    ptx_safety, ptx_dr, ptx_time, ptx_average = get_alloy_performance(ptx_log)
    ptx_total = int(ptx_safety) + int(ptx_dr)
    vmm_safety, vmm_dr, vmm_time, vmm_average = get_alloy_performance(vmm_log)
    vmm_total = int(vmm_safety) + int(vmm_dr)

    table = [
        ["Tool", "Model", "Safety", "Liveness", "DRF", "Total", "Time", "Time/Tests"],
        ["Sabre", "PTX6.0", dat3m_ptx60_size_safety, dat3m_ptx60_size_liveness, 0, dat3m_ptx60_size_total, dat3m_ptx60_time_total, dat3m_ptx60_average_total],
        ["Alloy", "PTX6.0", 0, 0, 0, 0, 0, 0],
        ["Sabre", "PTX7.5", dat3m_ptx75_size_safety, dat3m_ptx75_size_liveness, 0, dat3m_ptx75_size_total, dat3m_ptx75_time_total, dat3m_ptx75_average_total],
        ["Alloy", "PTX7.5", ptx_safety, 0, ptx_dr, ptx_total, ptx_time, ptx_average],
        ["Sabre", "VMM", dat3m_vmm_size_safety, dat3m_vmm_size_liveness, dat3m_vmm_size_drf, dat3m_vmm_size_total, dat3m_vmm_time_total, dat3m_vmm_average_total],
        ["Alloy", "VMM", vmm_safety, 0, vmm_dr, vmm_total, vmm_time, vmm_average],
    ]

    profiler_result = tabulate(table[1:], headers=table[0])
    print(profiler_result)
    
    with open(result_csv, 'w') as csvfile:
        writer = csv.writer(csvfile)
        for row in table:
            writer.writerow(row)

if __name__ == "__main__":
    # sys.argv = ['profiler.py', '/home/Dat3M/performance/dat3m.log',
    #            '/home/Dat3M/performance/ptx.log', '/home/Dat3M/performance/vmm.log', '/home/Dat3M/performance/result.csv']
    main()
