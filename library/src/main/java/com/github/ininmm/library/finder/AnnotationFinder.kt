package com.github.ininmm.library.finder

import com.github.ininmm.library.annotation.Produce
import com.github.ininmm.library.annotation.Subscribe
import com.github.ininmm.library.annotation.TagModel
import com.github.ininmm.library.entity.EventType
import com.github.ininmm.library.entity.ProducerEvent
import com.github.ininmm.library.entity.SubscriberEvent
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * 用來尋找 [Produce], [Subscribe] 註解的方法
 * Created by User
 * on 2018/4/21.
 */
object AnnotationFinder {

    /**
     * 暫存所有 Producer 方法的 EventBus
     */
    private val ProducersCache = ConcurrentHashMap<Class<*>, MutableMap<EventType, SourceMethod>>()

    /**
     * 暫存所有 Subscriber 方法的 EventBus
     */
    private val SubscribersCache = ConcurrentHashMap<Class<*>, MutableMap<EventType, MutableSet<SourceMethod>>>()

    /**
     * 暫存所有 @[Produce] 及 @[Subscribe] 方法到指定的 EventBus
     */
    private fun loadAnnotatedMethods(listenerClass: Class<*>,
                             producerMethods: MutableMap<EventType, SourceMethod> = HashMap(),
                             subscriberMethods: MutableMap<EventType, MutableSet<SourceMethod>> = HashMap()) {

        run breaking@ {
            listenerClass.declaredMethods.forEach continuing@ { method ->
                // 忽略橋接方法
                if (method.isBridge) return@continuing
                // 如果已設定註解標註
                if (method.isAnnotationPresent(Subscribe::class.java)) {
                    val parameterTypes = method.parameterTypes
                    if (parameterTypes.size != 1) {
                        throw IllegalArgumentException("Method: $method has @Subscribe annotation require $parameterTypes params.\n" +
                                "Methods require a param.")
                    }

                    val parameterClazz = parameterTypes[0]
                    if (parameterClazz.isInterface) {
                        throw IllegalArgumentException("Method: $method has @Subscribe annotation on $parameterClazz which is an Interface." +
                                "Only Subscribe on an instance class.")
                    }

                    // 取得修飾符以判斷是否是 public 方法
                    if (!Modifier.isPublic(method.modifiers)) {
                        throw IllegalArgumentException("Method: $method has @Subscribe annotation on $parameterClazz which is not 'public'.")
                    }

                    val annotation = method.getAnnotation(Subscribe::class.java)
                    val tags = annotation.tags
                    var tagSize = tags.size
                    do {
                        // tag: Array<Tag.Model> = [] 表示是默認 Tag
                        val tag = if (tagSize > 0) tags[tagSize - 1].value else TagModel.DEFAULT
                        val type = EventType(tag, parameterClazz)
                        var sourceMethods: MutableSet<SourceMethod>? = subscriberMethods[type]
                        if (sourceMethods == null) {
                            sourceMethods = HashSet()
                            subscriberMethods[type] = sourceMethods
                        }
                        sourceMethods.add(SourceMethod(annotation.thread, method))
                        tagSize --
                    } while (tagSize > 0)
                } else if (method.isAnnotationPresent(Produce::class.java)) {
                    val parameterTypes = method.parameterTypes
                    if (parameterTypes.isNotEmpty()) {
                        throw IllegalArgumentException("Method: $method has @Produce annotation but has $parameterTypes params.\n" +
                                "Methods require no param.")
                    }

                    if (method.returnType == Void::class.java) {
                        throw IllegalArgumentException("Method: $method has a return type of void.\n" +
                                "Methods must return a non-void type.")
                    }

                    val parameterClazz: Class<*> = method.returnType
                    if (parameterClazz.isInterface) {
                        throw IllegalArgumentException("Method: $method has @Produce annotation on $parameterClazz which is an Interface." +
                                "Producer must return a concrete class type.")
                    }
                    if (parameterClazz == Void.TYPE) {
                        throw IllegalArgumentException("Method: $method has @Produce annotation has no return type.")
                    }

                    if (!Modifier.isPublic(method.modifiers)) {
                        throw IllegalArgumentException("Method: $method has @Produce annotation on $parameterClazz which is not 'public'.")
                    }

                    val annotation = method.getAnnotation(Produce::class.java)
                    val tags = annotation.tags
                    var tagSize = tags.size
                    do {
                        val tag = if (tagSize > 0) tags[tagSize - 1].value else TagModel.DEFAULT
                        val type = EventType(tag, parameterClazz)
                        if (producerMethods.containsKey(type)) {
                            throw IllegalArgumentException("Producer type: $type had already been registered.")
                        }
                        producerMethods[type] = SourceMethod(thread = annotation.thread, method = method)
                        tagSize --
                    } while (tagSize > 0)
                }
            }
        }
        ProducersCache[listenerClass] = producerMethods
        SubscribersCache[listenerClass] = subscriberMethods
    }

    /**
     *  [IFinder.findAllProducers] 實作時調用此 function 尋找標記@[Produce]的方法
     */
    fun findAllProducers(listener: Any): MutableMap<EventType, ProducerEvent> {
        val listenerClass = listener::class.java
        val producersInMethod = HashMap<EventType, ProducerEvent>()
        val methods = ProducersCache[listenerClass]
        if (methods == null) {
            loadAnnotatedProducerMethods(listenerClass = listenerClass)
        }
//        var methods = ProducersCache[listenerClass]
//        if (methods == null) {
//            methods = HashMap()
//            loadAnnotatedProducerMethods(listenerClass, methods)
//        }

        if (methods?.isNotEmpty() == true) {
            methods.entries.forEach { entry ->
                val producer = ProducerEvent(listener, entry.value.method, entry.value.thread)
                producersInMethod[entry.key] = producer
            }
        }
        return producersInMethod
    }

    /**
     *  [IFinder.findAllSubscribers] 實作時調用此 function 尋找標記@[Subscribe]的方法
     */
    fun findAllSubscribers(listener: Any): MutableMap<EventType, MutableSet<SubscriberEvent<*>>> {
        val listenerClass = listener::class.java
        val subscribersInMethod = HashMap<EventType, MutableSet<SubscriberEvent<*>>>()
        val methods = SubscribersCache[listenerClass]

        if (methods == null) {
            loadAnnotatedSubscriberMethods(listenerClass = listenerClass)
        }

        if (methods?.isNotEmpty() == true) {
//            for (Map.Entry<EventType, Set<SourceMethod>> e : methods.entrySet()) {
//                Set<SubscriberEvent> subscribers = new HashSet<>();
//                for (SourceMethod m : e.getValue()) {
//                subscribers.add(new SubscriberEvent(listener, m.method, m.thread));
//            }
//                subscribersInMethod.put(e.getKey(), subscribers);
//            }
            methods.entries.forEach { entry ->
                val subscribers = HashSet<SubscriberEvent<*>>()
                entry.value.forEach { sourceMethod ->
//                    subscribers.add(SubscriberEvent<*>(listener, sourceMethod.method, sourceMethod.thread))
                    subscribers.add(SubscriberEvent<Any>(listener, sourceMethod.method, sourceMethod.thread))
                }
                subscribersInMethod[entry.key] = subscribers
            }
        }
        return subscribersInMethod
    }

    private fun loadAnnotatedProducerMethods(listenerClass: Class<*>, producerMethods: MutableMap<EventType, SourceMethod> = HashMap()) {
        loadAnnotatedMethods(listenerClass, producerMethods = producerMethods)
    }

    private fun loadAnnotatedSubscriberMethods(listenerClass: Class<*>, subscriberMethods: MutableMap<EventType, MutableSet<SourceMethod>> = HashMap()) {
        loadAnnotatedMethods(listenerClass, subscriberMethods = subscriberMethods)
    }
}