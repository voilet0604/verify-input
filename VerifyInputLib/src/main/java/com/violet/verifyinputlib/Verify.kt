package com.violet.verifyinputlib

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.Fragment
import java.lang.reflect.Field
import java.util.regex.Pattern

/**
 * 只支持 字段为String/TextView/EditText以及子类。
 */
@Keep
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class VerifyInput(
    val error: String = "",
    val maxLength: Int = -1,
    val minLength: Int = -1,
    val index: Int = 1,
    val showToast: Boolean = true,
    @VerifyInputType val type: Int = VerifyInputType.TYPE_EMPTY
)

@Keep
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class VerifyInputType {
    companion object {

        /**
         * 非空判断包括空字符串
         */
        const val TYPE_EMPTY = 1

        /**
         * 邮箱判断
         */
        const val TYPE_EMAIL = 2

        /**
         * 中国手机号校验
         */
        const val TYPE_PHONE_CN = 3

        /**
         * 中国身份证号校验
         */
        const val TYPE_ID_CN = 4

        /**
         * 纯中文校验
         */
        const val TYPE_CN = 5

        /**
         * 纯英文校验
         */
        const val TYPE_EN = 6

        /**
         * 纯数字校验
         */
        const val TYPE_NUMBER = 7
    }
}

fun View.verify(): Boolean {
    return verify(this.context.applicationContext, this)
}

fun AppCompatDialog.verify(): Boolean {
    return verify(this.context.applicationContext, this)
}

fun Activity.verify(): Boolean {
    return verify(this.applicationContext, this)
}

fun Fragment.verify(): Boolean {
    return verify(requireContext().applicationContext, this)
}

private fun sortList(fields: Array<Field>?): MutableList<Field>? {
    if(fields.isNullOrEmpty()) return null
    val list = mutableListOf<Field>()
    for(field in fields) {
        if(!field.isAnnotationPresent(VerifyInput::class.java)) continue
        list.add(field)
    }

    list.sortBy {
        it.getAnnotation(VerifyInput::class.java).index
    }
    return list
}

fun verify(context: Context, any: Any): Boolean {
    val fields = any.javaClass.declaredFields
    if (fields.isNullOrEmpty()) return false
    val fieldList = sortList(fields)
    if(fieldList.isNullOrEmpty()) return false
    for (field in fieldList) {
        field.isAccessible = true
        if(!field.isAnnotationPresent(VerifyInput::class.java)) continue
        val ann = field.getAnnotation(VerifyInput::class.java) ?: continue
        val obj = field.get(any)
        if(obj == null) {
            toast(context, ann, "内容不能为空")
            return false
        }
        if (!isStringField(obj) && !isTextViewField(obj)) {
            throw IllegalArgumentException("${field.name} must be String or TextView")
        }
        when (ann.type) {
            VerifyInputType.TYPE_EMPTY -> {
                if (!verifyNull(context, ann, obj, "")) {
                    return false
                }
                if (ann.maxLength > 0) {
                    val str = getString(obj)
                    if (str.length > ann.maxLength) {
                        if(!ann.showToast) return false
                        Toast.makeText(context, "长度不能超过 ${ann.maxLength}", Toast.LENGTH_SHORT).show()
                        return false
                    }
                }
                if (ann.minLength > 0) {
                    val str = getString(obj)
                    if (str.length < ann.minLength) {
                        if(!ann.showToast) return false
                        Toast.makeText(context, "长度不能少于 ${ann.minLength}", Toast.LENGTH_SHORT).show()
                        return false
                    }
                }
            }
            VerifyInputType.TYPE_PHONE_CN -> {
                if (!verifyNull(context, ann, obj, "手机号")) {
                    return false
                }

                val phone = getString(obj)
                if (!verifyPhoneCN(phone)) {
                    toast(context, ann, "")
                    return false
                }
            }
            VerifyInputType.TYPE_ID_CN -> {
                if (!verifyNull(context, ann, obj, "身份证号码")) {
                    return false
                }
                val id = getString(obj)
                if (!isIDNumber(id)) {
                    toast(context, ann, "身份证号码格式不正确")
                    return false
                }
            }

            VerifyInputType.TYPE_EMAIL -> {
                if (!verifyNull(context, ann, obj, "邮箱")) {
                    return false
                }

                val id = getString(obj)
                if (!verifyEmail(id)) {
                    toast(context, ann, "邮箱格式不正确")
                    return false
                }
            }

            VerifyInputType.TYPE_CN -> {
                if (!verifyNull(context, ann, obj,"")) {
                    return false
                }

                val cn = getString(obj)

                if (!verifyCN(cn)) {
                    toast(context, ann, "中文格式不正确")
                    return false
                }
            }

            VerifyInputType.TYPE_NUMBER -> {
                if (!verifyNull(context, ann, obj, "")) {
                    return false
                }

                val number = getString(obj)

                if (!verifyNumber(number)) {
                    toast(context, ann, "数字格式不正确")
                    return false
                }
            }

            VerifyInputType.TYPE_EN -> {
                if (!verifyNull(context, ann, obj, "")) {
                    return false
                }

                val en = getString(obj)

                if (!verifyEN(en)) {
                    toast(context, ann, "英文格式不正确")
                    return false
                }
            }
        }
    }
    return true
}

private fun getToastMsg(ann: VerifyInput, toastMsg: String): String {
    return if(ann.error.isEmpty()) toastMsg else ann.error
}

private fun toast(context: Context, ann: VerifyInput, toastMsg: String){
    if(!ann.showToast) return
    Toast.makeText(context, getToastMsg(ann, toastMsg), Toast.LENGTH_SHORT).show()
}

private fun getString(obj: Any): String {
    return when(obj) {
        is String -> obj
        is TextView -> obj.text.toString().trim()
        else -> ""
    }
}

fun verifyRegex(content: String, regex: String): Boolean {
    val compile = Pattern.compile(regex)
    val matcher = compile.matcher(content)
    return matcher.matches()
}

fun verifyEN(content: String): Boolean {
    return verifyRegex(content, "[a-zA-Z]+")
}

fun verifyNumber(content: String): Boolean {
    return verifyRegex(content, "[0-9]{1,}")
}

fun verifyCN(content: String): Boolean {
    return verifyRegex(content, "[\u4e00-\u9fa5]+")
}

fun verifyEmail(email: String): Boolean {
    return verifyRegex(email, "^\\s*\\w+(?:\\.?[\\w-]+)*@[a-zA-Z0-9]+(?:[-.][a-zA-Z0-9]+)*\\.[a-zA-Z]+\\s*$")
}

fun verifyPhoneCN(phone: String): Boolean {
    return verifyRegex(phone, "^(12|13|14|15|16|17|18|19)\\d{9}$")
}

private fun verifyNull(context: Context, ann: VerifyInput, obj: Any, type: String): Boolean {
    if(isStringField(obj)) {
        val str = obj as String
        if(str.isEmpty()) {
            toast(context, ann, "${type}内容不能为空")
            return false
        }
    } else if (isTextViewField(obj)){
        val text = obj as TextView
        if(text.text.toString().trim().isEmpty()) {
            toast(context, ann, "${type}内容不能为空")
            return false
        }
    }
    return true
}

/**
 * 省份证id正则校验
 */
private fun isIDNumber(IDNumber: String): Boolean {
    // 定义判别用户身份证号的正则表达式（15位或者18位，最后一位可以为字母）
    val regularExpression = "(^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|" + "(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)"
    //假设18位身份证号码:41000119910101123X  410001 19910101 123X
    //^开头
    //[1-9] 第一位1-9中的一个      4
    //\\d{5} 五位数字           10001（前六位省市县地区）
    //(18|19|20)                19（现阶段可能取值范围18xx-20xx年）
    //\\d{2}                    91（年份）
    //((0[1-9])|(10|11|12))     01（月份）
    //(([0-2][1-9])|10|20|30|31)01（日期）
    //\\d{3} 三位数字            123（第十七位奇数代表男，偶数代表女）
    //[0-9Xx] 0123456789Xx其中的一个 X（第十八位为校验值）
    //$结尾

    //假设15位身份证号码:410001910101123  410001 910101 123
    //^开头
    //[1-9] 第一位1-9中的一个      4
    //\\d{5} 五位数字           10001（前六位省市县地区）
    //\\d{2}                    91（年份）
    //((0[1-9])|(10|11|12))     01（月份）
    //(([0-2][1-9])|10|20|30|31)01（日期）
    //\\d{3} 三位数字            123（第十五位奇数代表男，偶数代表女），15位身份证不含X
    //$结尾


    val matches = IDNumber.matches(regularExpression.toRegex())

    //判断第18位校验值
    if (matches) {
        if (IDNumber.length == 18) {
            try {
                val charArray = IDNumber.toCharArray()
                //前十七位加权因子
                val idCardWi = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
                //这是除以11后，可能产生的11位余数对应的验证码
                val idCardY = arrayOf("1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2")
                var sum = 0
                for (i in idCardWi.indices) {
                    val current = Integer.parseInt(charArray[i].toString())
                    val count = current * idCardWi[i]
                    sum += count
                }
                val idCardLast = charArray[17]
                val idCardMod = sum % 11
                return idCardY[idCardMod].equals(idCardLast.toString(), ignoreCase = true)

            } catch (e: Exception) {
                return false
            }
        }
    }
    return matches
}

private fun isStringField(any: Any): Boolean {
    return any is String
}

private fun isTextViewField(any: Any): Boolean {
    return any is TextView
}