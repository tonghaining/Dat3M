import sys
import os
import time
import subprocess


def get_test_number(tests_path):
    test_extension = ".test"
    safety_check = 0
    dr_check = 0
    for root, _, filenames in os.walk(tests_path):
        for file in filenames:
            if file.endswith(test_extension):
                content = ""
                with open(os.path.join(root, file), "r") as f:
                    content = f.read()
                lines = content.split("\n")
                for line in lines:
                    if "#dr=0" in line:
                        safety_check += 1
                        continue
                    if "#dr>0" in line:
                        dr_check += 1
                        continue
                    if "NOSOLUTION" in line or "SATISFIABLE" in line:
                        safety_check += 1
                        continue
    return safety_check, dr_check


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
    safety_check, dr_check = get_test_number(os.path.join(vulkan_repo_path, "tests"))
    test_size = safety_check + dr_check
    cost_time = (end - start) * 1000

    with open(log, "w") as f:
        f.write(f"Time:(ms) {cost_time}\n")
        f.write(f"Safety Check: {safety_check}\n")
        f.write(f"DR Check: {dr_check}\n")
        f.write(f"Average:(s) {(end - start) / test_size}\n")


if __name__ == "__main__":
    # sys.argv = ['run_vmm.py', '/home/Vulkan-MemoryModel/alloy/', '/home/Dat3M/performance/vmm.log']
    main()
