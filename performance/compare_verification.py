import sys
import csv
from tabulate import tabulate

import profiler

SPIRV_RACY_TESTS = [
    "SpirvRacesVerifyTest.java",
]

def main():
    if len(sys.argv) != 4:
        print("Usage: compare_verification.py <dat3m_log> <gpuverify_log> <result_csv>")
        sys.exit(1)

    dat3m_log = sys.argv[1]
    gpuverify_log = sys.argv[2]
    result_csv = sys.argv[3]


    _, gpuverify_dr, gpuverify_time, _ = profiler.get_alloy_performance(gpuverify_log)
    gpuverify_total = int(gpuverify_dr)
    gpuverify_time = float(gpuverify_time) * 1000
    gpuverify_average = gpuverify_time / gpuverify_total

    # TODO: LOAD DAT3M SPIRV RACY TESTS LOG

    table = [
        ["Tool", "Total", "Time", "Time/Tests"],
        ["GPUVerify", gpuverify_total, gpuverify_time, gpuverify_average],
        ["Dat3m", 0, 0, 0],
    ]

    profiler_result = profiler.tabulate(table[1:], headers=table[0])
    print(profiler_result)

    with open(result_csv, 'w') as csvfile:
        writer = csv.writer(csvfile)
        for row in table:
            writer.writerow(row)

if __name__ == "__main__":
    # sys.argv = ['compare_verification.py', '/home/Dat3M/performance/dat3m.log',
    # '/home/Dat3M/performance/gpuverify.log', '/home/Dat3M/performance/verification.csv']
    main()
