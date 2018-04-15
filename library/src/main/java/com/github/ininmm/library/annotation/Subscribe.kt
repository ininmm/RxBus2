package com.github.ininmm.library.annotation

import com.github.ininmm.library.thread.EventThread

/**
 * Created by User
 * on 2018/2/11.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
annotation class Subscribe(val tags: Array<TagModel.Tag> = arrayOf(), val thread: EventThread = EventThread.MainThread)