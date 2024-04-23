import sys
import csv
from tabulate import tabulate

import profiler

SPIRV_RACY_TESTS = [
    "SpirvRacesTest",
    # "SpirvRacesVerifyTest",
]

def main():
    if len(sys.argv) != 4:
        print("Usage: compare_verification.py <dat3m_log> <gpuverify_log> <result_csv>")
        sys.exit(1)

    dat3m_log = sys.argv[1]
    gpuverify_log = sys.argv[2]
    result_csv = sys.argv[3]


    _, gpuverify_dr, gpuverify_time = profiler.get_alloy_performance(gpuverify_log)
    gpuverify_total = int(gpuverify_dr)
    gpuverify_time = float(gpuverify_time)
    gpuverify_average = gpuverify_time / gpuverify_total

    # TODO: LOAD DAT3M SPIRV RACY TESTS LOG
    dat3m_total, dat3m_time, dat3m_average = profiler.get_dat3m_performance(dat3m_log, tests=SPIRV_RACY_TESTS)

    table = [
        ["Tool", "benchmarks", "TPT"],
        ["\sabre", dat3m_total, dat3m_average],
        ["\gpuverify", gpuverify_total, gpuverify_average],
    ]

    profiler_result = profiler.tabulate(table[1:], headers=table[0])
    print(profiler_result)

    with open(result_csv, 'w') as csvfile:
        writer = csv.writer(csvfile)
        for row in table:
            writer.writerow(row)

if __name__ == "__main__":
    # sys.argv = ['compare_verification.py', '/home/Dat3M/performance/dat3m.log',
    # '/home/Dat3M/performance/gpuverify.log', '/home/Dat3M/performance/spirv_result.csv']
    main()
