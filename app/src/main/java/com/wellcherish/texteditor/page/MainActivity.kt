package com.wellcherish.texteditor.page

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wellcherish.texteditor.R
import com.wellcherish.texteditor.database.bean.FileItem
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.databinding.ActivityMainBinding
import com.wellcherish.texteditor.mainlist.MainAdapter
import com.wellcherish.texteditor.model.FileEventBus
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
    private var animation: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(this.root)
            binding = this
        }

        lifecycleScope.launch(Dispatchers.Main) {
            animation = createRotateAnim(binding?.ivState)
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
        stopStateAnim()
        animation = null
        binding = null
        adapter = null
    }

    private fun initView() {
        val mBinding = binding ?: return
        mBinding.rv.apply {
            adapter = MainAdapter(::noDoubleClick).apply { this@MainActivity.adapter = this }
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
        val showState: Boolean
        when {
            viewModel.showLoading.value == true -> {
                showState = true
                mBinding.ivState.setImageResource(R.drawable.ic_loading)
                mBinding.tvStateTips.setText(R.string.data_syncing_tips)
                startStateAnim()
            }
            viewModel.showEmpty.value == true -> {
                showState = true
                mBinding.ivState.setImageResource(R.drawable.ic_no_file)
                mBinding.tvStateTips.setText(R.string.empty_page_tips)
                stopStateAnim()
            }
            else -> {
                showState = false
                stopStateAnim()
            }
        }

        mBinding.stateView.isVisible = showState
        mBinding.contentView.isVisible = !showState
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

    private fun getFileCountHint(listSize: Int): String {
        return String.format(R.string.file_count_tips.stringRes, listSize)
    }

    private fun noDoubleClick(view: View, position: Int, data: FileItem) {
        DataManager.chosenFileData = data
        startEditorPage()
    }

    private fun startEditorPage() {
        val intent = Intent(this, EditorActivity::class.java)
        startActivity(intent)
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
        private const val TAG = "MainActivity"
    }
}