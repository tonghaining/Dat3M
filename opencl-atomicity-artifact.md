# Artifact Documentation

This document outlines the directory structure for our test files, the locations of the automated test suites, and instructions for running the verification tool from the command line.

> **Note:** All paths below are relative to the project root directory (`DAT3M_HOME`).

### Test File Locations

* **Litmus Tests:** `litmus/OPENCL/`
* **SPIR-V Binaries:** `dartagnan/src/test/resources/spirv/OPENCL/`
* **OpenCL Source Kernels:** `benchmarks/opencl/`
* **SYCL Source Kernels:** `benchmarks/sycl/`

---

###  Automated Test Suites (Java Testers)

The JUnit test classes are located within `dartagnan/src/test/java/com/dat3m/dartagnan/`.

**Original OpenCL Model:**
* **Data Race Tests:** `litmus/LitmusOpenCLRacesTest.java`
* **Safety Tests:** `litmus/LitmusOpenCLTest.java`

**Revised OpenCL Model (Per-Access-Atomicity):**
* **Data Race Tests:** `litmus/LitmusOpenCLPaaRacesTest.java`
* **Safety Tests:** `litmus/LitmusOpenCLPaaTest.java`

**SPIR-V:**
* **SPIR-V Test Suites:** `spirv/opencl/` *(Directory containing SPIR-V specific testers)*

---

### Command Line Execution

When running the tool via the CLI, you must pair the correct target flag with its corresponding `.cat` memory model file:
* **For the Original Model:** Use `--target=opencl` and `cat/opencl.cat`
* **For the Revised Model:** Use `--target=openclpaa` (OpenCL with Per-Access-Atomicity) and `cat/openclpaa.cat`

**Example Command:**

```bash
java -jar dartagnan/target/dartagnan.jar <path/to/test/file> cat/<model>.cat \
    --target=<target_name> \
    --property=[cat_spec | program_spec] \
    --bound=<unrolling_bound> \
    --method=eager \
    --solver=yices2