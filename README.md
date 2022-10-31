Tools
----------------
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

### Basic requirements

- CURL
- JDK (>=1.8)
- Groovy

### Set build targets

First we set a "target string" is in the format "\<ABI\>-\<API\>", with `ABI` is Android ABI and `API` is Android target API level. 

The possible values of `ABI` are `armeabi-v7a`, `arm64-v8a`, `x86` and `x86_64`, and you can refer to [Android supported ABIs](https://developer.android.com/ndk/guides/abis#sa). The `ABI` value is an integer, you can refer to [Platform codenames, versions, API levels, and NDK releases](https://source.android.com/setup/start/build-numbers#platform-code-names-versions-api-levels-and-ndk-releases). Ensure that the NDK compiler with the specified API level `$NDK_TOOLCHAIN_BIN_DIR/<target-triple><API>-clang` (e.g. `$SDK_DIR/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android29-clang`) exists.

Now add a property with the key `ndk.target` in `config.properties` (create it if not existing), and its value is the "target string". If multiple build targets are required, join them together with commas.

Examples:

```properties
ndk.target=arm64-v8a-29
```

or 

```properties
ndk.target=armeabi-v7a-19,arm64-v8a-29,x86-29
```

### Configure SDK path

Add property `sdk.dir=SDK_PATH` (replace `SDK_PATH` with your Android SDK path) to "local.properties" at the project root (create it if it doesn't exist).

Make sure the SDK contains NDK. Try running `./gradlew` to check, and it's expected to print messages like below:

```
...
NDK targets: [[abi:..., api:...]...]
Build environment info:
SDK path: ...
NDK path: ...
CMake Version:  ...
...
```

NDK dependencies requirements:

- CMake (>=3.10.0)

### Compile OpenSSL and set path

Because I haven't embeded OpenSSL build into Gradle of the project, so at present it should be compiled manually.

First clone OpenSSL from GitHub repository: https://github.com/openssl/openssl, and checkout the tag `openssl-3.0.1`.

These commands below should work:

```bash
git clone https://github.com/openssl/openssl --depth 1
cd openssl
git fetch --unshallow
git fetch origin openssl-3.0
git fetch --all
git checkout openssl-3.0.1
```

Then go back into this project and run `./tools/build-openssl <openssl-path>`  with an argument of the directory path of OpenSSL just cloned, and it will compile OpenSSL libraries in the targets defined in `config.properties`.

After compilation, the compiled libraries are stored in `<openssl-dir>/libs/<Android-ABI>/`.

Now you need to set `opensslLib.dir=<openssl-dir>` property in `config.properties`.

### Configure Rust

If you don't have [Rust](https://www.rust-lang.org/learn/get-started) installed, you can install it first:

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

Note: This installation method is recommended, since `rustup` is required in the further operations.

#### Add Rust cross-compilation targets

Run `./tools/configure-rust`

### Build Project

So now check again that the file `local.properties` should have `sdk.dir` property, and `config.properties` should contain `opensslLib.dir` and `ndk.target` properties. These configurations are useful for building.

Run `./gradlew :app:assembleDebug` or `./gradlew asD` for debug build.

Run `./gradlew :app:assembleRelease` or `./gradlew asR` for release build.
