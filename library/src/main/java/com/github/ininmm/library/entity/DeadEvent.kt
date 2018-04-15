package com.github.ininmm.library.entity

/**
 * Created by User
 * on 2018/4/15.
 */
/**
 * 包裝已經被 post 出去，但是沒有 subscribers 訂閱的 Event
 * 有大大說適合用在 debug 或是 test case ，尚未完全理解怎麼寫測試案例
 */
class DeadEvent(val source: Any, val event: Any)