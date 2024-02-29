import sys
import os
import time
import subprocess


def get_test_number(tests_path):
    test_extension = ".test"
    test_count = 1
    for root, _, filenames in os.walk(tests_path):
        for file in filenames:
            if file.endswith(test_extension):
                content = ""
                with open(os.path.join(root, file), "r") as f:
                    content = f.read()
                test_count += content.count("NOSOLUTION")
                test_count += content.count("SATISFIABLE")
    return test_count


def main():
    if len(sys.argv) != 3:
        print("Usage: run_vmm.py <vmm_repo_path> <log>")
        sys.exit(1)

    vulkan_repo_path = sys.argv[1]
    log = sys.argv[2]

    # Change directory to the Vulkan-MemoryModel repository
    # Note: use absolute path of log file, as the working directory will change
    os.chdir(vulkan_repo_path)

    start = time.time()
    subprocess.run(["make", "-j4"])
    end = time.time()
    test_size = get_test_number(os.path.join(vulkan_repo_path, "tests"))

    with open(log, "w") as f:
        f.write("Time:(s) " + str(end - start) + "\n")
        f.write("Tests: " + str(test_size) + "\n")
        f.write("Average:(s) " + str((end - start) / test_size) + "\n")


if __name__ == "__main__":
    # sys.argv = ['run_vmm.py', '/home/Vulkan-MemoryModel/alloy/', '/home/Dat3M/performance/vmm.log']
    main()
