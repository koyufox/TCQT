package com.owo233.tcqt.features.internal.pipeline

import com.owo233.tcqt.api.PipelineDecorator
import com.owo233.tcqt.core.action.ActionRegistry

/**
 * 管线装饰器的**能力发现**注册表。
 *
 * ## 为什么需要它
 *
 * 三条管线曾经各自硬编码装饰器清单：
 *
 * ```kotlin
 * private val decorators = arrayOf(ShowMsgInfo, RecallHeaderTip(), AitChameleon)
 * ```
 *
 * 这既让"新增一个视图装饰器"必须去改管线（本仓库最初的核心痛点），也**违反了
 * spec §3.6**：「禁止 `features.*` 之间互相 import（`features/internal/pipeline/`
 * 只允许经 `Registry` 能力发现）」—— 管线当时直接 import 了 `features.chat.*`
 * 与 `features.message.*`（共 6 处）。
 *
 * 现在管线只认 `OnXxx` 接口，装饰器由两类来源发现：
 *
 * 1. **注册 Action**：`ActionRegistry` 里实现了该接口的功能，自动被发现；
 * 2. **非注册装饰器**：由 [register] 显式登记（见下）。
 *
 * ## 非注册装饰器为什么必须显式登记
 *
 * `RecallHeaderTip` 不是注册 Action（它没有自己的功能开关，只是
 * `MsgAntiRecall` 的顶部提醒渲染器）。"普通类 + 能力发现"必须有**一个注册点**，
 * 而管线又不能 import 它所在的 `features.message` —— 否则禁令就白定了。
 *
 * 因此登记发生在 `loader/ModuleLoader`：`loader` 是唯一允许同时依赖 `features`
 * 与 `core` 的层，且它的两条启动路径都会在 `HookSteps.initStartup` 之前调用
 * `installPipelineDecorators()`，时机确定（早于任何管线 `install()`）。
 *
 * ## 顺序
 *
 * 结果按 [PipelineDecorator.decoratorOrder] 稳定排序 —— 显式、可 review，
 * 不依赖注册表返回的类名字典序。
 */
internal object PipelineDecorators {

    private val registered = mutableListOf<PipelineDecorator>()

    /** 登记非注册装饰器。由 `loader` 在启动早期调用，重复登记会被忽略。 */
    fun register(vararg decorators: PipelineDecorator) {
        decorators.forEach { if (it !in registered) registered += it }
    }

    /** 发现全部实现了 [type] 的装饰器，按 `decoratorOrder` 稳定排序。 */
    fun <T : PipelineDecorator> all(type: Class<T>): List<T> {
        val fromRegistry = ActionRegistry.allActionClasses()
            .mapNotNull { ActionRegistry.instanceOf(it) }

        return (fromRegistry + registered)
            .filterIsInstance<PipelineDecorator>()
            .filter { type.isInstance(it) }
            .sortedBy { it.decoratorOrder }
            .map { type.cast(it) }
    }
}
