package com.hidoan.base.core.ads

fun interface AdHandle {
    fun destroy()
}

data class AdReward(val type: String, val amount: Long)
