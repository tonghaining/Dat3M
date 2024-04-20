import sys
from tabulate import tabulate

RELEVANT_TESTS = [
    "LitmusPTXv6_0Test",
    "LitmusPTXv7_5Test",
    "LitmusPTXv6_0LivenessTest",
    "LitmusPTXv7_5LivenessTest",
    "LitmusVulkanTest",
    "LitmusVulkanNochainsTest" "LitmusVulkanRacesTest",
    "LitmusVulkanRacesNochainsTest",
    "LitmusVulkanLivenessTest",
]

PTX_TESTS = [
    "LitmusPTXv6_0Test",
    "LitmusPTXv7_5Test",
    "LitmusPTXv6_0LivenessTest",
    "LitmusPTXv7_5LivenessTest",
]

VMM_TESTS = [
    "LitmusVulkanTest",
    "LitmusVulkanNochainsTest",
    "LitmusVulkanRacesTest",
    "LitmusVulkanRacesNochainsTest",
    "LitmusVulkanLivenessTest",
]


def get_alloy_performance(alloy_log):
    with open(alloy_log, "r") as f:
        content = f.read()
        lines = content.split("\n")
        for line in lines:
            line = line.strip()
            if "Time" in line:
                time = line.split(" ")[-1]
            if "Tests" in line:
                test_size = line.split(" ")[-1]
            if "Average" in line:
                average = line.split(" ")[-1]
    return test_size, time, average


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
        # print(colored_current_size)
        # print(current_size)
        # print(current_time)
        test_size += int(current_size)
        test_time += float(current_time)

    test_average = test_time / test_size

    return test_size, test_time, test_average


def main():
    if len(sys.argv) != 4:
        print("Usage: profiler.py <dat3m_log> <ptx_log> <vmm_log>")
        sys.exit(1)

    dat3m_log = sys.argv[1]
    ptx_log = sys.argv[2]
    vmm_log = sys.argv[3]

    dat3m_ptx_size, dat3m_ptx_time, dat3m_ptx_average = get_dat3m_performance(
        dat3m_log, tests=PTX_TESTS
    )
    dat3m_vmm_size, dat3m_vmm_time, dat3m_vmm_average = get_dat3m_performance(
        dat3m_log, tests=VMM_TESTS
    )
    ptx_size, ptx_time, ptx_average = get_alloy_performance(ptx_log)
    vmm_size, vmm_time, vmm_average = get_alloy_performance(vmm_log)

    table = [
        ["DAT3M", "PTX", dat3m_ptx_size, dat3m_ptx_time, dat3m_ptx_average],
        ["ALLOY", "PTX", ptx_size, ptx_time, ptx_average],
        ["DAT3M", "VMM", dat3m_vmm_size, dat3m_vmm_time, dat3m_vmm_average],
        ["ALLOY", "VMM", vmm_size, vmm_time, vmm_average],
    ]

    print(tabulate(table, headers=["Tool", "Test", "Size", "Time", "Average"]))


if __name__ == "__main__":
    # sys.argv = ['profiler.py', '/home/Dat3M/performance/dat3m.log',
    #            '/home/Dat3M/performance/ptx.log', '/home/Dat3M/performance/vmm.log']
    main()
