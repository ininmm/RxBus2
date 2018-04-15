package com.github.ininmm.library.annotation

import com.github.ininmm.library.thread.EventThread

/**
 * Created by User
 * on 2018/2/11.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Produce(val thread: EventThread = EventThread.MainThread)