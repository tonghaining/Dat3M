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
    apt-get install -y maven && \
    apt-get install -y openjdk-17-jdk && \
    apt-get install -y openjdk-17-jre && \
    apt-get install -y graphviz

# Install Dat3M
RUN cd home && \
    git clone --branch asplos24artifact https://github.com/tonghaining/Dat3M.git && \
    git clone --branch manual https://github.com/tonghaining/Vulkan-MemoryModel.git && \
    git clone --branch manual https://github.com/tonghaining/mixedproxy.git && \
    cd Dat3M && \
    mvn clean install -DskipTests && \
    mvn test > /home/Dat3M/performance/dat3m.log && \
    cd performance && \
    python3 run_vmm.py /home/Vulkan-MemoryModel/alloy/ > vmm.log && \
    python3 run_ptx.py /home/mixedproxy/ > ptx.log && \
    python3 profiler.py dat3m.log vmm.log ptx.log

# symlink for clang
RUN ln -s clang-12 /usr/bin/clang

ENV DAT3M_HOME=/home/Dat3M
ENV DAT3M_OUTPUT=$DAT3M_HOME/output
ENV CFLAGS="-I$DAT3M_HOME/include"
ENV OPTFLAGS="-mem2reg -sroa -early-cse -indvars -loop-unroll -fix-irreducible -loop-simplify -simplifycfg -gvn"