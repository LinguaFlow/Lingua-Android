package com.yju.presentation.view.speech

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.yju.presentation.R
import kotlin.random.Random
import androidx.core.content.withStyledAttributes

/**
 * 음성 메시지 파형을 표시하는 커스텀 뷰
 * TTS 재생 상태에 따라 파형의 진행 상황을 시각적으로 표시
 */
class AudioWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 파형 속성
    private var waveColor = 0xFFCCCCCC.toInt()              // 기본 파형 색상
    private var waveProgressColor = 0xFF1A73E8.toInt()      // 진행 중인 파형 색상
    private var waveCount = 10                              // 파형 개수
    private var waveWidth = 3f                              // 파형 너비
    private var waveHeight = 24f                            // 최대 파형 높이
    private var waveGap = 2f                                // 파형 간격
    private var randomizeHeights = true                     // 파형 높이 랜덤화 여부
    private var speedFactor = 1.5f                          // 속도 조절 계수 (추가됨)

    // 그리기 관련 변수
    private val paint = Paint()
    private val waveHeights = mutableListOf<Float>()        // 각 파형의 높이
    private var progress = 0f                               // 현재 진행률 (0.0 ~ 1.0)

    // 애니메이션 관련 변수
    private var animator: ValueAnimator? = null
    private var isPlaying = false
    private var animationDuration = 0L                      // 애니메이션 총 시간 (밀리초)
    private var elapsedTime = 0L                            // 경과 시간 (밀리초)

    init {
        // XML 속성 가져오기
        context.withStyledAttributes(attrs, R.styleable.AudioWaveView) {
            waveColor = getColor(R.styleable.AudioWaveView_waveColor, waveColor)
            waveProgressColor =
                getColor(R.styleable.AudioWaveView_waveProgressColor, waveProgressColor)
            waveCount = getInteger(R.styleable.AudioWaveView_waveCount, waveCount)
            waveWidth = getDimension(R.styleable.AudioWaveView_waveWidth, waveWidth)
            waveHeight = getDimension(R.styleable.AudioWaveView_waveHeight, waveHeight)
            waveGap = getDimension(R.styleable.AudioWaveView_waveGap, waveGap)
            randomizeHeights =
                getBoolean(R.styleable.AudioWaveView_randomizeHeights, randomizeHeights)
            // 속도 계수 속성 추가 (필요한 경우)
            // speedFactor = getFloat(R.styleable.AudioWaveView_speedFactor, speedFactor)
        }

        // 페인트 초기화
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        // 초기 파형 높이 생성
        generateWaveHeights()
    }

    /**
     * 파형 높이 생성
     */
    private fun generateWaveHeights() {
        waveHeights.clear()
        if (randomizeHeights) {
            // 랜덤 높이 생성 (최소 30% ~ 최대 100%)
            for (i in 0 until waveCount) {
                val heightPercentage = Random.nextFloat() * 0.7f + 0.3f
                waveHeights.add(waveHeight * heightPercentage)
            }
        } else {
            // 고정 높이 사용
            for (i in 0 until waveCount) {
                waveHeights.add(waveHeight)
            }
        }
    }

    /**
     * 속도 계수 설정 메소드 (추가됨)
     * @param factor 속도 증가 계수 (1.0 = 정상 속도, 2.0 = 2배 빠름)
     */
    fun setSpeedFactor(factor: Float) {
        this.speedFactor = factor.coerceAtLeast(0.1f) // 너무 작은 값 방지
    }

    /**
     * TTS 진행 시간 설정 및 애니메이션 시작
     * @param duration 전체 재생 시간 (밀리초)
     */
    fun startAnimation(duration: Long) {
        stopAnimation()

        // 속도 계수를 적용하여 애니메이션 시간 조정 (추가됨)
        val adjustedDuration = (duration / speedFactor).toLong()

        animationDuration = duration  // 원래 오디오 길이는 그대로 유지
        elapsedTime = 0L
        progress = 0f
        isPlaying = true

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = adjustedDuration  // 조정된 시간으로 애니메이션 실행
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                progress = valueAnimator.animatedValue as Float
                elapsedTime = (progress * duration).toLong()  // 원래 시간 기준으로 경과 시간 계산
                invalidate()
            }
            start()
        }
    }

    /**
     * 애니메이션 일시 정지
     */
    fun pauseAnimation() {
        isPlaying = false
        animator?.pause()
    }

    /**
     * 애니메이션 재개
     */
    fun resumeAnimation() {
        isPlaying = true
        animator?.resume()
    }

    /**
     * 애니메이션 중지 및 리셋
     */
    fun stopAnimation() {
        isPlaying = false
        animator?.cancel()
        animator = null
        progress = 0f
        elapsedTime = 0L
        invalidate()
    }

    /**
     * 현재 진행률 설정 (0.0 ~ 1.0)
     */
    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    /**
     * 현재 진행 시간 가져오기 (밀리초)
     */
    fun getCurrentTime(): Long = elapsedTime

    /**
     * 전체 재생 시간 가져오기 (밀리초)
     */
    fun getTotalDuration(): Long = animationDuration

    /**
     * 재생 중인지 확인
     */
    fun isPlaying(): Boolean = isPlaying

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 뷰의 중앙 Y 좌표 계산
        val centerY = height / 2f

        // 각 파형 그리기
        val totalWidth = waveCount * (waveWidth + waveGap) - waveGap
        val startX = (width - totalWidth) / 2f

        for (i in 0 until waveCount) {
            val waveHeight = waveHeights[i]
            val x = startX + i * (waveWidth + waveGap)

            // 현재 진행률에 따라 색상 결정
            val progressPosition = (waveCount * progress).toInt()
            paint.color = if (i <= progressPosition) waveProgressColor else waveColor

            // 파형 그리기 (수직 막대)
            canvas.drawRect(
                x,
                centerY - waveHeight / 2,
                x + waveWidth,
                centerY + waveHeight / 2,
                paint
            )
        }
    }

    /**
     * 뷰 크기가 변경될 때 호출
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 필요하다면 여기서 파형 크기 조정
    }
}