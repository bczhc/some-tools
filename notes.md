使用的Android SDK也有设置对应的jdk版本，在Project Structure里选中一个Platform，可以查看到jdk版本的设置

如果这个设置和其它使用的jdk版本不匹配且没有设置任何兼容性的话，就会出现问题。
比如gradle的buildSrc构建Groovy后的jar再引用到gradle作为插件之类的，会报 Unsupported class file major version xx

JNI开发：注意线程之间的资源通信，JNIEnv不能共享，需要使用JavaVM。
还有一些对象也不能共享，需要使用NewGlobalRef。详见官网说明
<https://developer.android.google.cn/training/articles/perf-jni?hl=zh_cn>

JNI dev:

Objects returned to Java don't need to be released because of the GC.

wrong:
```c++
auto ret = env->NewStringUTF(s);
env->DeleteLocalRef(ret);
return ret;
```
right:
```c++
return env->NewStringUTF(s);
```

Objects that are newed and passed to a callback should be released. I've tested on Android 7 and Android 8, and if you don't release it, it may cause 512-max-reference overflow on Android 7. (Safe on Android 8, so f**k Google.)
wrong:
```c++
jstring s = env->NewStringUTF(str);
env->CallVoidMethod(callback, mid, s, (jdouble) d);
return;
```
right:
```c++
jstring s = env->NewStringUTF(str);
env->CallVoidMethod(callback, mid, s, (jdouble) d);
env->DeleteLocalRef(s);
return;
```


The right way to send a broadcast:
```java
    Intent intent = new Intent();
    intent.setAction(MyBroadcastReceiver.ACTION_A);
    sendBroadcast(intent);
```
Or:
```java
    Intent intent = new Intent(MyBroadcastReceiver.ACTION_A);
    sendBroadcast(intent);
```

In my case (on Android 7.0, 7.1, 8.0), when sending a broadcast using `Context#sendBroadcast`, there's no need to pass broadcast's class to `Intent` constructor, like:
```java
    Intent intent = new Intent(this, MyBroadcastReceiver.class);
    intent.setAction(MyBroadcastReceiver.ACTION_A);
    sendBroadcast(intent);
```
Otherwise, the destination broadcast will not receive.

But the [official documentation](https://developer.android.google.cn/training/notify-user/build-notification?hl=en#Actions) is as above which doesn't work for me, it's so wired!
```java
Intent snoozeIntent = new Intent(this, MyBroadcastReceiver.class);
snoozeIntent.setAction(ACTION_SNOOZE);
snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
PendingIntent snoozePendingIntent =
PendingIntent.getBroadcast(this, 0, snoozeIntent, 0);

NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
.setSmallIcon(R.drawable.notification_icon)
.setContentTitle("My notification")
.setContentText("Hello World!")
.setPriority(NotificationCompat.PRIORITY_DEFAULT)
.setContentIntent(pendingIntent)
.addAction(R.drawable.ic_snooze, getString(R.string.snooze),
snoozePendingIntent);
```
