package com.wellcherish.texteditor.page

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wellcherish.texteditor.R
import com.wellcherish.texteditor.bean.FileItem
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.databinding.ActivityMainBinding
import com.wellcherish.texteditor.mainlist.MainAdapter
import com.wellcherish.texteditor.utils.DataManager
import com.wellcherish.texteditor.utils.stringRes
import com.wellcherish.texteditor.viewmodel.MainViewModel

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

        viewModel.init()
        initView()
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        adapter = null
    }

    private fun initView() {
        val mBinding = binding ?: return
        mBinding.rv.apply {
            adapter = MainAdapter(::noDoubleClick).apply { this@MainActivity.adapter = this }
            layoutManager = StaggeredGridLayoutManager(ConfigManager.spanCount, RecyclerView.VERTICAL)
        }

        mBinding.mainToolbar.apply {
            initToolbar(
                this,
                onAddClick = {
                    startEditorPage()
                },
                onSettingClick = {

                }
            )
            setShowSave(false)
        }
    }

    private fun initData() {
        viewModel.dataListLiveData.observe(this) {
            binding?.tvTextCountTips?.text = getFileCountHint(it?.size ?: 0)
            it ?: return@observe
            adapter?.submitList(it)
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

    companion object {
        private const val TAG = "MainActivity"
    }
}