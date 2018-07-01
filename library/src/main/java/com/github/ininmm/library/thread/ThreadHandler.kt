package com.github.ininmm.library.thread

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Created by Michael Lien
 * on 2018/2/11.
 */
interface ThreadHandler {
    fun getExecutor(): Executor

    fun getHandler(): Handler

    companion object {
        val DEFAULT = object : ThreadHandler {
            private var executor: Executor? = null
            private var handler: Handler? = null
            /**
             * 建立一個自定義的 IO Thread Pool，
             * ex : 建立一個容量上限為 10 的 IO Thread Pool
             * {@code val executor = Executors.newCachedThreadPool(10)}
             */
            override fun getExecutor(): Executor {
                if (executor == null) {
                    executor = Executors.newCachedThreadPool()
                }
                return executor!!
            }

            /**
             * 注意 :  調用 Main Thread
             * 如果系統應用在不同線程上帶有多個視圖，UI 線程可以與主線程不同。
             * 因此， Android 在  [android.support.annotation] 特別提到
             * 應使用 @UiThread 標註與應用的視圖層次結構關聯的方法，使用 @MainThread 僅標註與應用生命週期關聯的方法
             *
             * <p>
             * 若是在 Android 元件呼叫 Looper.myLooper() 會返回 getMainLooper()，但是 Looper.myLooper() 是 Nullable !
             * </p>
             * @see [android.support.annotation]
             * @return 主線程 Handler
             */
            override fun getHandler(): Handler {
                if (handler == null) {
                    handler = Handler(Looper.getMainLooper())
                }
                return handler!!
            }
        }
    }
}