package com.github.ininmm.library.annotation

import com.github.ininmm.library.thread.EventThread

/**
 * 一般用來註解接收方法
 * Created by User
 * on 2018/2/11.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
annotation class Subscribe(val tags: Array<TagModel.Tag> = [], val thread: EventThread = EventThread.MainThread)