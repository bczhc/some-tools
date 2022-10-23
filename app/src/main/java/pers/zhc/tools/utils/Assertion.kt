package pers.zhc.tools.utils

import pers.zhc.util.Assertion

class UnreachableError : Error()

fun <T> unreachable(): T {
    throw UnreachableError()
}

fun androidAssert(v: Boolean) {
    Assertion.doAssertion(v)
}
