import sys

RELEVANT_TESTS = ["LitmusPTXv6_0Test", "LitmusPTXv7_5Test",
                  "LitmusPTXv6_0LivenessTest", "LitmusPTXv7_5LivenessTest",
                  "LitmusVulkanTest", "LitmusVulkanNochainsTest"
                  "LitmusVulkanRacesTest", "LitmusVulkanRacesNochainsTest",
                  "LitmusVulkanLivenessTest",]


def get_alloy_performance(alloy_log):
    with open(alloy_log, "r") as f:
        content = f.read()
        lines = content.split("\n")
        for line in lines:
            if "Time" in line:
                time = line.split(" ")[-2]
            if "Tests" in line:
                test_size = line.split(" ")[-2]
            if "Average" in line:
                average = line.split(" ")[-2]
    return test_size, time, average


def get_dat3m_performance(dat3m_log):
    result = []
    with open(dat3m_log, "r") as f:
        content = f.read()
        lines = content.split("\n")
        for line in lines:
            # check if line contains any of the relevant tests
            if any(test in line for test in RELEVANT_TESTS) and ("Running" not in line):
                result.append(line)

    # print("\n".join(result))

    test_size = 0
    test_time = 0
    for res in result:
        cols = res.split(",")
        colored_current_size = cols[0].split(" ")[-1]
        current_time = cols[-1].split(" ")[3]
        # remove color codes
        current_size = colored_current_size.replace(
            "\x1b[0;1;32m", "").replace("\x1b[m", "")
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

    dat3m_size, dat3m_time, dat3m_average = get_dat3m_performance(dat3m_log)
    ptx_size, ptx_time, ptx_average = get_alloy_performance(ptx_log)
    vmm_size, vmm_time, vmm_average = get_alloy_performance(vmm_log)

    # Print result table to console
    print("Dat3m\t\t| {}\t| {}\t| {}".format(
        dat3m_size, dat3m_time, dat3m_average))
    print("PTX\t\t| {}\t| {}\t| {}".format(ptx_size, ptx_time, ptx_average))
    print("VMM\t\t| {}\t| {}\t| {}".format(vmm_size, vmm_time, vmm_average))


if __name__ == "__main__":
    # sys.argv = ['profiler.py', '/home/Dat3M/performance/dat3m.log',
    #            '/home/Dat3M/performance/ptx.log', '/home/Dat3M/performance/vmm.log']
    main()
