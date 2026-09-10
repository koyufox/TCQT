package com.owo233.tcqt.core.env

/**
 * 模块内部组件的**类名字符串**常量。
 *
 * 存在的理由：功能侧需要在运行时按名字加载这些类
 * （`ClassLoader.loadClass` / `Intent.setClassName`），却不应 import `ui` 层的类。
 * 注意这里只放字符串 —— 一旦有人把类本身搬走，这些常量必须同步更新。
 */
object ModuleComponents {

    /** 模块设置主界面（`ui/settings/SettingActivity.kt`）。 */
    const val SETTINGS_ACTIVITY: String = "com.owo233.tcqt.ui.settings.SettingActivity"

    /** 通知渠道管理页（`ui/settings/NotificationChannelManagerActivity.kt`）。 */
    const val NOTIFICATION_CHANNEL_ACTIVITY: String =
        "com.owo233.tcqt.ui.settings.NotificationChannelManagerActivity"
}
