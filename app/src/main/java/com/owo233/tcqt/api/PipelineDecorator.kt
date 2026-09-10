package com.owo233.tcqt.api

/**
 * 管线装饰器：由 `features/internal/pipeline/` 下的三条管线按能力驱动。
 *
 * ## 为什么需要它
 *
 * 三条管线（`AIOSendMsgBefore` / `AIOViewUpdate` / `MenuBuilder`）过去这样挑装饰器：
 *
 * ```kotlin
 * decorators
 *     .filterIsInstance<ActionSpec>()           // 1) 装饰器被迫实现 ActionSpec
 *     .filter { it.canRun() && it.onInit() } // 2) 副作用被塞进 onInit()
 *     .filterIsInstance<OnXxx>()
 * ```
 *
 * 两个毛病：
 *
 * 1. **1) 让 `ActionSpec` 删不掉** —— 装饰器只因为"要被认出来"就得实现整个功能契约。
 * 2. **2) 是基线缺陷 D7 的成因** —— 非注册装饰器（`RecallHeaderTip`）的 `install()`
 *    永远不会被框架调用（它不在注册清单里），于是作者把"注册监听器"这种副作用
 *    塞进了本该是纯判断的 `onInit()`，靠管线筛选时顺带执行。
 *
 * 现在把两件事分开：[isAvailable] 只回答"参不参与"，[activate] 负责安装。
 *
 * ```kotlin
 * class RecallHeaderTip : OnAIOViewUpdate {
 *     override fun isAvailable() = featureEnabled && AntiRecallConfig.isTopTipEnabled()
 *     override fun activate() { if (!registered) RecallManager.addListener(::onRecalled) }
 * }
 * ```
 *
 * ## 注册 Action 的装饰器怎么用
 *
 * [Feature] 已经实现了本接口：`isAvailable()` 等于框架启动路径上的
 * `canRun() && onInit()`，而 `activate()` 保持空实现 —— 注册 Action 的安装由框架
 * 走它自己的 `install()`，管线不能也不该重复触发。
 */
interface PipelineDecorator {

    /**
     * 装配顺序，小的排在前面。
     *
     * **必须显式声明，不要依赖枚举顺序。** 装饰器由能力发现从注册表取出，而注册表
     * 的顺序就是 KSP 的类名字典序 —— 依赖它意味着"改个类名"会静默改变菜单项或
     * 视图条的先后，那是最难查的一类回归。同序时按发现顺序稳定排序。
     */
    val decoratorOrder: Int get() = 1000

    /**
     * 是否参与本次管线装配。
     *
     * **必须是纯函数**：它在 `install()` 期间被调用，而 D7 的教训正是
     * "可用性判断里跑副作用"。
     */
    fun isAvailable(): Boolean = true

    /**
     * 被判定为可用之后调用**一次**。
     *
     * 只有"非注册装饰器"需要覆写它（它们没有 `install()` 可被框架调用）。
     */
    fun activate() = Unit
}
