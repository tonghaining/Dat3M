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
    apt-get install -y graphviz

# Set up Dat3M
RUN cd home && \
    git clone --depth=1 --branch artifact https://github.com/tonghaining/Dat3M.git && \
    cd Dat3M && \
    mvn clean install -DskipTests

# symlink for clang
RUN ln -s clang-12 /usr/bin/clang

ENV DAT3M_HOME=/home/Dat3M
ENV DAT3M_OUTPUT=$DAT3M_HOME/output
ENV CFLAGS="-I$DAT3M_HOME/include"
ENV OPTFLAGS="-mem2reg -sroa -early-cse -indvars -loop-unroll -fix-irreducible -loop-simplify -simplifycfg -gvn"

# Set up mixedproxy
RUN cd /home && \
    git clone --depth=1 --branch manual https://github.com/tonghaining/mixedproxy.git && \
    cd /home/mixedproxy && \
    python3 -m pip install --upgrade pip lark-parser && \
    make

# Set up Vulkan-MemoryModel
RUN cd /home && \
    git clone --depth=1 --branch manual https://github.com/tonghaining/Vulkan-MemoryModel.git && \
    cd /home/Vulkan-MemoryModel/alloy && \
    wget https://oss.sonatype.org/content/repositories/snapshots/org/alloytools/org.alloytools.alloy.dist/5.0.0-SNAPSHOT/org.alloytools.alloy.dist-5.0.0-20190619.101010-34.jar

# Profiler
RUN cd /home/Dat3M && \
    python3 -m pip install tabulate && \
    mvn test | tee /home/Dat3M/performance/dat3m.log && \
    cd /home/Dat3M/performance && \
    python3 run_ptx.py /home/mixedproxy/ /home/Dat3M/performance/ptx.log && \
    python3 run_vmm.py /home/Vulkan-MemoryModel/alloy/ /home/Dat3M/performance/vmm.log && \
    python3 profiler.py dat3m.log ptx.log vmm.log
