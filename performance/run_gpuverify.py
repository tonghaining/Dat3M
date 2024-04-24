import sys
import os
import time
import subprocess

def get_files_under_dir(dir_path):
    files = []
    for root, _, filenames in os.walk(dir_path):
        for file in filenames:
            if file.endswith(".cl"):
                files.append(os.path.join(root, file))
    return files

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

def get_test_filter(gpuverify_filter_path, gpuverify_repo_path):
    # compilation failed
    compilation_failed = os.path.join(gpuverify_filter_path, "compilation-failed.txt")
    with open(compilation_failed, "r") as f:
        compilation_failed_tests = f.read().split("\n")
    compilation_failed_tests = [test for test in compilation_failed_tests if test != ""]

    # compiled empty
    compiled_empty = os.path.join(gpuverify_filter_path, "compiled-empty.txt")
    with open(compiled_empty, "r") as f:
        compiled_empty_tests = f.read().split("\n")
    compiled_empty_tests = [test for test in compiled_empty_tests if test != ""]

    # compiled removed accesses
    compiled_removed_accesses = os.path.join(gpuverify_filter_path, "compiled-removed-accesses.txt")
    with open(compiled_removed_accesses, "r") as f:
        compiled_removed_accesses_tests = f.read().split("\n")
    compiled_removed_accesses_tests = [test for test in compiled_removed_accesses_tests if test != ""]

    test_to_remove = compilation_failed_tests + compiled_empty_tests + compiled_removed_accesses_tests
    test_to_remove = [os.path.join(gpuverify_repo_path, test) for test in test_to_remove]
    return test_to_remove



def main():
    if len(sys.argv) != 4:
        print("Usage: run_gpuverify.py <gpuverify_repo_path> <gpuverify_filter_path> <log>")
        sys.exit(1)

    gpuverify_repo_path = sys.argv[1]
    gpuverify_filter_path = sys.argv[2]
    log = sys.argv[3]
    tests_path = os.path.join(gpuverify_repo_path, "latest_benchmarks/OpenCL/")
    all_test = get_files_under_dir(tests_path)
    test_to_remove = get_test_filter(gpuverify_filter_path, gpuverify_repo_path)
    tests = [test for test in all_test if test not in test_to_remove]
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
    # '/home/Dat3M/performance/gpuverify-filter/', '/home/Dat3M/performance/gpuverify.log']
    main()
