FROM ubuntu

COPY / /some-tools/

ARG ndk_version=25.1.8937393
ARG cmake_version=3.18.1

WORKDIR /

RUN apt update && \
    export DEBIAN_FRONTEND=noninteractive && \
    apt install openjdk-17-jdk groovy git wget unzip make curl gcc ruby xz-utils -y

RUN git clone https://github.com/openssl/openssl --depth 1 && \
    cd openssl && \
    git fetch --unshallow && \
    git fetch origin openssl-3.0 && \
    git fetch --all && \
    git checkout openssl-3.0.1

WORKDIR /
# Set up SDK
RUN mkdir sdk && \
    wget 'https://dl.google.com/android/repository/commandlinetools-linux-8092744_latest.zip' -O tools.zip && \
    unzip tools.zip && \
    yes | ./cmdline-tools/bin/sdkmanager --licenses --sdk_root=./sdk && \
    ./cmdline-tools/bin/sdkmanager --sdk_root=./sdk --install "ndk;$ndk_version" && \
    ./cmdline-tools/bin/sdkmanager --sdk_root=./sdk --install "cmake;$cmake_version"

WORKDIR /some-tools
# Clone submodules
RUN git submodule update --init --recursive

# Set up basic `config.properties`
RUN echo 'sdk.dir=/sdk' > local.properties && \
    echo "ndk.dir=/sdk/ndk/$ndk_version" >> local.properties && \
    echo 'opensslLib.dir=/openssl' >> config.properties && \
    # dummy values below, for passing the gradle build script check
    echo 'ndk.target=x86-29' >> config.properties && \
    echo 'ndk.buildType=debug' >> config.properties

# Gradle build script check
RUN ./gradlew

# Install Rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs > install && \
    chmod +x install && \
    ./install -y && \
    . ~/.cargo/env && \
    rustup default nightly-2022-11-21 && \
    rustc --version && \
    ./tools/configure-rust

# Build OpenSSL for all Android targets
RUN ./tools/build-openssl /openssl

# Build single-Android-ABI Apps
RUN . ~/.cargo/env && \
    mkdir /apks && mkdir /apks/debug && mkdir /apks/release && \
    for a in armeabi-v7a-21 arm64-v8a-29 x86-29 x86_64-29; do \
      # reconfigure
      sed -ri "s/^(ndk\.target)=.*/\1=$a/" config.properties && \
      sed -ri 's/^(ndk\.buildType)=.*/\1=debug/' config.properties && \
      ./gradlew asD && \
      cp -v app/build/outputs/apk/debug/app-debug.apk /apks/debug/$a.apk && \
      sed -ri 's/^(ndk\.buildType)=.*/\1=release/' config.properties && \
      ./gradlew asR && \
      cp -v app/build/outputs/apk/release/app-release.apk /apks/release/$a.apk; \
    done

# build universal-Android-ABI App
RUN . ~/.cargo/env && \
    sed -ri 's/^(ndk\.target)=.*/\1=armeabi-v7a-21,arm64-v8a-29,x86-29,x86_64-29/' config.properties && \
    sed -ri 's/^(ndk\.buildType)=.*/\1=debug/' config.properties && \
    ./gradlew asD && \
    cp app/build/outputs/apk/debug/app-debug.apk /apks/debug/universal.apk && \
    sed -ri 's/^(ndk\.buildType)=.*/\1=release/' config.properties && \
    ./gradlew asR && \
    cp app/build/outputs/apk/release/app-release.apk /apks/release/universal.apk

# Test `cleanAll` task
RUN . ~/.cargo/env && ./gradlew cleanAll
