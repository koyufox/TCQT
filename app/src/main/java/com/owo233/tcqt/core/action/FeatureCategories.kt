package com.owo233.tcqt.core.action

import com.owo233.tcqt.core.action.FeatureCategories.LABELS


/**
 * **目录即分类**：功能在设置界面里的归属，只由它所在的包决定。
 *
 * ## 为什么放在 `core.action` 而不是 `features`
 *
 * 读它的是 `ui.settings.FeatureCatalog`，而 spec §3.1 规定 **`ui` 不得依赖
 * `features` / `api`** —— 它只能依赖 `core` 与 `host`。表放这里，`ui` 才够得着。
 *
 * ## 为什么要有这张表
 *
 * 在这之前，分类是一个写在**每个功能构造参数里的中文字符串**
 * （`uiTab = "界面"`），与功能实际所在的目录**各说各话**：目录早就按分类组织好了，
 * 而设置界面读的是那 83 个字符串。两处维护 ⇒ 必然漂移，正是本仓库最初的痛点
 * 「目录树与设置界面分类脱节」。
 *
 * 现在唯一的真相是**包路径**；这张表只负责把包名翻译成给人看的标签。
 * 改分类 = 搬文件，或者改这一张表 —— 没有第三处。
 *
 * ## 一个必须守住的构建假设
 *
 * 运行期取包名用的是 `javaClass.name`（见 `ActionSpec.uiTab`）。这要求 **R8 不做包
 * 扁平化** —— 也就是**不能**加 `-repackageclasses` / `-flattenpackagehierarchy`。
 * 当前项目没有任何 ProGuard 规则文件，release 的 `optimization { enable = true }`
 * 也不会开这两条，所以假设成立。
 *
 * 一旦有人加上它们，所有功能的 `javaClass.name` 会丢掉 `features/<分类>/` 这一段，
 * 于是**全部静默掉进 [FALLBACK]** —— 设置界面只剩一个分类。改构建配置前请先看这段。
 */
object FeatureCategories {

    /**
     * 包的最后一段 → 设置界面分类标签。
     *
     * 键与 `features/<key>/` 目录一一对应，顺序即 [ORDER]。
     */
    private val LABELS: Map<String, String> = linkedMapOf(
        "general" to "基础",
        "chat" to "聊天",
        "message" to "消息操作",
        "menu" to "菜单与入口",
        "appearance" to "外观",
        "cleanup" to "净化",
        "advanced" to "高级",
        "notification" to "通知",
        "misc" to "杂项",
        "debug" to "调试",
    )

    /** 设置界面根列表的展示顺序（= [LABELS] 的声明顺序）。 */
    val ORDER: List<String> = LABELS.values.toList()

    /**
     * 包不在表内时的兜底标签。
     *
     * 只应出现在"未注册/隐藏"的类上（例如 `features/internal` 下的管线与
     * 基础设施任务）—— 它们不会进设置界面。可见功能若走到这里，
     * `CategoryConsistencyTest` 会报错。
     */
    const val FALLBACK = "基础"

    /** 取全限定类名所在包的最后一段（即分类键）。 */
    fun segmentOf(className: String): String =
        className.substringBeforeLast('.', "").substringAfterLast('.')

    /** 全限定类名 → 分类标签；包不在表内返回 null。 */
    fun labelOf(className: String): String? = LABELS[segmentOf(className)]

    /** 包是否已在分类表里登记。 */
    fun isKnown(className: String): Boolean = labelOf(className) != null
}
