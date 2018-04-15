package com.github.ininmm.library.thread

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Created by User
 * on 2018/2/11.
 */
interface ThreadHandler {
    fun getExecutor(): Executor

    fun getHandler(): Handler

    companion object {
        val DEFAULT = object : ThreadHandler {
            private var executor: Executor? = null
            private var handler: Handler? = null
            override fun getExecutor(): Executor {
                if (executor == null) {
                    executor = Executors.newCachedThreadPool()
                }
                return executor!!
            }

            override fun getHandler(): Handler {
                if (handler == null) {
                    handler = Handler(Looper.getMainLooper())
                }
                return handler!!
            }
        }
    }

}