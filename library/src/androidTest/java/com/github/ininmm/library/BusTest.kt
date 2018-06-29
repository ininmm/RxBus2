package com.github.ininmm.library

import android.util.Log
import com.github.ininmm.library.entity.EventType
import com.github.ininmm.library.entity.SubscriberEvent
import com.github.ininmm.library.finder.IFinder
import com.github.ininmm.library.thread.EventThread
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.Test
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by User
 * on 2018/5/6.
 */
class BusTest {

    companion object {
        private val TAG = BusTest::class.java.simpleName
    }
    private val subscribersByType = ConcurrentHashMap<EventType, MutableSet<SubscriberEvent<Any>>>()

    private val subject: Subject<Int> by lazy { PublishSubject.create<Int>().toSerialized() }
    init {
        initObservable()
    }

    fun initObservable() {
        subject.subscribe()
    }

    @Test
    @Throws (Exception::class)
    fun register() {
        val any = "123"
        val method: Method = BusTest::class.java.declaredMethods[1]
        val thread: EventThread = EventThread.MainThread
        val findSubscribersMap = IFinder.Annotated.findAllSubscribers(any)
        val subscribers : MutableSet<SubscriberEvent<Any>>? = mutableSetOf()
        val subscriberEvent = SubscriberEvent<Any>(any, method, thread)
        val findSubscribers: MutableSet<SubscriberEvent<Any>>? = mutableSetOf(subscriberEvent)
        if (findSubscribers?.let {
                    subscribers?.addAll(it) == true
                } != true) {
            Log.d(TAG, "true")
            throw IllegalArgumentException("Object already registered.")
        } else {
            Log.d(TAG, "false")
        }
    }

    @Test
    fun unregister() {
    }

    @Test
    fun post() {
    }
}