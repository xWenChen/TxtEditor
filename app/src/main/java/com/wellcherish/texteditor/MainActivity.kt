package com.wellcherish.texteditor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.wellcherish.texteditor.databinding.ActivityMainBinding
import com.wellcherish.texteditor.utils.SaveState
import com.wellcherish.texteditor.utils.colorRes
import com.wellcherish.texteditor.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(this.root)
            binding = this
        }
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    private fun initView() {
        val mBinding = binding ?: return

        viewModel.contentSaveState.observe(this) {
            changeSaveStateUI(mBinding, it)
        }
    }

    /**
     * 变更保存状态的提示UI。
     * */
    private fun changeSaveStateUI(mBinding: ActivityMainBinding, newState: SaveState?) {
        val saved = newState == SaveState.SAVED
        val color = if (saved) {
            R.color.green.colorRes
        } else {
            R.color.red_50.colorRes
        }
        mBinding.tips.apply {
            tvSaveState.setText(
                if (saved) {
                    R.string.saved
                } else {
                    R.string.not_save
                }
            )
            tvSaveState.setTextColor(color)

            ivSaveState.setImageResource(
                if (saved) {
                    R.drawable.ic_ok
                } else {
                    R.drawable.ic_warning
                }
            )
            ivSaveState.setColorFilter(color)
        }
    }
}