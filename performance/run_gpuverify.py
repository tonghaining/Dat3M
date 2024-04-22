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


def main():
    if len(sys.argv) != 3:
        print("Usage: run_gpuverify.py <gpuverify_repo_path> <log>")
        sys.exit(1)

    gpuverify_repo_path = sys.argv[1]
    log = sys.argv[2]
    tests_path = os.path.join(gpuverify_repo_path, "latest_benchmarks/OpenCL/")
    gpuverify_path = os.path.join(gpuverify_repo_path, "gpuverify")
    tests = get_files_under_dir(tests_path)
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
