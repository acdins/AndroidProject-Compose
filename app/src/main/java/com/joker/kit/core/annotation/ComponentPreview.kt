package com.joker.kit.core.annotation

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * 组件浅色主题预览（无设备框架）
 */
@Preview(showBackground = true)
annotation class ComponentPreview

/**
 * 组件深色主题预览（无设备框架）
 */
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ComponentPreviewDark

/**
 * 组件浅色 + 深色主题预览（无设备框架）
 */
@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ComponentPreviewLightAndDark
