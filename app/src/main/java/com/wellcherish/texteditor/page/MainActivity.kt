package com.wellcherish.texteditor.page

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.databinding.ActivityMainBinding
import com.wellcherish.texteditor.mainlist.MainAdapter
import com.wellcherish.texteditor.viewmodel.MainViewModel

/**
 * 文件列表页
 * */
class MainActivity : AppCompatActivity() {
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
        mBinding.rv.adapter = MainAdapter().apply {
            adapter = this
        }
        mBinding.rv.layoutManager = StaggeredGridLayoutManager(
            ConfigManager.spanCount,
            RecyclerView.VERTICAL
        )
    }

    private fun initData() {
        viewModel.dataListLiveData.observe(this) {
            it ?: return@observe
            adapter?.submitList(it)
        }
    }

    private fun startEditorPage() {
        startActivity(Intent(this, EditorActivity::class.java))
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}