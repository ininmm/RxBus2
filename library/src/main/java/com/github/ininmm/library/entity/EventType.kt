package com.github.ininmm.library.entity

import com.github.ininmm.library.annotation.Produce
import com.github.ininmm.library.annotation.Subscribe

/**
 * 在調用反射查找註解方法時，會將結果存在Map中，此類是Map的Key值
 *
 * @param tag 方法註解中的tag
 * @param clazz @[Subscribe] 或 @ [Produce] 註解中的參數
 * Created by Michael Lien
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