package com.bsit.cyclesync.ui.utils



object AppProvider {
    private const val application = "application"
    private const val context = "context"
    private val providers = mutableMapOf<String, () -> Any>()

    fun registerApplicationProvider(provider: () -> Any): AppProvider {
        providers[application] = provider
        return this
    }

    fun registerContextProvider(provider: () -> Any): AppProvider {
        providers[context] = provider
        return this
    }

    fun getApplication(): Any? = providers[application]?.invoke()

    fun getContext(): Any? =
        providers[context]?.invoke()
}