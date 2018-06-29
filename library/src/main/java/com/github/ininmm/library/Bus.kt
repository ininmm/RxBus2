package com.github.ininmm.library

import com.github.ininmm.library.annotation.TagModel
import com.github.ininmm.library.entity.EventType
import com.github.ininmm.library.entity.ProducerEvent
import com.github.ininmm.library.entity.SubscriberEvent
import com.github.ininmm.library.finder.IFinder
import com.github.ininmm.library.thread.ThreadEnforcer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet


/**
 * 事件分配，並幫助事件註冊
 * 1. 接收事件(泛型物件)，使用 Bus 單例的 [register] 傳遞
 *
 * 2. 發送事件，使用 Bus 單例的 [post] 發送
 *
 * 3. Subscriber Methods，永遠只接受 Event
 *   Bus 預設在 main thread 執行，如果要切換線程可以在 constructor 加入建構子 [ThreadEnforcer]
 * Created by Michael Lien
 * on 2018/2/11.
 */
open class Bus internal constructor(private val enforcer: ThreadEnforcer,
                           private val identifier: String,
                           private val finder: IFinder) {
    constructor(identifier: String = DefaultIdentifier) : this(ThreadEnforcer.MAIN, identifier)

    constructor(enforcer: ThreadEnforcer, identifier: String = DefaultIdentifier) : this(enforcer, identifier, IFinder.Annotated)


    companion object {
        const val DefaultIdentifier = "default"
    }

    private val subscribersByType = ConcurrentHashMap<EventType, MutableSet<SubscriberEvent<Any>>>()

    private val producersByType = ConcurrentHashMap<EventType, ProducerEvent>()

    private val flattenHierarchyCache = ConcurrentHashMap<Class<*>, Set<Class<*>>>()

    /**
     * 註冊 [any] 所有訂閱者方法 (subscriber) 以接收事件，和生產者方法 (producer) 以提供事件
     * 如果任何訂閱者正在註冊已經有生產者的類型，他們將立即被調用，並調用該生產者。
     * 如果任何生產者正在註冊已經擁有訂閱者的類型，每個訂閱者將使用得自調用生產者的返回值調用
     * @param any any 內所有 subscriber方法都會被註冊
     */
    fun register(any: Any) {
        enforcer.enforce(this)

        val findProducers = finder.findAllProducers(any)
        findProducers.keys.forEach { eventType ->
            val producer = findProducers[eventType]
            val previousProducer = producer?.let { producersByType.putIfAbsent(eventType, it) }
            // 檢查 previous 是否存在
            if (previousProducer != null) {
                throw IllegalArgumentException("Producer method for type $eventType " +
                        "has found on type ${producer.getTarget()::class.java}, " +
                        "but already registered by type ${previousProducer.getTarget()::class.java}.")
            }

            subscribersByType[eventType]?.let { subscribers ->
                if (subscribers.isNotEmpty()) {
                    subscribers.forEach { subscriber ->
                        producer?.let { dispatchProducerResult(subscriber, it) }
                    }
                }
            }
        }

        val findSubscribersMap = finder.findAllSubscribers(any)
        findSubscribersMap.keys.forEach { eventType ->
            var subscribers = subscribersByType[eventType]
            subscribers ?: run {
                val subscribersCreation = CopyOnWriteArraySet<SubscriberEvent<Any>>()
                subscribers = subscribersByType.putIfAbsent(eventType, subscribersCreation) ?: subscribersCreation
            }

            val findSubscribers = findSubscribersMap[eventType]
            if (findSubscribers?.let { return@let subscribers?.addAll(it) == true } != true) {
                throw IllegalArgumentException("Object already registered.")
            }
        }

        findSubscribersMap.entries.forEach { entry ->
            val type = entry.key
            val producer = producersByType[type]
            if (producer?.isValid == true) {
                val subscriberEvents = entry.value
                subscriberEvents.forEach { subscribeEvent ->
                    if (subscribeEvent.isValid) {
                        dispatchProducerResult(subscribeEvent, producer)
                    }
                }
            }
        }
    }

    fun unregister(any: Any) {
        enforcer.enforce(this)


    }

    fun post(tag: String = TagModel.DEFAULT, event: Any) {

    }

    private fun dispatch(event: Any, wrapper: SubscriberEvent<Any>) {
        if (wrapper.isValid) {
            wrapper.handle(event)
        }
    }

    private fun dispatchProducerResult(subscriberEvent: SubscriberEvent<Any>, producer: ProducerEvent) {
        producer.produce().subscribe {
            dispatch(it, subscriberEvent)
        }
    }

    override fun toString(): String {
        return "Bus with identifier='$identifier'"
    }
}