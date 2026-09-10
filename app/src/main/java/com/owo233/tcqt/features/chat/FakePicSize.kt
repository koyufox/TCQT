// hook 代码来自 https://github.com/cinit/QAuxiliary

package com.owo233.tcqt.features.chat

import com.owo233.tcqt.annotations.RegisterAction
import com.owo233.tcqt.api.Feature
import com.owo233.tcqt.core.hook.hookBefore
import com.owo233.tcqt.core.reflect.findMethod
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact

/**
 * 迁移到 api 门面的原型功能（S2a）。
 *
 * 改造前后对比：
 * - 框架 import 从 8 行降到 **3 行**（`api.*` + `core.reflect.findMethod` +
 *   `core.hook.hookBefore`）。要降到 1 行需要 S3 的 hook DSL —— 本原型做不到，
 *   G1 的"≤1 行"验收仍归 S3。
 * - 配置项声明与读取不再各写一遍 key，`TCQTSetting.getInt("fake_pic_size.type")`
 *   变成直接读委托属性 `type`
 * - 持久化 key 逐字未变：`fake_pic_size` / `fake_pic_size.type` /
 *   `fake_pic_size.custom_width` / `fake_pic_size.custom_height`
 */
@RegisterAction
object FakePicSize : Feature(
    key = "fake_pic_size",
    name = "篡改图片显示大小",
    desc = "将发送的消息图片以指定的比例显示。",
) {

    private val type by intOption(
        settingKey = "type",
        name = "图片比例",
        defaultValue = 1,
        options = listOf("默认", "最小", "略小", "略大", "最大", "自定义"),
    )

    private val customWidth by stringOption(
        settingKey = "custom_width",
        name = "自定义宽度",
        desc = "图片比例选择自定义时生效。留空或0表示不修改/按比例缩放",
    )

    private val customHeight by stringOption(
        settingKey = "custom_height",
        name = "自定义高度",
        desc = "图片比例选择自定义时生效。留空或0表示不修改/按比例缩放",
    )

    override fun install() {
        hookSendMsg()
    }

    private fun hookSendMsg() {
        IKernelMsgService.CppProxy::class.java.findMethod {
            name = "sendMsg"
            paramCount = 5
        }.hookBefore { param ->
            val contact = param.args[1] as Contact
            val elements = param.args[2] as Iterable<*>

            elements
                .filterIsInstance<MsgElement>()
                .forEach { it.adjustPicSize(contact) }
        }
    }

    private fun MsgElement.adjustPicSize(contact: Contact) {
        val pic = picElement ?: return

        if (contact.chatType != 4) {
            pic.picSubType = 0
        }

        val mode = type
        if (mode <= 1) return

        if (mode == 6) {
            val width = customWidth.toIntOrNull() ?: 0
            val height = customHeight.toIntOrNull() ?: 0

            if (width > 0 && height > 0) {
                pic.picWidth = width
                pic.picHeight = height
            } else if (width > 0) {
                val oldW = pic.picWidth.takeIf { it > 0 } ?: return
                val oldH = pic.picHeight.takeIf { it > 0 } ?: return
                val ratio = oldW.toDouble() / oldH.toDouble()
                pic.picWidth = width
                pic.picHeight = (width / ratio).toInt()
            } else if (height > 0) {
                val oldW = pic.picWidth.takeIf { it > 0 } ?: return
                val oldH = pic.picHeight.takeIf { it > 0 } ?: return
                val ratio = oldW.toDouble() / oldH.toDouble()
                pic.picWidth = (height * ratio).toInt()
                pic.picHeight = height
            }
            return
        }

        val targetSize = mode.toTargetSize() ?: return

        if (targetSize == 1) {
            pic.picWidth = targetSize
            pic.picHeight = targetSize
            return
        }

        val oldW = pic.picWidth.takeIf { it > 0 } ?: return
        val oldH = pic.picHeight.takeIf { it > 0 } ?: return

        val ratio = oldW.toDouble() / oldH.toDouble()

        if (oldW > oldH) {
            pic.picWidth = targetSize
            pic.picHeight = (targetSize / ratio).toInt()
        } else {
            pic.picWidth = (targetSize * ratio).toInt()
            pic.picHeight = targetSize
        }
    }

    private fun Int.toTargetSize(): Int? = when (this) {
        2 -> 1
        3 -> 64
        4 -> 512
        5 -> 1024
        else -> null
    }
}
