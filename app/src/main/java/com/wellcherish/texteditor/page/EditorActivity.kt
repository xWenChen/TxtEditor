package com.wellcherish.texteditor.page

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.utils.*
import com.wellcherish.texteditor.viewmodel.EditorViewModel
import com.wellcherish.texteditor.R
import com.wellcherish.texteditor.databinding.ActivityEditorBinding
import com.wellcherish.texteditor.model.SaveState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 编辑页
 * */
class EditorActivity : BaseActivity() {
    private var binding: ActivityEditorBinding? = null
    private val viewModel: EditorViewModel by viewModels()

    /**
     * 是否跳过状态改变。首次进入页面时，可以跳过。
     * */
    private var skipChangeState: Boolean = false
    private var skipTitleChangeState: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityEditorBinding.inflate(layoutInflater).apply {
            setContentView(this.root)
            binding = this
        }
        initView()
        initData()
    }

    override fun onStop() {
        super.onStop()
        // 退出页面时，尝试保存下。
        lifecycleScope.launch(Dispatchers.autoSave) {
            // 点击返回按钮时，先进行保存，再响应返回按钮的点击操作。
            saveText()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
        binding = null
    }

    private fun initView() {
        val mBinding = binding ?: return

        updateTextCountTips()

        mBinding.title.apply {
            filters = arrayOf(InputMaxCountFilter(ConfigManager.titleMaxCount, ::showTextLimitTips))
            doAfterTextChanged {
                if (!skipTitleChangeState) {
                    // 文本发生变化后，保存状态重置。
                    viewModel.changeContentSaveState(SaveState.NOT_SAVE)
                }
                skipTitleChangeState = false
            }
        }

        mBinding.tvContent.apply {
            filters = arrayOf(InputMaxCountFilter(ConfigManager.texMaxCount, ::showTextLimitTips))
            doAfterTextChanged {
                // 更新字数提示
                updateTextCountTips()
                if (!skipChangeState) {
                    // 文本发生变化后，保存状态重置。
                    viewModel.changeContentSaveState(SaveState.NOT_SAVE)
                }
                skipChangeState = false
            }
        }

        // 拦截back按钮点击，先尝试保存文本。
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch(Dispatchers.autoSave) {
                    // 点击返回按钮时，先进行保存，再响应返回按钮的点击操作。
                    //saveText()
                    withContext(Dispatchers.Main) {
                        // 保存成功，退出页面。
                        this@EditorActivity.finish()
                    }
                }
            }
        })

        viewModel.contentSaveState.observe(this) {
            changeSaveStateUI(mBinding, it)
        }

        mBinding.mainToolbar.apply {
            initToolbar(
                this,
                onSaveClick = {
                    saveText()
                },
                onSettingClick = {

                }
            )
        }

        initContent()
    }

    private fun saveText() {
        viewModel.saveText(getContentTitle(), getContent(), ::onSavedFail)
    }

    private fun initContent() {
        val mBinding = binding ?: return

        val fileData = DataManager.chosenFileData
        DataManager.chosenFileData = null
        val filePath = fileData?.dbData?.filePath
        if (filePath == null) {
            ZLog.w(TAG, "initContent, filePath=null")
            return
        }
        skipChangeState = true
        skipTitleChangeState = true
        viewModel.currentOpenFile = fileData
        mBinding.title.setText(fileData.dbData?.title)
        mBinding.tvContent.setText(fileData.text)
    }

    private fun updateTextCountTips() {
        lifecycleScope.launch(Dispatchers.Main) {
            val text = withContext(Dispatchers.uiAsync) {
                getTextCountTips()
            }
            binding?.tips?.tvTextCountTips?.text = text
        }
    }

    private fun getTextCountTips(): String {
        // 剔除换行和回车符后再统计字数
        val filterText = binding?.tvContent?.text?.filter { it != '\n' && it != '\r' }
        return String.format(
            R.string.text_count_tips.stringRes,
            filterText?.length ?: 0,
            ConfigManager.texMaxCount
        )
    }

    /**
     * 变更保存状态的提示UI。
     * */
    private fun changeSaveStateUI(mBinding: ActivityEditorBinding, newState: SaveState?) {
        val color = when (newState) {
            SaveState.SAVED -> R.color.green.colorRes
            SaveState.SAVING -> R.color.light_orange_500.colorRes
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
        //viewModel.startAutoSave(::getContentTitle, ::getContent, ::onSavedFail)
    }

    private fun getContentTitle(): String? {
        return binding?.title?.text?.toString()
    }

    private fun getContent(): String {
        return binding?.tvContent?.text?.toString().orEmpty()
    }

    private fun onSavedFail() {

    }

    /**
     * 提示文本打到最大数量限制。
     * */
    private fun showTextLimitTips() {
        val view = binding?.tvContent ?: return ZLog.e(TAG, "showTextLimitTips, view=null")
        Snackbar.make(
            view,
            R.string.text_count_to_limit.stringRes,
            Snackbar.LENGTH_LONG
        ).apply {
            setBackgroundTint(R.color.main_green.colorRes)
            setTextColor(R.color.red_100.colorRes)
            show()
        }
    }

    companion object {
        private const val TAG = "EditorActivity"
    }
}