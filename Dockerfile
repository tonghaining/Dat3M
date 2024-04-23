#Download base image ubuntu 20.04
FROM ubuntu:20.04

ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y software-properties-common && \
    apt-get install -y git && \
    apt-get install -y graphviz && \
    apt-get install -y sudo && \
    apt-get install -y wget && \
    apt-get install -y make && \
    apt-get install -y python3 && \
    apt-get install -y python3-pip && \
    apt-get install -y maven && \
    apt-get install -y openjdk-17-jdk && \
    apt-get install -y openjdk-17-jre && \
    apt-get install -y graphviz && \
    apt-get install -y python-is-python3 && \
    apt-get install -y ca-certificates gnupg && \
    apt-get install -y libncurses5

ENV DAT3M_HOME=/home/Dat3M
ENV DAT3M_OUTPUT=$DAT3M_HOME/output
ENV CFLAGS="-I$DAT3M_HOME/include"
ENV OPTFLAGS="-mem2reg -sroa -early-cse -indvars -loop-unroll -fix-irreducible -loop-simplify -simplifycfg -gvn"

# Install mono
RUN gpg --homedir /tmp --no-default-keyring --keyring /usr/share/keyrings/mono-official-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF && \
    echo "deb [signed-by=/usr/share/keyrings/mono-official-archive-keyring.gpg] https://download.mono-project.com/repo/ubuntu stable-focal main" | sudo tee /etc/apt/sources.list.d/mono-official-stable.list && \
    apt update && \
    apt install -y mono-devel


# Set up Dat3M
WORKDIR /home
RUN git clone --depth=1 --branch artifact https://github.com/tonghaining/Dat3M.git && \
    cd Dat3M && \
    mvn clean install -DskipTests


# Set up gpuverify
WORKDIR /home
RUN git clone --depth=1 --branch port-verify https://github.com/tonghaining/gpuverify-release.git && \
    python3 -m pip install --upgrade pip psutil

# Set up mixedproxy
WORKDIR /home
RUN git clone --depth=1 --branch manual https://github.com/tonghaining/mixedproxy.git && \
    cd /home/mixedproxy && \
    python3 -m pip install --upgrade pip lark-parser && \
    make

# Set up Vulkan-MemoryModel
WORKDIR /home
RUN git clone --depth=1 --branch manual https://github.com/tonghaining/Vulkan-MemoryModel.git && \
    cd /home/Vulkan-MemoryModel/alloy && \
    wget https://oss.sonatype.org/content/repositories/snapshots/org/alloytools/org.alloytools.alloy.dist/5.0.0-SNAPSHOT/org.alloytools.alloy.dist-5.0.0-20190619.101010-34.jar

# Run Dat3M
WORKDIR /home/Dat3M
RUN mvn test | tee /home/Dat3M/performance/dat3m.log

# Profiler
WORKDIR /home/Dat3M/performance
RUN python3 -m pip install tabulate && \
    python3 run_ptx.py /home/mixedproxy/ /home/Dat3M/performance/ptx.log && \
    python3 run_vmm.py /home/Vulkan-MemoryModel/alloy/ /home/Dat3M/performance/vmm.log && \
    python3 run_gpuverify.py /home/gpuverify-release/ /home/Dat3M/performance/gpuverify.log && \
    python3 profiler.py dat3m.log ptx.log vmm.log litmus_result.csv && \
    python3 compare_verification.py dat3m.log gpuverify.log spirv_result.csv
