package com.github.ininmm.library.entity

import com.github.ininmm.library.Bus
import com.github.ininmm.library.annotation.Produce
import com.github.ininmm.library.thread.EventThread
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * "Producer" 的封裝類，封裝 "Producer" method (使用 [Produce] 調用)
 *
 * @param target 註冊使用 Event 之 class，ex: class A 使用 @Produce ， target 即 class A
 * @param method 被 [Produce]  annotation 調用的方法
 * @param thread @Produce 使用的線程
 *
 * Created by User
 * on 2018/4/17.
 */
class ProducerEvent(private val target: Any, private val method: Method, private val thread: EventThread) : Event() {

    init {
        // 設為 true 則把 JAVA 的訪問權限安全檢查關閉，就可以訪問到非 public 的對象 (但還是不可變)
        // 在這裡設定主要是因為關閉後可以提高反射速度
        method.isAccessible = true
    }

    var isValid = true
        private set

    /**
     * 如果是無效事件，將會拒絕發送 event
     * PS : 當封裝對象從 [Bus] 取消註冊時會被調用
     */
    fun invalidate() {
        isValid = false
    }

    fun getTarget(): Any = target
    /**
     * 調用封裝的 preducer method 並返回 [Flowable]
     */
    fun produce(): Flowable<Any> {
        return Flowable.create<Any>({
            try {
                it.onNext(produceEvent())
                it.onComplete()
            } catch (e: InvocationTargetException) {
                throwRuntimeException("Producer ${this@ProducerEvent} threw an exception.", e)
            }
        }, BackpressureStrategy.BUFFER).subscribeOn(EventThread.getScheduler(thread))
    }

    /**
     * 調用封裝的 produce 方法
     * @throws IllegalStateException 如果是無效事件
     * @throws InvocationTargetException 如果封裝方法拋出不是 [Error]] 的 [Throwable]，照原樣傳出去
     */
    @Throws(InvocationTargetException::class)
    private fun produceEvent(): Any {
        if (!isValid) {
            throw IllegalStateException("${toString()} has been invalidated and can' t produce event.")
        }
        try {
            return method.invoke(target)
        } catch (e: InvocationTargetException) {
            if (e.cause is Error) {
                throw e.cause as Error
            }
            throw e
        }
    }

    override fun hashCode(): Int {
        val prime = 31
        return (prime + method.hashCode()) * prime + target.hashCode()
    }

    override fun toString(): String {
        return "[EventProducer $method]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass !== other.javaClass) {
            return false
        }
        val objects = other as ProducerEvent?
        return method == objects?.method && target == objects.target
    }
}