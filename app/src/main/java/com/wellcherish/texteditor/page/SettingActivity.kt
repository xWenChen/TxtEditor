package com.wellcherish.texteditor.page

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wellcherish.base.ui.IView
import com.wellcherish.texteditor.databinding.ActivitySettingBinding
import com.wellcherish.texteditor.ui.SettingAdapter
import com.wellcherish.texteditor.ui.State
import com.wellcherish.texteditor.viewmodel.SettingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingActivity : BaseActivity(), IView {
    private var binding: ActivitySettingBinding? = null
    private val viewModel: SettingViewModel by viewModels()
    private var adapter: SettingAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivitySettingBinding.inflate(layoutInflater).apply {
            setContentView(this.root)
            binding = this
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.changeLoadingState(true)
            withContext(Dispatchers.IO) {
                viewModel.init(this@SettingActivity)
            }
            viewModel.changeLoadingState(false)
            initView()
            initData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    private fun initView() {
        val mBinding = binding ?: return
        mBinding.rv.apply {
            adapter = SettingAdapter().apply {
                this@SettingActivity.adapter = this
            }
            layoutManager = LinearLayoutManager(this@SettingActivity)
            setHasFixedSize(true)
        }

        mBinding.mainToolbar.apply {
            initToolbar(this)
            setShowTitle(true)
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
            if (it.isNullOrEmpty()) {
                viewModel.showEmpty.value = true
                return@observe
            }
            viewModel.showEmpty.value = false
            adapter?.submitList(it) {
                binding?.rv?.smoothScrollToPosition(0)
            }
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
        mBinding.rv.isVisible = mBinding.stateView.state == State.NONE
    }

    override fun getActivity(): AppCompatActivity? {
        return this
    }
}