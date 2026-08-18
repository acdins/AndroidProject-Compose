package com.joker.kit.feature.main.viewmodel

import com.joker.kit.core.base.viewmodel.BaseViewModel
import com.joker.kit.feature.main.model.TopLevelDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 主界面 ViewModel
 *
 * @author Joker.X
 */
@HiltViewModel
class MainViewModel @Inject constructor(
) : BaseViewModel() {

    /** 当前底部导航页签索引状态源 */
    private val _currentPageIndex = MutableStateFlow(0)

    /** 对外暴露的当前底部导航页签索引 */
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    /**
     * 更新当前选中的导航项
     *
     * @param index 导航项索引
     * @author Joker.X
     */
    fun updateDestination(index: Int) {
        if (index !in TopLevelDestination.entries.indices) return
        if (_currentPageIndex.value == index) return
        _currentPageIndex.value = index
    }
}
