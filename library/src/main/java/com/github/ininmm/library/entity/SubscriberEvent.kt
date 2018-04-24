package com.github.ininmm.library.entity

import com.github.ininmm.library.Bus
import com.github.ininmm.library.annotation.Subscribe
import com.github.ininmm.library.thread.EventThread
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 *  "Subscriber" 的封裝類，封裝 "Subscriber" method (使用 [Subscribe] 調用)
 *
 * @param target 註冊使用 Event 之 class，ex: class A 使用 @Subscribe ， target 即 class A
 * @param method 被 [Subscribe]  annotation 調用的方法
 * @param thread @Subscribe 使用的線程
 *
 * Created by User
 * on 2018/4/21.
 */
class SubscriberEvent<T>(private val target: Any, private val method: Method, private val thread: EventThread) : Event() {

    init {
        // 設為 true 則把 JAVA 的訪問權限安全檢查關閉，就可以訪問到非 public 的對象 (但還是不可變)
        // 在這裡設定主要是因為關閉後可以提高反射速度
        method.isAccessible = true
        initObservable()
    }

    /**
     * 此訂閱者是否應該收到 event
     */
    var isValid = true
        private set

    private val subject: Subject<T> by lazy { PublishSubject.create<T>() }

    /**
     * 如果是無效事件，將會拒絕發送 event
     * PS : 當封裝對象從 [Bus] 取消註冊時會被調用
     */
    fun invalidate() {
        isValid = false
    }

    private fun initObservable() {
        subject.observeOn(EventThread.getScheduler(thread))
                .subscribe {
                    try {
                        if (isValid) {
                            handleEvent(it)
                        }
                    } catch (e: InvocationTargetException) {
                        throwRuntimeException("Could not dispatch event: $it to subscriber ${this@SubscriberEvent}", e)
                    }
                }
    }

    fun handle(event: T) {
        subject.onNext(event)
    }

    /**
     * 調用封裝的 subscribe method方法
     * @throws IllegalStateException 如果是無效事件
     * @throws InvocationTargetException 如果封裝方法拋出不是 [Error]] 的 [Throwable]，照原樣傳出去
     */
    @Throws(InvocationTargetException::class)
    private fun handleEvent(event: T) {
        if (!isValid) {
            throw IllegalStateException("${toString()} has been invalidated and can no longer handle events.")
        }
        try {
            method.invoke(target, event)
        } catch (e: InvocationTargetException) {
            if (e.cause is Error) {
                throw e.cause as Error
            }
            throw e
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other == null) {
            return false
        }

        if (javaClass != other.javaClass) return false

        val objects = other as SubscriberEvent<*>
        return method == objects.method && target == objects.target
    }

    override fun hashCode(): Int {
        val prime = 31
        return (prime + method.hashCode()) * prime + target.hashCode()
    }

    override fun toString(): String {
        return "[SubscriberEvent $method]"
    }
}