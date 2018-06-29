package com.github.ininmm.library

import com.github.ininmm.library.thread.ThreadEnforcer

/**
 * Created by Michael Lien
 * on 2018/4/29.
 */
object RxBus {

    private var bus: Bus? = null

    @JvmStatic
    fun getBus() : Bus {
        return bus ?: synchronized(RxBus::class.java) {
            bus ?: Bus(ThreadEnforcer.ANY).also { bus = it }
        }
    }
}
