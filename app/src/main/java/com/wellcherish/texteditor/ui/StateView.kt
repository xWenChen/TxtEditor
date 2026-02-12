package com.wellcherish.texteditor.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.wellcherish.texteditor.R
import com.wellcherish.texteditor.databinding.StateViewBinding
import com.wellcherish.texteditor.utils.ZLog

class StateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding: StateViewBinding
    private var animation: ObjectAnimator? = null

    var state: State = State.NONE
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            changeState(value)
        }

    init {
        binding = StateViewBinding.inflate(LayoutInflater.from(context), this ,true)
        animation = createRotateAnim(binding?.ivState)
        changeState(State.NONE)
    }

    fun onDestroy() {
        stopStateAnim()
        animation = null
    }

    fun showLoading() {
        state = State.LOADING
    }

    fun hideLoading() {
        hide()
    }

    fun showEmptyPage() {
        state = State.EMPTY_PAGE
    }

    fun hideEmptyPage() {
        hide()
    }

    fun hide() {
        state = State.NONE
    }

    private fun changeState(newState: State) {
        when(newState) {
            State.LOADING -> {
                this.isVisible = true
                binding.ivState.setImageResource(R.drawable.ic_loading)
                binding.tvStateTips.setText(R.string.data_syncing_tips)
                startStateAnim()
            }
            State.EMPTY_PAGE -> {
                this.isVisible = true
                binding.ivState.setImageResource(R.drawable.ic_no_file)
                binding.tvStateTips.setText(R.string.empty_page_tips)
                stopStateAnim()
            }
            else -> {
                // State.NONE
                this.isVisible = false
                stopStateAnim()
            }
        }
    }

    private fun startStateAnim() {
        animation?.apply {
            if (!isStarted) {
                start()
            }
        }
    }

    private fun stopStateAnim() {
        runCatching {
            animation?.apply {
                if (isStarted) {
                    cancel()
                }
            }
        }.onFailure {
            ZLog.e(TAG, it)
        }
    }

    private fun createRotateAnim(view: View?): ObjectAnimator? {
        view ?: return null
        // 创建动画：从 0 度旋转到 360 度
        return ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply {
            duration = 1000               // 持续时间 1 秒
            repeatCount = ObjectAnimator.INFINITE // 无限循环
            repeatMode = ObjectAnimator.RESTART  // 每次从头开始
            interpolator = LinearInterpolator()  // 匀速转动
        }
    }

    companion object {
        private const val TAG = "StateView"
    }
}

enum class State {
    /**
     * 空布局
     * */
    EMPTY_PAGE,
    /**
     * 加载态
     * */
    LOADING,
    /**
     * 没有状态，布局需要隐藏
     * */
    NONE
}