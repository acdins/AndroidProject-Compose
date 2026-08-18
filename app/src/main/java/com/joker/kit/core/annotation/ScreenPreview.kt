package com.joker.kit.core.annotation

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * 页面浅色主题预览（手机尺寸）
 */
@Preview(name = "Phone", showBackground = true, device = Devices.PHONE)
annotation class ScreenPreview

/**
 * 页面深色主题预览（手机尺寸）
 */
@Preview(
    name = "Phone Dark",
    showBackground = true,
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ScreenPreviewDark

/**
 * 页面浅色主题预览（手机 + 平板）
 */
@Preview(name = "Phone", showBackground = true, device = Devices.PHONE)
@Preview(name = "Tablet", showBackground = true, device = Devices.TABLET)
annotation class ScreenPreviewPhoneAndTablet

/**
 * 页面深色主题预览（手机 + 平板）
 */
@Preview(
    name = "Phone Dark",
    showBackground = true,
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Tablet Dark",
    showBackground = true,
    device = Devices.TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ScreenPreviewPhoneAndTabletDark

/**
 * 页面浅色主题预览（手机 + 折叠屏 + 平板）
 */
@Preview(name = "Phone", showBackground = true, device = Devices.PHONE)
@Preview(name = "Foldable", showBackground = true, device = Devices.FOLDABLE)
@Preview(name = "Tablet", showBackground = true, device = Devices.TABLET)
annotation class ScreenPreviewMultiDevice

/**
 * 页面深色主题预览（手机 + 折叠屏 + 平板）
 */
@Preview(
    name = "Phone Dark",
    showBackground = true,
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Foldable Dark",
    showBackground = true,
    device = Devices.FOLDABLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Tablet Dark",
    showBackground = true,
    device = Devices.TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ScreenPreviewMultiDeviceDark
