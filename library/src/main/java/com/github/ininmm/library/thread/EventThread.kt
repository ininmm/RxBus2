package com.github.ininmm.library.thread

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by User
 * on 2018/2/11.
 */
enum class EventThread {

    /**
     * 封裝RxBus [Schedulers]
     */
    MainThread,
    NewThread,
    IO,
    Computation,
    Single,
    Trampoline,
    Executor,
    Handler;

    companion object {
        fun getScheduler(thread: EventThread): Scheduler {
            return when (thread) {
                MainThread -> AndroidSchedulers.mainThread()
                NewThread -> Schedulers.newThread()
                IO -> Schedulers.io()
                Computation -> Schedulers.computation()
                Single -> Schedulers.single()
                Trampoline -> Schedulers.trampoline()
                Executor -> Schedulers.from(ThreadHandler.DEFAULT.getExecutor())
                Handler -> AndroidSchedulers.from(ThreadHandler.DEFAULT.getHandler().looper)
            }
        }
    }
}