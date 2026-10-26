package com.owo233.tcqt.features.general

import android.app.Activity
import android.content.Context
import com.owo233.tcqt.annotations.RegisterAction
import com.owo233.tcqt.api.Feature
import com.owo233.tcqt.core.action.ActionProcess
import com.owo233.tcqt.core.env.HookEnv.toHostClass
import com.owo233.tcqt.core.env.isFlagEnabled
import com.owo233.tcqt.core.hook.hookReplace
import com.owo233.tcqt.core.reflect.findMethod
import com.owo233.tcqt.core.reflect.getObject
import com.owo233.tcqt.core.reflect.invoke

@RegisterAction
object UseSystemAppPicker : Feature(
    key = "use_system_app_picker",
    name = "使用系统应用选择器",
    desc = "将文件的“其他应用打开”交给 Android 系统处理。",
    processes = setOf(ActionProcess.MAIN),
) {

    private val options by multiIntOption(
        settingKey = "options",
        name = "可选项",
        defaultValue = 0,
        options = listOf("屏蔽确认对话框"),
    )

    override fun install() {
        val targetClass = FILE_MANAGER_UTIL_IMPL.toHostClass()
        val systemOpenMethod = targetClass.findMethod {
            name = "openFileWithOtherAppWithSystem"
            paramTypes(Context::class.java, String::class.java)
            returnType = void
        }

        val hasChecked by lazy {
            val jefsClass = "com.tencent.mobileqq.haoliyou.JefsClass".toHostClass()
            val instance = jefsClass.invoke("getInstance")!!
            @Suppress("UNCHECKED_CAST")
            instance.getObject("hasChecked") as ThreadLocal<Boolean>
        }

        targetClass.findMethod {
            name = "openWithOtherApp"
            paramTypes(Activity::class.java, String::class.java)
            returnType = void
        }.hookReplace { param ->
            if (!options.isFlagEnabled(0)) {
                return@hookReplace param.thisObject.invoke(systemOpenMethod, *param.args)
            }

            // 仅在本次同步启动期间跳过 QQ 的外部应用确认。
            val checked = hasChecked
            val previous = checked.get()
            checked.set(true)
            try {
                param.thisObject.invoke(systemOpenMethod, *param.args)
            } finally {
                checked.set(previous)
            }
        }
    }

    private const val FILE_MANAGER_UTIL_IMPL =
        "com.tencent.mobileqq.filemanager.api.impl.FileManagerUtilImpl"
}
