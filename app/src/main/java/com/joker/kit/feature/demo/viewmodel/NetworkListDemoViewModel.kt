package com.joker.kit.feature.demo.viewmodel

import com.joker.kit.core.base.viewmodel.BaseNetWorkListViewModel
import com.joker.kit.core.data.repository.GoodsRepository
import com.joker.kit.core.model.entity.Goods
import com.joker.kit.core.model.network.NetworkPageData
import com.joker.kit.core.model.network.NetworkResponse
import com.joker.kit.core.model.request.GoodsSearchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Network List Demo 示例页 ViewModel
 *
 * @param goodsRepository 商品数据仓库
 * @author Joker.X
 */
@HiltViewModel
class NetworkListDemoViewModel @Inject constructor(
    private val goodsRepository: GoodsRepository
) : BaseNetWorkListViewModel<Goods>(
) {

    /** 首屏加载至少保持基类规定的最短时长 */
    override val enableMinLoadingTime: Boolean get() = true

    /** 商品列表每页请求数量 */
    override val pageSize: Int get() = 15

    init {
        initLoad()
    }

    /**
     * 获取商品列表响应流
     *
     * @return 商品分页数据流
     * @author Joker.X
     */
    override fun requestListData(): Flow<NetworkResponse<NetworkPageData<Goods>>> {
        return goodsRepository.getGoodsPage(
            GoodsSearchRequest(
                page = currentPage,
                size = pageSize
            )
        )
    }
}
