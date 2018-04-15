package com.github.ininmm.library.entity

/**
 * Created by User
 * on 2018/4/15.
 */
class EventType(private val tag: String, private val clazz: Class<*>) {

    override fun toString(): String {
        return "[EventType $tag && $clazz]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventType

        if (tag != other.tag) return false
        if (clazz != other.clazz) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tag.hashCode() + 31
        result = 31 * result + clazz.hashCode()
        return result
    }
}