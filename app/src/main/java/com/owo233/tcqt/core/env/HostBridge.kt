package com.owo233.tcqt.core.env

import android.app.Activity
import android.app.Application
import com.owo233.tcqt.core.log.Log
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 「上层能力注入 core」的唯一通道。
 *
 * core 不得依赖 `host` / `ui` / `loader`（Spec §3.1），但启动流程里 core
 * 确实需要两件只有上层才知道的事：当前顶层 Activity、以及宿主 Application
 * 就绪后的初始化钩子。这里把它们定义成空注册点，由 `loader` 在装配阶段填实现。
 *
 * 保持**窄**：目前只有 2 个注册点。若将来超过 3 个，应改为接口 + 多实现注册表。
 */
object HostBridge {

    private val hostApplicationReadyCallbacks =
        CopyOnWriteArrayList<(Application) -> Unit>()

    /**
     * 当前顶层 Activity 的提供者，由 `host` 侧注册。
     * 返回 null 表示此刻拿不到（未登录 / 进程刚起）。
     */
    @Volatile
    var topActivityProvider: (() -> Activity?)? = null

    fun topActivity(): Activity? =
        runCatching { topActivityProvider?.invoke() }.getOrNull()

    /** 注册「宿主 Application 已就绪」的回调；可注册多个，按注册顺序执行。 */
    fun onHostApplicationReady(block: (Application) -> Unit) {
        hostApplicationReadyCallbacks += block
    }

    /**
     * 通知宿主 Application 已就绪。由
     * [com.owo233.tcqt.core.action.HookSteps.initContext] 调用。
     * 单个回调抛异常不影响其它回调。
     */
    fun notifyHostApplicationReady(app: Application) {
        hostApplicationReadyCallbacks.forEach { block ->
            runCatching { block(app) }
                .onFailure { Log.e("HostBridge hostApplicationReady callback failed", it) }
        }
    }
}
