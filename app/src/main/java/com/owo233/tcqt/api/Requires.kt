package com.owo233.tcqt.api

import com.owo233.tcqt.core.env.HookEnv
import com.owo233.tcqt.core.hook.HookEngineManager
import com.owo233.tcqt.core.hook.HookFramework

/**
 * 声明式的功能可用性条件。
 *
 * 取代 `ActionSpec.onInit()` 里的手写判断。原契约要求 `onInit` 是**纯条件判断**，
 * 但框架会从设置界面路径调用它（`FeatureCatalog.isInitReady`），
 * 而 `features/message/RecallHeaderTip.kt` 曾在其中注册监听器（基线缺陷 D7）——
 * 那是陷阱：任何人把带副作用的判断注册成正式功能就会踩中。
 *
 * 现在 `Feature.onInit()` 是 final，作者**无法**在其间插入副作用；
 * 可用性只能用这个数据类声明。
 *
 * ```kotlin
 * object ShowPreciseBanTime : Feature(
 *     key = "show_precise_ban_time",
 *     name = "显示精准禁言时间",
 *     requires = Requires(host = Requires.Host.QQOnly, minQQVersion = QQVersion.QQ_9_1_75),
 * )
 * ```
 */
data class Requires(
    /** QQ 最低版本号；0 表示不限制。TIM 宿主视为不满足。 */
    val minQQVersion: Long = 0L,

    /** TIM 最低版本号；0 表示不限制。QQ 宿主视为不满足。 */
    val minTimVersion: Long = 0L,

    /** 宿主类型限制。 */
    val host: Host = Host.Any,

    /** 是否仅在 NT 架构宿主上可用。 */
    val ntOnly: Boolean = false,

    /**
     * 是否排除 Zygisk 引擎。
     *
     * Zygisk 注入路径没有「Xposed 模块更新后重启宿主」的语义，因此
     * `ModuleUpdate` 在这条路径上必须视为不可用。
     */
    val nonZygiskOnly: Boolean = false,

    /**
     * 逃生口：上面所有字段都表达不了时的**纯**谓词条件。
     *
     * `LiquidGlassTabBar` 需要「NT 宿主 **且**（用户选了新视图 **或** 系统支持
     * RuntimeShader）」这种复合条件，无法拆成一个个独立字段。
     *
     * 两条纪律：
     * 1. **必须是纯函数** —— 这里等价于旧契约的 `onInit`，而 D7 的教训正是
     *    有人在 `onInit` 里注册监听器；[evaluate] 会在设置界面路径上被调用。
     * 2. **优先用上面的声明式字段** —— 只有复合条件才用这个。
     */
    val extraCondition: (() -> Boolean)? = null,
) {

    enum class Host { Any, QQOnly, TimOnly }

    /** 求值。保持纯函数：不产生任何副作用。 */
    fun evaluate(): Boolean {
        if (ntOnly && !HookEnv.isNT()) return false

        if (nonZygiskOnly && HookEngineManager.engine.frameworkName == HookFramework.ZYGISK) {
            return false
        }

        when (host) {
            Host.QQOnly -> if (!HookEnv.isQQ()) return false
            Host.TimOnly -> if (!HookEnv.isTIM()) return false
            Host.Any -> Unit
        }

        if (minQQVersion > 0L && !HookEnv.requireMinQQVersion(minQQVersion)) return false
        if (minTimVersion > 0L && !HookEnv.requireMinTimVersion(minTimVersion)) return false

        if (extraCondition != null && !extraCondition.invoke()) return false

        return true
    }


    companion object {
        /** 无任何限制。 */
        val None: Requires = Requires()
    }
}
