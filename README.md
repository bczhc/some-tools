Tools
======

![some-tools Shitcode](https://img.shields.io/static/v1?label=some-tools&message=Shitcode&color=7B5804)

![Android CI](https://github.com/bczhc/some-tools/workflows/Android%20CI/badge.svg)

![commit activity](https://img.shields.io/github/commit-activity/y/bczhc/some-tools?style=flat-square)
![commit activity](https://img.shields.io/github/commit-activity/m/bczhc/some-tools?style=flat-square)
![commit activity](https://img.shields.io/github/commit-activity/w/bczhc/some-tools?style=flat-square)

![languages](https://img.shields.io/github/languages/count/bczhc/some-tools?style=flat-square)
![repo size](https://img.shields.io/github/repo-size/bczhc/some-tools?style=flat-square)
[![closed issues](https://img.shields.io/github/issues-closed-raw/bczhc/some-tools?color=red&style=flat-square)](https://github.com/bczhc/some-tools/issues?q=is%3Aissue+is%3Aclosed)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/bczhc/some-tools.svg)](https://github.com/bczhc/some-tools/issues?q=is%3Aissue+is%3Aopen)


## Build

### Prerequisites

- cURL

- JDK (>=1.8)

- Ruby

  Gem `toml` is required. You can install it using:

  ```bash
  gem install toml
  ```

- CMake (>=3.10.0)

### Set Up SDK and NDK Paths

Add property `sdk.dir=SDK_PATH` (replace `SDK_PATH` with your Android SDK path) to file `local.properties` at the project root (create if it doesn't exist).

The NDK is expected to be detected automatically, or you can also specify it via `ndk.dir` or `ndk.version`.

### Set Up NDK Custom Configurations

In the project root, create a file: `config.toml`. Here is a sample configuration:

```toml
[ndk]
#targets = ["armeabi-v7a-21", "arm64-v8a-29", "x86-29", "x86_64-29"]
targets = ["arm64-v8a-21"]
build_type = "release"
openssl_dir = "/home/bczhc/bin/openssl"

[ndk.rust]
enable_build = true
enable_jni = true
keep_debug_symbols = false
```

The accepted properties are:

- ndk.targets

   A "target string" is in the format `<ABI>-<API>`. Possible values of `ABI` are `armeabi-v7a`, `arm64-v8a`, `x86` and `x86_64`, and you can refer to [Android supported ABIs](https://developer.android.com/ndk/guides/abis#sa). The `API` value is an integer, you can refer to [Platform codenames, versions, API levels, and NDK releases](https://source.android.com/setup/start/build-numbers#platform-code-names-versions-api-levels-and-ndk-releases). Multiple targets are separated by commas.

- ndk.build_type

   It can be `debug` or `release`; this will be applied to both C/C++ and Rust builds.

- ndk.openssl_dir

   OpenSSL path. See [below](#compile-openssl).

- ndk.rust.enable_build

   Add Rust build process to the project's Gradle task tree.

- ndk.rust.enable_jni

   Enable Rust functions in-app.

   Disabling it will cause all functions related to JNI written in Rust unusable, and the app will crash on using them.

   Enabling `ndk.rust.enable_jni` and disabling `ndk.rust.enable_build` is useful when Rust JNI libraries are present as pre-compiled, and the host machine lacks Rust toolchains.

- ndk.rust.keep_debug_symbols

   Keep Rust library debug symbols.

### Compile OpenSSL

First clone OpenSSL from its GitHub repository: https://github.com/openssl/openssl, and checkout tag `openssl-3.0.1`.

These commands below should work:

```bash
git clone https://github.com/openssl/openssl --depth 1
cd openssl
git fetch --unshallow
git fetch origin openssl-3.0
git fetch --all
git checkout openssl-3.0.1
```

Then go back into this project and run `./tools/build-openssl <openssl-path>` with a path to OpenSSL's project root. This will cross-compile OpenSSL libraries in the targets defined in `config.toml`.

The compiled libraries are stored in `<openssl-dir>/libs/<Android-ABI>/`.

Don't forget to set `ndk.openssl_dir` to the OpenSSL project root in `config.toml`.

### Configure Rust

If you don't have [Rust](https://www.rust-lang.org/learn/get-started) installed, you can install it first by running:

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

Note: This installation method is recommended, since it has a good Rust toolchain manager `rustup`.

Then, to add Rust targets for Android, run:

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
```

### Build Project

Run `./gradlew :app:assembleDebug` or `./gradlew asD` for debug build.

Run `./gradlew :app:assembleRelease` or `./gradlew asR` for release build.
