package com.owo233.tcqt.core.hook

import java.lang.reflect.Member

fun interface Unhook {

    fun unhook()
}

interface Invoker {

    fun invokeOrigin(thisObject: Any?, vararg args: Any?): Any?
    fun invokeWithMaxPriority(maxPriority: Int, thisObject: Any?, vararg args: Any?): Any?
}

interface HookParam {

    val method: Member

    val thisObject: Any

    var args: Array<Any?>
    var result: Any?
    var throwable: Throwable?
}

interface Chain : HookParam {

    fun proceed(args: Array<Any?> = this.args): Any?
}

interface IHookEngine {

    val apiLevel: Int
    val frameworkName: String
    val frameworkVersion: String
    val frameworkVersionCode: Long
    val bridgeClass: Class<*>?

    /**
     * 当前引擎是否运行在兼容模式（仅 Zygisk 引擎会返回 true）。
     *
     * 带默认值，既有实现（Legacy / Modern）无需改动即可编译。
     */
    val isCompatMode: Boolean get() = false

    fun hookBefore(method: Member, priority: Int = 50, callback: (HookParam) -> Unit): Unhook
    fun hookAfter(method: Member, priority: Int = 50, callback: (HookParam) -> Unit): Unhook
    fun hookReplace(method: Member, priority: Int = 50, callback: (Chain) -> Any?): Unhook

    fun getInvoker(method: Member): Invoker
    fun deoptimize(method: Member): Boolean

    fun log(priority: Int, tag: String?, msg: String, t: Throwable? = null)
}

/**
 * `IHookEngine.frameworkName` 的已知取值。
 *
 * 存在的理由：功能侧曾用 `engine !is ZygiskHookEngine` 判断引擎种类，
 * 这使 `features` 必须 import `loader.zygisk`。改为比较 `frameworkName`
 * 后，功能只需要 core 里的这个常量。
 */
object HookFramework {

    /** `ZygiskHookEngine.frameworkName` 的取值。 */
    const val ZYGISK: String = "Zygisk"
}

object HookEngineManager {

    lateinit var engine: IHookEngine

    val isInitialized: Boolean
        get() = this::engine.isInitialized
}
