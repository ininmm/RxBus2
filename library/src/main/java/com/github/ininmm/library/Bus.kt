package com.github.ininmm.library

import com.github.ininmm.library.annotation.Produce
import com.github.ininmm.library.annotation.Subscribe
import com.github.ininmm.library.annotation.TagModel
import com.github.ininmm.library.entity.DeadEvent
import com.github.ininmm.library.entity.EventType
import com.github.ininmm.library.entity.ProducerEvent
import com.github.ininmm.library.entity.SubscriberEvent
import com.github.ininmm.library.finder.IFinder
import com.github.ininmm.library.thread.ThreadEnforcer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.collections.HashSet


/**
 * 事件分配，並幫助事件註冊
 * 1. 接收事件(泛型物件)，使用 Bus 單例的 [register] 傳遞
 *
 * 2. 發送事件，使用 Bus 單例的 [post] 發送
 *
 * 3. Subscriber Methods，永遠只接受 Event
 *   Bus 預設在 main thread 執行，如果要切換線程可以在 constructor 加入建構子 [ThreadEnforcer]
 *
 * 4. 一個 RxBus 流程如下 :
 *              Object obj
 *                  |
 *            post (tag, event)
 *                  |
 *            flattenHierarchy  // 將 obj 及其父類放在一個容器中(Set<>)，再分別發送
 *                  |
 *                  |                           null
 *    從 subscribersByType 取出 SubscriberEvent -----> event 會封裝成 DeadEvent 並發送
 *                  |  有值
 *    dispatch(event, SubscriberEvent) 發送
 *                  |
 *    SubscriberEvent 中調用 Rx 分發管理
 * Created by Michael Lien
 * on 2018/2/11.
 * @param enforcer 用於切換線程; 預設 [ThreadEnforcer.MAIN] 則不能切換
 * @param identifier 標籤，用來辨識此 Bus
 * @param finder 調用 [IFinder.Annotated] 實做 [IFinder] 以尋找註解方法，再封裝成 [SubscriberEvent] 及 [ProducerEvent]
 */
open class Bus internal constructor(private val enforcer: ThreadEnforcer = ThreadEnforcer.MAIN,
                                    private val identifier: String = DefaultIdentifier,
                                    private val finder: IFinder = IFinder.Annotated) {

    companion object {
        const val DefaultIdentifier = "default"
    }

    /**
     * 所有已註冊的 Event subscribers ，利用 EventType 當作索引
     */
    private val subscribersByType = ConcurrentHashMap<EventType, MutableSet<SubscriberEvent<Any>>>()

    /**
     * 所有已註冊的 Event producers ，利用 EventType 當作索引
     */
    private val producersByType = ConcurrentHashMap<EventType, ProducerEvent>()

    private val flattenHierarchyCache = ConcurrentHashMap<Class<*>, Set<Class<*>>>()

    /**
     * 註冊 [any] 中所有 [Subscribe] 註解的方法 (subscriber) 並放入 [subscribersByType] 清單中，
     * 和 [Produce] 註解的方法(producer) 並放入 [producersByType] 清單中
     * 如果任何 subscriber 正在註冊已經有 producer 的類型，他們將立即被調用，並調用該 producer。
     * 如果任何 producer 正在註冊已經擁有 subscriber 的類型，每個 subscriber 將使用得自調用 producer 的返回值
     * @param any any 內所有 subscriber方法都會被註冊
     */
    fun register(any: Any) {
        enforcer.enforce(this)

        val findProducers = finder.findAllProducers(any)
        // if find Producer
        findProducers.keys.forEach { eventType ->
            val producer: ProducerEvent? = findProducers[eventType]
            // 如果 finder 有找到 Producer，看看 ProducerEvent 是否與 producersByType 內的一致
            // producersByType 內如果沒東西就放入，最後再將返回值給 previousProducer
            val previousProducer = producer?.let { producersByType.putIfAbsent(eventType, it) }
            // 檢查 previous 是否存在
            if (previousProducer != null) {
                throw IllegalArgumentException("Producer method for type $eventType " +
                        "has found on type ${producer.getTarget()::class.java}, " +
                        "but already registered by type ${previousProducer.getTarget()::class.java}.")
            }

            // 檢查 subscribersByType 是否有 subscriber
            subscribersByType[eventType]?.let { subscribers ->
                if (subscribers.isNotEmpty()) {
                    subscribers.forEach { subscriber ->
                        producer?.let { dispatchProducerResult(subscriber, it) }
                    }
                }
            }
        }

        val findSubscribersMap = finder.findAllSubscribers(any)
        // if find Subscriber
        findSubscribersMap.keys.forEach { eventType ->
            var subscribers: MutableSet<SubscriberEvent<Any>>? = subscribersByType[eventType]
            // 如果 finder 有找到 Subscriber，看看 SubscriberEvent 是否與 subscribersByType 內的一致
            // subscribersByType 內如果沒東西就放入一組新的Set<SubscriberEvent>，最後再將返回值給 subscribers
            if (subscribers == null) {
                // CopyOnWriteArraySet 建立一個安全並發的 Set
                // bonus : 並發 & 並行
                val subscribersCreation = CopyOnWriteArraySet<SubscriberEvent<Any>>()
                subscribers = subscribersByType.putIfAbsent(eventType, subscribersCreation) ?: subscribersCreation
            }

            val findSubscribers = findSubscribersMap[eventType]
            // if (!subscribers.addAll(findSubscribers))
            // 將 findSubscribers 加入到 subscribers，如果 Set 內已有此元素則不加入並返回 false
            if (findSubscribers?.let { subscribers.addAll(it) }?.not() == false) {
                throw IllegalArgumentException("Object already registered.")
            }
        }

        findSubscribersMap.entries.forEach { entry ->
            val type = entry.key
            // 如果 finder 有找到 Subscriber，看看 producersByType 內有沒有需要發出的事件
            val producer = producersByType[type]
            if (producer?.isValid == true) {
                // 取出 SubscriberEvent 並根據 Producer 分發
                val subscriberEvents = entry.value
                subscriberEvents.forEach { subscribeEvent ->
                    if (subscribeEvent.isValid) {
                        dispatchProducerResult(subscribeEvent, producer)
                    }
                }
            }
        }
    }

    /**
     * 取消訂閱指定 [any] 已經訂閱的 producer 和 subscriber 方法
     * @param any 指定的物件
     */
    fun unregister(any: Any) {
        enforcer.enforce(this)

        val producersInListener = finder.findAllProducers(any)
        // if find Producer
        producersInListener.entries.forEach { entry ->

            val key: EventType = entry.key
            // 從 producersByType 清單找出當前註冊的 Event
            val producer = getProducerForEventType(key)
            // 透過 annotation 找到實際的 Event function
            val value = entry.value
            if (value != producer) {
                throw IllegalArgumentException("Missing event producer. Is ${any.javaClass.simpleName} registered?")
            }

            producersByType.remove(key)?.invalidate()
        }

        val subscribersInListener = finder.findAllSubscribers(any)
        // if find Subscriber
        subscribersInListener.entries.forEach { entry ->

            //從 subscribersByType 清單找出當前註冊的 Event
            val currentSubscribers: MutableSet<SubscriberEvent<Any>>? = getSubscribersForEventType(entry.key)
            // 透過 annotation 找到實際的 Event function
            val eventMethodsInListener: Collection<SubscriberEvent<Any>> = entry.value

            if (currentSubscribers == null || !currentSubscribers.containsAll(eventMethodsInListener)) {
                throw IllegalArgumentException("Missing Event subscribers. Is ${any.javaClass.simpleName} registered?")
            }

            currentSubscribers.forEach { subscriber ->
                if (eventMethodsInListener.contains(subscriber)) {
                    subscriber.invalidate()
                }
            }

            currentSubscribers.removeAll(eventMethodsInListener)
        }
    }

    /**
     * 將事件傳給所有訂閱者。無論是否有錯誤都會返回
     * 注意要發送的 [event] 及其父類皆會發送
     *
     * 如果沒有任何已經訂閱的訂閱者。則將會轉為 [DeadEvent]，並發送
     * @param tag 要傳送的 Event Tag
     * @param event 要傳送的 event
     */
    fun post(tag: String = TagModel.DEFAULT, event: Any) {
        enforcer.enforce(this)

        val dispatchClasses = flattenHierarchy(event.javaClass)

        var dispatched = false

        dispatchClasses.forEach { clazz ->
            val wrappers = getSubscribersForEventType(EventType(tag, clazz))

            if (wrappers?.isNotEmpty() == true) {
                dispatched = true
                wrappers.forEach { wrapper ->
                    dispatch(event, wrapper)
                }
            }
        }

        if (!dispatched && event !is DeadEvent) {
            post(event = DeadEvent(this, event))
        }
    }

    /**
     * 在 [wrapper] 中把 [event] 分發給訂閱者，注意是一個非同步方法
     * @param event 要分派的事件
     * @param wrapper 封裝調用 {@code Subject.onNext}([SubscriberEvent.handle]) 的封裝類
     */
    private fun dispatch(event: Any, wrapper: SubscriberEvent<Any>) {
        if (wrapper.isValid) {
            wrapper.handle(event)
        }
    }

    /**
     * 讓 [Produce] 可以被自己的 [Subscribe] 分發給訂閱者，注意是一個非同步方法
     * @param subscriberEvent 自己的 [SubscriberEvent]，準備實際開始分發
     * @param producer 讓 [ProducerEvent.produce] 可以開始分發
     */
    private fun dispatchProducerResult(subscriberEvent: SubscriberEvent<Any>, producer: ProducerEvent) {
        producer.produce().subscribe {
            dispatch(it, subscriberEvent)
        }
    }

    override fun toString(): String {
        return "Bus with identifier='$identifier'"
    }

    fun cleanStickyEvent() {
        producersByType.clear()
    }

    /**
     * 找出 [type] 當前註冊的 producers ，注意如果當前沒有 producer 將會返回 null
     * @param type 要返回的 Producer type
     * @return 當前註冊的 producer ，如果為空則返回 null
     */
    fun getProducerForEventType(type: EventType): ProducerEvent? = producersByType[type]

    /**
     * 找出 [type] 當前註冊的 subscribers ，注意如果當前沒有 subscriber 將會返回 null 或空的 Set
     * @param type 要返回的 Subscriber type
     * @return 當前註冊的 subscriber
     */
    fun getSubscribersForEventType(type: EventType) = subscribersByType[type]

    /**
     * 檢查 [flattenHierarchyCache] 是否存在該 event 的集合
     */
    fun flattenHierarchy(concreteClass: Class<*>): Set<Class<*>> {
        var classes: Set<Class<*>>? = flattenHierarchyCache[concreteClass]
        if (classes != null) return classes

        val classesCreation: Set<Class<*>> = getClassesFor(concreteClass)
        classes = flattenHierarchyCache.putIfAbsent(concreteClass, classesCreation)
        // 真的還是空的
        if (classes == null) {
            classes = classesCreation
        }

        return classes
    }

    /**
     * 工具方法，尋找一個類的所有父類，包括自己，並存成 Set<Class<>>
     */
    private fun getClassesFor(concreteClass: Class<*>): Set<Class<*>> {
        val parents = LinkedList<Class<*>>()
        val classes = HashSet<Class<*>>()

        parents.add(concreteClass)

        while (!parents.isEmpty()) {
            val clazz = parents.removeAt(0)
            classes.add(clazz)

            val parent: Class<*>? = clazz.superclass

            if (parent != null) {
                parents.add(parent)
            }
        }
        return classes
    }
}