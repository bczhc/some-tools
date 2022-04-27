package pers.zhc.tools.utils

import org.jetbrains.annotations.Contract

class Assertion {
    companion object {
        @Contract(" -> fail")
        fun unreachable() {
            throw UnreachableError()
        }

        class UnreachableError : Error()
    }
}