使用的Android SDK也有设置对应的jdk版本，在Project Structure里选中一个Platform，可以查看到jdk版本的设置

如果这个设置和其它使用的jdk版本不匹配且没有设置任何兼容性的话，就会出现问题。
比如gradle的buildSrc构建Groovy后的jar再引用到gradle作为插件之类的，会报 Unsupported class file major version xx

JNI开发：注意线程之间的资源通信，JNIEnv不能共享，需要使用JavaVM。
还有一些对象也不能共享，需要使用NewGlobalRef。详见官网说明
<https://developer.android.google.cn/training/articles/perf-jni?hl=zh_cn>

