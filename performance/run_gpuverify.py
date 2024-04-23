import sys
import os
import time
import subprocess


def get_config(test):
    with open(test, "r") as f:
        test_content = f.read()
    config_line = test_content.split("\n")[1]
    config = config_line.strip("//").split(" ")
    return config

def get_general_tests(gpuverify_repo_path):
    general_test_txt = os.path.join(gpuverify_repo_path, "PortAndVerify/no_gpuverify_specific_feature.txt")
    with open(general_test_txt, "r") as f:
        general_tests = f.read().split("\n")
    general_tests = [test for test in general_tests if test != ""]
    general_tests_path = [os.path.join(gpuverify_repo_path, test[3:]) for test in general_tests]
    return general_tests_path


def main():
    if len(sys.argv) != 3:
        print("Usage: run_gpuverify.py <gpuverify_repo_path> <log>")
        sys.exit(1)

    gpuverify_repo_path = sys.argv[1]
    log = sys.argv[2]
    tests = get_general_tests(gpuverify_repo_path)
    gpuverify_path = os.path.join(gpuverify_repo_path, "gpuverify")
    test_size = len(tests)
    start = time.time()
    for i, test in enumerate(tests):
        command = [gpuverify_path]
        command.extend(get_config(test))
        command.append(test)
        subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        print(f"Tested: {i + 1}/{test_size}")
    end = time.time()
    cost_time = (end - start) * 1000


    with open(log, "w") as f:
        f.write(f"Time:(ms) {cost_time}\n")
        f.write(f"Safety Check: 0\n")
        f.write(f"DR Check: {test_size}\n")


if __name__ == "__main__":
    # sys.argv = ['run_gpuverify.py', '/home/gpuverify-release/',
    # '/home/Dat3M/performance/gpuverify.log']
    main()
