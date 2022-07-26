package pers.zhc.tools.utils

import org.jetbrains.annotations.Contract

class Assertion {
    companion object {
        @Contract(" -> fail")
        fun <T> unreachable(): T {
            throw UnreachableError()
        }

        class UnreachableError : Error()
    }
}