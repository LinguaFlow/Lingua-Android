package com.yju.presentation.util

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * 이벤트를 한 번만 소비할 수 있는 Flow 인터페이스
 */
interface EventFlow<out T> : Flow<T> {
    companion object {
        const val DEFAULT_REPLAY: Int = 0
        const val DEFAULT_EXTRA_BUFFER: Int = 16
    }
}

/**
 * 이벤트를 발행할 수 있는 변경 가능한 EventFlow 인터페이스
 */
interface MutableEventFlow<T> : EventFlow<T>, FlowCollector<T>

/**
 * MutableEventFlow를 생성하는 팩토리 함수
 *
 * @param replay 재생 버퍼 크기
 * @param extraBufferCapacity 추가 버퍼 용량
 * @param onBufferOverflow 버퍼 오버플로우 시 동작 방식
 */
fun <T> MutableEventFlow(
    replay: Int = EventFlow.DEFAULT_REPLAY,
    extraBufferCapacity: Int = EventFlow.DEFAULT_EXTRA_BUFFER,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): MutableEventFlow<T> = EventFlowImpl(replay, extraBufferCapacity, onBufferOverflow)

/**
 * MutableEventFlow를 읽기 전용 EventFlow로 변환
 */
fun <T> MutableEventFlow<T>.asEventFlow(): EventFlow<T> = ReadOnlyEventFlow(this)

/**
 * 읽기 전용 EventFlow 구현
 */
private class ReadOnlyEventFlow<T>(flow: EventFlow<T>) : EventFlow<T> by flow

/**
 * EventFlow의 실제 구현체
 *
 * 내부에서는 각 이벤트에 고유 ID를 부여하여, 각 구독자가 중복으로 이벤트를 소비하지 않도록 합니다.
 *
 * @param replay 재생 버퍼 크기
 * @param extraBufferCapacity 추가 버퍼 용량
 * @param onBufferOverflow 버퍼 오버플로우 시 동작 방식
 */
private class EventFlowImpl<T>(
    replay: Int,
    extraBufferCapacity: Int,
    onBufferOverflow: BufferOverflow
) : MutableEventFlow<T> {

    // 각 이벤트에 고유 ID와 값을 함께 저장
    private data class EventWithId<T>(val id: Long, val value: T)

    // 마지막에 할당된 이벤트 ID를 추적 (AtomicLong 사용)
    private val lastId = AtomicLong(0L)

    // 최적화된 SharedFlow 사용
    private val sharedFlow = MutableSharedFlow<EventWithId<T>>(
        replay = replay,
        extraBufferCapacity = extraBufferCapacity,
        onBufferOverflow = onBufferOverflow
    )

    /**
     * 각 콜렉터별로 마지막으로 emit한 이벤트의 ID를 추적하여,
     * 동일한 이벤트가 중복으로 소비되는 것을 방지합니다.
     */
    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>) {
        var lastEmittedId = 0L
        sharedFlow.collect { event ->
            if (event.id > lastEmittedId) {
                lastEmittedId = event.id
                collector.emit(event.value)
            }
        }
    }

    /**
     * 새로운 이벤트를 발행합니다.
     * 이벤트에 고유 ID를 부여한 후, SharedFlow에 emit합니다.
     */
    override suspend fun emit(value: T) {
        val id = lastId.incrementAndGet()
        sharedFlow.emit(EventWithId(id, value))
    }
}
