package pers.zhc.tools.main

data class Commit(
    val commitHash: String,
    val commitMessage: String,
    val apks: ArrayList<Apk>,
)