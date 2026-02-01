package com.wellcherish.texteditor.page

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.utils.*
import com.wellcherish.texteditor.viewmodel.MainViewModel
import com.wellcherish.txteditor.R
import com.wellcherish.txteditor.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    private fun initView() {
        val mBinding = binding ?: return

        mBinding.editText.apply {
            filters = arrayOf(InputMaxCountFilter(ConfigManager.texMaxCount, ::showTextLimitTips))
            doAfterTextChanged {
                // 文本发生变化后，保存状态重置。
                viewModel.changeContentSaveState(SaveState.NOT_SAVE)
            }
        }

        // 拦截back按钮点击，先尝试保存文本。
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch(Dispatchers.autoSave) {
                    // 点击返回按钮时，先进行保存，再响应返回按钮的点击操作。
                    viewModel.saveText(getContent(), ::onSavedFail)
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        viewModel.contentSaveState.observe(this) {
            changeSaveStateUI(mBinding, it)
        }
    }

    /**
     * 变更保存状态的提示UI。
     * */
    private fun changeSaveStateUI(mBinding: ActivityMainBinding, newState: SaveState?) {
        val saved = newState == SaveState.SAVED
        val color = when (newState) {
            SaveState.SAVED -> R.color.green.colorRes
            SaveState.SAVING -> R.color.light_orange_300
            else -> R.color.red_50.colorRes
        }
        mBinding.tips.apply {
            tvSaveState.setText(
                when (newState) {
                    SaveState.SAVED -> R.string.saved
                    SaveState.SAVING -> R.string.saving
                    else -> R.string.not_save
                }
            )
            tvSaveState.setTextColor(color)

            ivSaveState.setImageResource(
                when (newState) {
                    SaveState.SAVED -> R.drawable.ic_ok
                    SaveState.SAVING -> R.drawable.ic_saving
                    else -> R.drawable.ic_warning
                }
            )
            ivSaveState.setColorFilter(color)
        }
    }

    private fun initData() {
        viewModel.startAutoSave(::getContent, ::onSavedFail)
    }

    private fun getContent(): CharSequence? {
        return binding?.editText?.text
    }

    private fun onSavedFail() {

    }

    /**
     * 提示文本打到最大数量限制。
     * */
    private fun showTextLimitTips() {
        val view = binding?.editText ?: return ZLog.e(TAG, "showTextLimitTips, view=null")
        Snackbar.make(
            view,
            R.string.text_count_to_limit.stringRes,
            Snackbar.LENGTH_LONG
        ).apply {
            setActionTextColor(R.color.light_orange_300.colorRes)
            show();
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}