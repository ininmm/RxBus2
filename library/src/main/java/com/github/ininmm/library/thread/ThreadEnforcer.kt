package com.github.ininmm.library.thread

import android.os.Looper
import com.github.ininmm.library.Bus

/**
 * Created by Michael Lien
 * on 2018/2/11.
 * 強制方法類在特定線程執行
 */
interface ThreadEnforcer {

    /**
     * 讓方法執行在特定執行緒
     * @param bus EventBus class
     */
    fun enforce(bus: Bus)

    companion object {

        /**
         * 未知的ThreadEnforcer
         */
        val ANY: ThreadEnforcer = object : ThreadEnforcer {
            override fun enforce(bus: Bus) {
                //any thread
            }
        }

        /**
         * Main Thread的ThreadEnforcer
         */
        val MAIN: ThreadEnforcer = object : ThreadEnforcer {
            override fun enforce(bus: Bus) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    throw IllegalStateException("Event Bus $bus accessed from non-main thread ${Looper.myLooper()}")
                }
            }
        }
    }

}