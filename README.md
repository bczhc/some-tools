Tools
----------------
![some-tools Shitcode](https://img.shields.io/static/v1?label=some-tools&message=Shitcode&color=7B5804)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/931b7e3905cc49448a14cebf432a6396)](https://app.codacy.com/gh/bczhc/some-tools?utm_source=github.com&utm_medium=referral&utm_content=bczhc/some-tools&utm_campaign=Badge_Grade)
[![lgtm code quality](https://img.shields.io/lgtm/grade/java/github/bczhc/some-tools)](https://lgtm.com/projects/g/bczhc/some-tools/context:java)
[![lgtm alerts](https://img.shields.io/lgtm/alerts/github/bczhc/some-tools)](https://lgtm.com/projects/g/bczhc/some-tools/alerts/?mode=list)

![Android CI](https://github.com/bczhc/some-tools/workflows/Android%20CI/badge.svg)

![starts](https://img.shields.io/github/stars/bczhc/some-tools?style=flat-square)

![commit activity](https://img.shields.io/github/commit-activity/y/bczhc/some-tools?style=flat-square)
![commit activity](https://img.shields.io/github/commit-activity/m/bczhc/some-tools?style=flat-square)
![commit activity](https://img.shields.io/github/commit-activity/w/bczhc/some-tools?style=flat-square)

![languages](https://img.shields.io/github/languages/count/bczhc/some-tools?style=flat-square)
![repo size](https://img.shields.io/github/repo-size/bczhc/some-tools?style=flat-square)
[![closed issues](https://img.shields.io/github/issues-closed-raw/bczhc/some-tools?color=red&style=flat-square)](https://github.com/bczhc/some-tools/issues?q=is%3Aissue+is%3Aclosed)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/bczhc/some-tools.svg)](https://github.com/bczhc/some-tools/issues?q=is%3Aissue+is%3Aopen)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/bczhc/some-tools.svg)](http://isitmaintained.com/project/bczhc/some-tools "Average time to resolve an issue")

![total lines](https://img.shields.io/tokei/lines/github/bczhc/some-tools)


# Build

## Basic requirements

- curl
- JDK (>=1.8)
- groovy

## Configure SDK path

Define SDK path in "local.properties" file at the project root directory (create if not exist). Add line "sdk.dir=SDK_PATH" (repleace `SDK_PATH` to your Android SDK path) to this configuration file.

The Android SDK needs to contain NDK, try running `./gradlew` to check, and it's expected to print messages like below:

```
...
Build environment info:
SDK path: /root/sdk
NDK path: /root/sdk/ndk-bundle
CMake Version:  3.18.1
...
```

NDK dependencies requirements:

- CMake (>=3.10.0)

## Compile OpenSSL

Because I haven't made a OpenSSL automatic build work, at present OpenSSL should be compiled manually.

Just run `./build-openssl`

## Configure Rust

### Install [Rust](https://www.rust-lang.org/learn/get-started):

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

Note: This installation method is recommended, since `rustup` is required in the further operations.

### Configure cross-compilation toolchain

Run `./configure-rust`

## Build Project

Run `./gradlew :app:assembleDebug` or `./gradlew asD` for debug build.

Run `./gradlew :app:assembleRelease` or `./gradlew asR` for release build.
