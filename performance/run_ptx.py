import sys
import os
import time


def get_files_under_dir(dir_path):
    test_extension = ".test"
    files = []
    for root, dirs, files in os.walk(dir_path):
        for file in files:
            print(file)
            if file.endswith(test_extension):
                files.append(os.path.join(root, file))
    return files


def run_test(script, test_file):
    print("Running test:", test_file)
    os.system("python3 " + script + " " + test_file)


def main():
    if len(sys.argv) != 3:
        print("Usage: run_ptx.py <ptx_repo_path> <log>")
        sys.exit(1)

    ptx_repo_path = sys.argv[1]
    log = sys.argv[2]
    tests_path = os.path.join(ptx_repo_path, "tests")
    script = os.path.join(ptx_repo_path, "src/test_to_alloy.py")

    tests = get_files_under_dir(tests_path)

    start_time = time.time()
    for test in tests:
        run_test(script, test)
    end_time = time.time()

    total_time = end_time - start_time
    avg_time = total_time / len(tests)

    with open(log, "w") as f:
        f.write("Total time: " + str(total_time) + " seconds\n")
        f.write("Number of tests: " + str(len(tests)) + "\n")
        f.write("Average time: " + str(avg_time) + " seconds\n")


if __name__ == "__main__":
    # sys.argv = ['run_ptx.py', '/home/mixedproxy/', 'ptx.log']
    main()
