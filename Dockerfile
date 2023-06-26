FROM ubuntu

COPY / /some-tools/

ARG ndk_version=25.1.8937393
ARG cmake_version=3.18.1
ARG full_targets='armeabi-v7a-21,arm64-v8a-29,x86-29,x86_64-29'

WORKDIR /

RUN apt update && \
    export DEBIAN_FRONTEND=noninteractive && \
    apt install openjdk-17-jdk groovy git wget unzip make curl gcc ruby xz-utils -y && \
    # Rust bindgen will not find `stddef.h`. Installing `clang` package solves this.
    # See https://github.com/rust-lang/rust-bindgen/issues/242
    apt install -y clang && \
    # Rust bindgen will report 'bits/libc-header-start.h' not found. These
    # two issues relate to this:
    # - https://github.com/rust-rocksdb/rust-rocksdb/issues/550
    # - https://github.com/rust-lang/rust-bindgen/issues/1229
    # Installing `gcc-multilib` package can let host have that header,
    # and Rust bindgen during cross-compilation seems to be looking it up.
    # The intended solution is to let `bindgen` look for the "sysroot"
    # in NDK, as the two issues above suggest. But `gcc-multilib` way
    # also works (at least so far); I just do as this.
    apt install -y gcc-multilib

# Ruby utilities
RUN gem install toml

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

# Set up `local.properties`
RUN echo 'sdk.dir=/sdk' > local.properties && \
    echo "ndk.dir=/sdk/ndk/$ndk_version" >> local.properties

# Set up basic `config.toml`
RUN cp -v .github/workflows/config.toml .

# Gradle build script check
RUN ./gradlew

# Build OpenSSL for all Android targets
RUN ./tools/build-openssl /openssl "$(echo $full_targets | sed "s/,/ /g")"

# Install Rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs > install && \
    chmod +x install && \
    ./install -y && \
    . ~/.cargo/env && \
    rustup default nightly-2022-11-21 && \
    rustc --version && \
    ./tools/configure-rust

# Build single-Android-ABI Apps
RUN . ~/.cargo/env && \
    mkdir /apks && mkdir /apks/debug && mkdir /apks/release && \
    for target in $(echo $full_targets | sed "s/,/ /g"); do \
      # reconfigure
      target_string="[$target]" && \
      ./tools/toml-replace config.toml 'ndk.build_targets' "$target_string" && \
      ./tools/toml-replace config.toml 'ndk.build_type' '"debug"' && \
      ./gradlew asD && \
      cp -v app/build/outputs/apk/debug/app-debug.apk /apks/debug/$target.apk && \
      ./tools/toml-replace config.toml 'ndk.build_type' '"release"' && \
      ./gradlew asR && \
      cp -v app/build/outputs/apk/release/app-release.apk /apks/release/$a.apk; \
    done

# Build universal-Android-ABI App
RUN . ~/.cargo/env && \
    targets_string="$(ruby -e "require 'json'; puts STDIN.read.chomp.split(',').to_json")" && \
    ./tools/toml-replace config.toml 'ndk.build_targets' "$targets_string" && \
    ./tools/toml-replace config.toml 'ndk.build_type' '"debug"' && \
    ./gradlew asD && \
    cp app/build/outputs/apk/debug/app-debug.apk /apks/debug/universal.apk && \
    ./tools/toml-replace config.toml 'ndk.build_type' '"release"' && \
    ./gradlew asR && \
    cp app/build/outputs/apk/release/app-release.apk /apks/release/universal.apk

# Test `cleanAll` task
RUN . ~/.cargo/env && ./gradlew cleanAll
