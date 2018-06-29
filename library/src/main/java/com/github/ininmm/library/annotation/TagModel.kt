package com.github.ininmm.library.annotation

import android.support.annotation.StringDef

/**
 * 註解的標籤
 * Created by Michael Lien
 * on 2018/2/11.
 */
class TagModel {
    companion object {
        const val DEFAULT = "RxBusDefaultTag"
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY_GETTER,
            AnnotationTarget.PROPERTY_SETTER)
    @StringDef(DEFAULT)
    annotation class Tag(val value: String = DEFAULT)
}