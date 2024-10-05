plugins {
    id("groovy")
    id("scala")
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation(gradleApi())
    implementation("org.scala-lang:scala-library:2.13.15")
    implementation("org.codehaus.groovy:groovy-all:3.0.22")
    implementation("org.jetbrains:annotations:23.0.0")
    implementation("com.github.bczhc:java-lib:18a858c167")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.github.bczhc:android-target-defs:ac1ea2f9fc")
}
