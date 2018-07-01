package com.github.ininmm.library.entity

import java.lang.reflect.InvocationTargetException

/**
 * Created by Michael Lien
 * on 2018/4/15.
 */
abstract class Event {

    /**
     *  [InvocationTargetException] 反射時當被調用的方法的內部拋出了異常而沒有被捕獲時，將由此異常接收。
     * 在這裡將 [InvocationTargetException] 轉成 [RuntimeException]，
     * 如果這裡沒有觸發 [InvocationTargetException] ，那這裡就不會觸發 [RuntimeException]
     */
    fun throwRuntimeException(message: String, exception: InvocationTargetException) {
        throwRuntimeError(message, exception)
    }

    private fun throwRuntimeError(message: String, throwable: Throwable) {
        val cause = throwable.cause
        throw if (cause != null) {
            RuntimeException("$message: ${cause.message}", cause)
        } else {
            RuntimeException("$message: ${throwable.message}", throwable)
        }
    }
}