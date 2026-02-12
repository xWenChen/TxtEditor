package com.wellcherish.texteditor.page

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wellcherish.texteditor.R
import com.wellcherish.texteditor.bean.FileData
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.databinding.ActivityMainBinding
import com.wellcherish.texteditor.ui.MainAdapter
import com.wellcherish.texteditor.model.FileEventBus
import com.wellcherish.texteditor.ui.State
import com.wellcherish.texteditor.utils.*
import com.wellcherish.texteditor.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 文件列表页
 * */
class MainActivity : BaseActivity() {
    private var binding: ActivityMainBinding? = null
    private val viewModel: MainViewModel by viewModels()
    private var adapter: MainAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(this.root)
            binding = this
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.changeLoadingState(true)
            withContext(Dispatchers.IO) {
                FileSyncToDbManager.trySync()
                viewModel.init()
                FileEventBus.registerFileChangeListener(viewModel.onFileChanged)
            }
            viewModel.changeLoadingState(false)
            initView()
            initData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FileEventBus.unregisterFileChangeListener(viewModel.onFileChanged)
        binding?.stateView?.onDestroy()
        binding = null
        adapter = null
    }

    private fun initView() {
        val mBinding = binding ?: return
        mBinding.rv.apply {
            adapter = MainAdapter(::noDoubleClick, ::onLongClick, ::deleteItem).apply {
                this@MainActivity.adapter = this
            }
            layoutManager = StaggeredGridLayoutManager(ConfigManager.spanCount, RecyclerView.VERTICAL)
            setHasFixedSize(true)
        }

        mBinding.ivAdd.setNoDoubleClickListener {
            startEditorPage()
        }

        mBinding.mainToolbar.apply {
            initToolbar(
                this,
                onSettingClick = {

                }
            )
            setShowSave(false)
        }

        // 拦截back按钮点击，先尝试保存文本。
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch(Dispatchers.autoSave) {
                    // 点击返回按钮时，判断是否处于删除态，如果是，则退出删除态，否则退出页面。
                    withContext(Dispatchers.Main) {
                        if (viewModel.isDeleting) {
                            viewModel.changeDeletingState(false)
                        } else {
                            // 保存成功，退出页面。
                            this@MainActivity.finish()
                        }
                    }
                }
            }
        })
    }

    private fun initData() {
        viewModel.showLoading.observe(this) {
            checkPageState()
        }
        viewModel.showEmpty.observe(this) {
            checkPageState()
        }

        viewModel.dataListLiveData.observe(this) {
            binding?.tvTextCountTips?.text = getFileCountHint(it?.size ?: 0)
            if (it.isNullOrEmpty()) {
                viewModel.showEmpty.value = true
                return@observe
            }
            viewModel.showEmpty.value = false
            adapter?.submitList(it)
        }
    }

    /**
     * 页面状态优先级
     *
     * loading态 > 空布局 > 列表
     * */
    private fun checkPageState() {
        val mBinding = binding ?: return
        when {
            viewModel.showLoading.value == true -> {
                mBinding.stateView.showLoading()
            }
            viewModel.showEmpty.value == true -> {
                mBinding.stateView.showEmptyPage()
            }
            else -> {
                mBinding.stateView.hide()
            }
        }

        // stateView 和 contentView 的显示互斥。
        mBinding.contentView.isVisible = mBinding.stateView.state == State.NONE
    }

    private fun getFileCountHint(listSize: Int): String {
        return String.format(R.string.file_count_tips.stringRes, listSize)
    }

    private fun noDoubleClick(view: View, position: Int, data: FileData) {
        DataManager.chosenFileData = data
        startEditorPage()
    }

    private fun onLongClick(view: View, position: Int, data: FileData) {
        viewModel.changeDeletingState(true)
    }

    private fun deleteItem(view: View, position: Int, data: FileData) {
        viewModel.deleteItem(position, data)
    }

    private fun startEditorPage() {
        val intent = Intent(this, EditorActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}