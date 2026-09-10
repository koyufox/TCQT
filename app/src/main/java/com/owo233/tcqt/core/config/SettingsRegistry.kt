package com.owo233.tcqt.core.config

import com.owo233.tcqt.core.action.ActionRegistry
import com.owo233.tcqt.core.action.ActionUiType

/**
 * 配置注册：把每个 [com.owo233.tcqt.core.action.ActionSpec] 声明的配置项
 * 展开成 `TCQTSetting` 的底层存储包装。
 *
 * 从 `SettingsRegistry.registerAllInto` 拆出。原实现里
 * `TCQTSetting.settingMap` 的 lazy 初始化会回调 `ActionManager`，形成
 * `core.config ↔ core.action` 的隐式双向依赖（Spec §3.6）；现在方向是
 * 单向的：`config → action`。
 */
internal object SettingsRegistry {

    /**
     * 把全部 Action 的配置项注册进 [target]。
     *
     * 规则与原实现逐字一致：
     * - `uiType == SWITCH` 且 key 非空 → 把**功能开关本身**注册为 BOOLEAN
     * - 每个 `settings` 项按其声明类型注册
     */
    fun registerAllInto(target: HashMap<String, TCQTSetting.Setting<out Any>>) {
        ActionRegistry.allActionClasses().forEach { actionClass ->
            val action = ActionRegistry.instanceOf(actionClass) ?: return@forEach
            if (action.key.isNotBlank() && action.uiType == ActionUiType.SWITCH) {
                target[action.key] = TCQTSetting.Setting(
                    action.key,
                    TCQTSetting.SettingType.BOOLEAN,
                    action.defaultEnabled
                )
            }
            action.settings.forEach { s ->
                val type = when (s) {
                    is BooleanSetting -> TCQTSetting.SettingType.BOOLEAN
                    is StringSetting -> TCQTSetting.SettingType.STRING
                    is IntSetting -> TCQTSetting.SettingType.INT
                    is IntSliderSetting -> TCQTSetting.SettingType.INT
                    is MultiIntSetting -> TCQTSetting.SettingType.INT_MULTI
                }
                target[s.key] = TCQTSetting.Setting(
                    s.key,
                    type,
                    s.defaultValue
                )
            }
        }
    }
}
