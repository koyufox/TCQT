package com.owo233.tcqt.core.action

import android.app.Application
import android.content.Context
import com.owo233.tcqt.core.config.Setting
import com.owo233.tcqt.core.config.TCQTSetting
import com.owo233.tcqt.core.log.ActionErrorStore
import com.owo233.tcqt.core.log.Log

enum class ActionUiType {
    SWITCH, ENTRY
}

enum class ActionProcess {

    MSF, MAIN, TOOL, OPENSDK, QZONE, QQFAV,
    OTHER, ALL
}

/**
 * 功能启动优先级。
 *
 * 宿主 `BaseApplicationImpl.onCreate` 的 Before 回调里只会**同步**安装
 * [CRITICAL]，其余优先级由 [com.owo233.tcqt.core.action.StartupScheduler] 在
 * onCreate 返回后于后台分批安装，从而让「白屏时间」不再随启用功能数量线性增长。
 */
enum class ActionPriority {

    /**
     * 必须在宿主 Application.onCreate 返回之前同步安装。
     *
     * 只允许「目标方法在 onCreate 执行期间就会被调用，且第一次调用不能漏」的
     * 功能使用（例如 [com.owo233.tcqt.features.advanced.FileRecvRedirect]）。
     * 数量必须严格控制，否则白屏时间会随 CRITICAL 数量线性增长。
     */
    CRITICAL,

    /**
     * onCreate 返回后立刻安装（主线程 Handler post 后转入后台线程）。
     * 目标方法在 Activity / 登录流程早期被调用，但不会在 onCreate 内被调用。
     */
    EARLY,

    /**
     * 默认值。MAIN 进程等首帧后、后台进程立刻，分批在后台线程安装。
     * 目标方法在用户与界面交互之后才会被调用（聊天、设置、WebView 等）。
     */
    DEFERRED,

    /** 最后一批安装，允许与其他初始化错峰。 */
    BACKGROUND,
}

/**
 * **注册表与设置界面的内部管道契约 —— 功能作者不要实现它。**
 *
 * 功能作者唯一该继承的是 `com.owo233.tcqt.api.Feature`（或 `api.InfraTask`），
 * 它实现本接口并额外提供派生 key、声明式 `Requires`、`install()` 约定与
 * `PipelineDecorator` 能力。`SingleAuthoringContractTest` 机械地保证了
 * `features/` 与 `host/` 下不会出现直接实现本接口的类。
 *
 * ## 为什么它没有被删掉
 *
 * S2b 之后曾计划"删掉本接口，让 `Feature` 独立存在"。实施时发现这与 spec §3.1
 * 的层次表冲突：
 *
 * - `core` 不得依赖 `api`，而 [ActionRegistry] 就在 `core.action`，它必须持有功能类型；
 * - `ui` 也不得依赖 `api`，而 `ui.settings.FeatureCatalog` 要读功能的
 *   `key/name/desc/uiTab/uiOrder/uiType/hidden/settings`。
 *
 * 也就是说，注册表与设置界面需要一个**层次上够得着**的功能模型，而它只能放在
 * `core`。所以本接口不是"另一套与 `Feature` 并存的功能契约"，而是
 * "内部模型 + 作者门面"的分工。要么保留它，要么把注册表搬出 `core`
 * （会连带破坏 `ui` 的依赖方向）—— 前者是唯一不违反层次表的解。
 *
 * 详见 `docs/aegis/adr/ADR-006-action-plumbing-vs-authoring-contract.md`。
 */
interface ActionSpec {

    val key: String
    val name: String
    val desc: String get() = ""

    /**
     * 设置界面分类。**由包路径唯一决定（目录即分类），功能不再声明它。**
     *
     * 唯一真相是该类所在的 `features/<分类>/` 目录；[FeatureCategories] 只负责把
     * 包名翻译成给人看的标签。这样"搬文件"与"改分类"是同一件事，
     * 不会再出现目录树与设置界面各说各话。
     */
    val uiTab: String
        get() = FeatureCategories.labelOf(this.javaClass.name) ?: FeatureCategories.FALLBACK
    val uiOrder: Int get() = 1000
    val hidden: Boolean get() = false
    val defaultEnabled: Boolean get() = false
    val uiType: ActionUiType get() = ActionUiType.SWITCH

    val settings: List<Setting<*>> get() = emptyList()

    val processes: Set<ActionProcess> get() = DEFAULT_PROCESSES

    /**
     * 启动优先级，默认 [ActionPriority.DEFERRED]。
     *
     * 绝大多数功能不需要覆盖：只有目标方法在宿主
     * [android.app.Application.onCreate] 执行期间就会被调用、且第一次调用
     * 不能漏时，才应提升为 [ActionPriority.CRITICAL]。
     */
    val priority: ActionPriority get() = ActionPriority.DEFERRED

    /**
     * 获取配置项的动态描述
     * @param key 配置项键名
     * @return 动态描述内容，返回 null 时将使用静态描述作为后备
     */
    fun getSettingDesc(key: String): String? = null

    operator fun invoke(app: Application, process: ActionProcess) {
        ActionErrorStore.withAction(key) {
            // A host restart starts a fresh health check for this feature in
            // this process. Any failure below (or in a later hook callback)
            // writes the error back immediately.
            ActionErrorStore.clear(key, com.owo233.tcqt.core.env.HookEnv.processName)
            runCatching {
                if (canRun() && onInit()) {
                    onRun(app, process)
                }
            }.onFailure {
                ActionErrorStore.report(key, "功能初始化", it)
                Log.e("功能 [${ActionRegistry.resolve(this)}] 执行异常", it)
            }
        }
    }

    fun onRun(app: Application, process: ActionProcess)

    fun onUiClick(context: Context): Boolean = false

    fun canRun(): Boolean {
        return uiType != ActionUiType.ENTRY && runCatching {
            TCQTSetting.getValue<Boolean>(key) ?: defaultEnabled
        }.getOrElse { e ->
            ActionErrorStore.report(key, "开关检查", e)
            Log.e("功能 [${ActionRegistry.resolve(this)}] 开关检查异常", e)
            defaultEnabled
        }
    }

    /**
     * 功能执行条件判断（模块设置界面会调用它来决定是否强制禁用该功能）。
     *
     * 保持为纯条件判断，不要在这里执行 Hook 安装、配置写入等副作用；
     * 这类初始化逻辑应放在 [onRun] 中。
     *
     * @return true 表示满足执行条件，继续执行后续 onRun 函数；false 则不执行
     */
    fun onInit(): Boolean = true

    companion object {
        val DEFAULT_PROCESSES = setOf(ActionProcess.MAIN)
    }
}
