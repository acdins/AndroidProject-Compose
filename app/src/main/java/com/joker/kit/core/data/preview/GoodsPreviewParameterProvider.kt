package com.joker.kit.core.data.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.joker.kit.core.model.entity.Goods

/**
 * 商品列表预览数据
 */
val previewGoods = listOf(
    Goods(
        id = 1,
        title = "小米手机 14",
        subTitle = "直屏旗舰",
        price = 3999,
        sold = 5000,
    ),
    Goods(
        id = 2,
        title = "Apple AirPods",
        subTitle = "二代",
        price = 1299,
        sold = 8000,
    ),
    Goods(
        id = 3,
        title = "Switch OLED",
        subTitle = "游戏机",
        price = 2599,
        sold = 3000,
    ),
)

/**
 * 商品列表预览参数提供者
 *
 * @author Joker.X
 */
class GoodsPreviewParameterProvider : PreviewParameterProvider<List<Goods>> {

    override val values: Sequence<List<Goods>>
        get() = sequenceOf(
            previewGoods, // 正常列表
            listOf(previewGoods.first()), // 单条数据
            emptyList(), // 空列表
        )
}
