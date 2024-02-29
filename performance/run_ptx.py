import sys
import os
import time
import subprocess


def get_files_under_dir(dir_path):
    test_extension = ".test"
    files = []
    for root, _, filenames in os.walk(dir_path):
        for file in filenames:
            if file.endswith(test_extension):
                files.append(os.path.join(root, file))
    return files


def main():
    if len(sys.argv) != 3:
        print("Usage: run_ptx.py <ptx_repo_path> <log>")
        sys.exit(1)

    ptx_repo_path = sys.argv[1]
    log = sys.argv[2]
    tests_path = os.path.join(ptx_repo_path, "tests")
    script = os.path.join(ptx_repo_path, "src/test_to_alloy.py")

    tests = get_files_under_dir(tests_path)

    start = time.time()
    output = ""
    for test in tests:
        result = subprocess.run(
            ["python3", script, test], stdout=subprocess.PIPE, stderr=subprocess.PIPE
        )
        output += result.stdout.decode("utf-8")
        output += "\n"
    end = time.time()
    test_size = output.count("Launching Alloy...")

    with open(log, "w") as f:
        f.write("Time:(s) " + str(end - start) + "s\n")
        f.write("Tests: " + str(test_size) + "\n")
        f.write("Average:(s) " + str((end - start) / test_size) + "\n")


if __name__ == "__main__":
    # sys.argv = ['run_ptx.py', '/home/mixedproxy/', '/home/Dat3M/performance/ptx.log']
    main()
