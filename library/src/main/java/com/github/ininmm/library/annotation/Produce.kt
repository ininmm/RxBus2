package com.github.ininmm.library.annotation

import com.github.ininmm.library.thread.EventThread

/**
 * 用來註解需要 sticky send 的方法
 * Created by User
 * on 2018/2/11.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Produce(val tags: Array<TagModel.Tag> = [], val thread: EventThread = EventThread.MainThread)