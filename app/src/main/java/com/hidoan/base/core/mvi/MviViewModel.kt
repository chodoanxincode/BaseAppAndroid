package com.hidoan.base.core.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel theo MVVM + MVI:
 * - View gửi hành động của user qua [onIntent] (một cửa vào duy nhất);
 * - View vẽ lại theo [state] (một [StateFlow] bất biến, luôn có giá trị);
 * - việc chạy một lần (điều hướng, toast...) đi qua [effects] - không nằm trong state để không lặp lại khi xoay / quay lại.
 */
abstract class MviViewModel<S : Any, I : Any, E : Any>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // BUFFERED: effect phát lúc View chưa collect (vd. đang ở nền) được giữ lại tới khi View quay lại.
    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    fun onIntent(intent: I) = handleIntent(intent)

    protected abstract fun handleIntent(intent: I)

    protected fun setState(reducer: S.() -> S) = _state.update(reducer)

    protected fun sendEffect(effect: E) {
        _effects.trySend(effect)
    }
}
