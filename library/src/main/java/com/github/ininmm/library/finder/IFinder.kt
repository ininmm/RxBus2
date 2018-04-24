package com.github.ininmm.library.finder

import com.github.ininmm.library.entity.EventType
import com.github.ininmm.library.entity.ProducerEvent
import com.github.ininmm.library.entity.SubscriberEvent

/**
 * Created by User
 * on 2018/4/21.
 */
interface IFinder {
    fun findAllProducers(listener: Any): MutableMap<EventType, ProducerEvent>

    fun findAllSubscribers(listener: Any): MutableMap<EventType, MutableSet<SubscriberEvent<*>>>

    companion object {
        /**
         * 實作接口
         */
        val Annotated = object : IFinder {
            override fun findAllProducers(listener: Any): MutableMap<EventType, ProducerEvent> =
                    AnnotationFinder.findAllProducers(listener)

            override fun findAllSubscribers(listener: Any): MutableMap<EventType, MutableSet<SubscriberEvent<*>>> =
                    AnnotationFinder.findAllSubscribers(listener)
        }
    }
}