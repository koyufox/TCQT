package com.owo233.tcqt.api

import com.owo233.tcqt.core.action.ActionPriority
import com.owo233.tcqt.core.action.ActionProcess
import com.owo233.tcqt.core.action.ActionSpec

/**
 * 基础设施任务：模块自身的骨架，**不面向用户、不进设置界面、无视开关、永远运行**。
 *
 * 取代旧契约的 `core.action.AlwaysRunAction`。这 7 个功能（菜单构建、发送前管线、
 * 视图更新管线、命令总线、账号变更监听、Web JS 桥、加号菜单管理）的"开关"没有
 * 意义 —— 关掉它们会让一批正常功能连带失效，所以设置界面也不该展示。
 *
 * 与 [Feature] 的差别**只有两件事**：`hidden = true` 与 `canRun() = true`。
 * 配置项声明、[Requires] 可用性、[install] 的写法与 [Feature] 完全一致。
 *
 * ```kotlin
 * @RegisterAction
 * object MenuBuilder : InfraTask(
 *     key = "MenuBuilder",
 *     priority = ActionPriority.BACKGROUND,
 *     requires = Requires(ntOnly = true),
 * ) {
 *     override fun install() { … }
 * }
 * ```
 *
 * ## 为什么不是 `Feature(hidden = true)` 这类参数
 *
 * `ActionSpec.hidden` 与 `ActionSpec.canRun()` 本来就是可覆写的接口成员，因此这里直接
 * 覆写即可，`Feature` 一个参数都不用加 —— 90 个功能里只有这 7 个用到这种语义，
 * 不值得把它变成所有人都能误用的构造参数。
 */
abstract class InfraTask(
    key: String,
    name: String = "",
    desc: String = "",
    processes: Set<ActionProcess> = ActionSpec.DEFAULT_PROCESSES,
    priority: ActionPriority = ActionPriority.DEFERRED,
    requires: Requires = Requires.None,
) : Feature(
    key = key,
    name = name,
    desc = desc,
    processes = processes,
    priority = priority,
    requires = requires,
) {

    /** 不在设置界面展示。 */
    final override val hidden: Boolean = true

    /** 无视设置开关：基础设施任务没有"关掉"这个状态。 */
    final override fun canRun(): Boolean = true
}
