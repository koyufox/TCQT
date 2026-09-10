/**
 * 功能来自 https://github.com/callng/QAuxiliary
 * 代码提供者： HdShare
 */
package com.owo233.tcqt.features.chat

import android.widget.TextView
import com.owo233.tcqt.annotations.RegisterAction
import com.owo233.tcqt.api.Feature
import com.owo233.tcqt.core.env.HookEnv.toHostClass
import com.owo233.tcqt.core.hook.hookAfter
import com.owo233.tcqt.core.reflect.FieldUtils
import com.owo233.tcqt.core.reflect.findMethod
import com.owo233.tcqt.core.reflect.invokeMethod

@RegisterAction
object ChangePreviewTextSize : Feature(
    key = "change_preview_text_size",
    name = "修改预览字体大小",
    desc = "修改双击启动预览界面的文本字体大小, 缩小方便预览和复制。",
) {

    /** 派生 key = `change_preview_text_size.string.textSize`（与历史一致）。 */
    private val textSize by stringOption(
        settingKey = "string.textSize",
        name = "textSize",
        desc = "默认大小 14\n配置为空或无效值则使用默认大小\n",
    )

    /**
     * 原 `companion object` 里的 `configTextSize`。
     * `class` 改 `object` 后 companion 不再合法，直接作为 object 成员。
     */
    val configTextSize: Float
        get() = textSize.trim().toFloatOrNull()?.takeIf { it > 0f } ?: 14f

    override fun install() {
        val containerViewClass =
            "com.tencent.qqnt.textpreview.PreviewTextContainerView".toHostClass()

        "com.tencent.mobileqq.activity.TextPreviewActivity".toHostClass().findMethod {
            name = "onCreate"
            paramTypes = arrayOf(bundle)
        }.hookAfter { param ->
            val containerView = FieldUtils.create(param.thisObject)
                .typed(containerViewClass)
                .getValue()
                ?: error("修改预览字体大小: 未找到 PreviewTextContainerView 字段")

            val textView = containerView.invokeMethod {
                returnType == TextView::class.java && parameterCount == 0
            } as TextView

            textView.textSize = configTextSize
        }
    }
}
