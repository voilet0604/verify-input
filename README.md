# VerifyInput

### 非侵入式 String/TextView/EditText 校验工具



1. **使用方法**

> + 添加注解
>
>   ```kotlin
>
>   //默认校验类型 判断是否是null或者""
>   @VerifyInput
>   private var mText: String? = null
>
>   //自定义，错误提示、校验类型， index 校验顺序
>   @VerifyInput(error = "手机号格式不正确", type = VerifyInputType.TYPE_PHONE_CN, index = 2)
>   private var mPhone: String? = null
>
>   //可以校验 TextView和EdiTtext
>   @VerifyInput(error = "数字格式不正确", type = VerifyInputType.TYPE_NUMBER, index = 3)
>   private var mNumber: EditText? = null
>
>   ```
>
> + 校验判断
>
>   ```kotlin
>   class MainActivity : AppCompatActivity() {
>
>       private lateinit var mViewBinding: ActivityMainBinding
>
>       @VerifyInput(error = "名字不能为空")
>       private var mText: String? = null
>
>       @VerifyInput(error = "手机号格式不正确", type = VerifyInputType.TYPE_PHONE_CN, index = 2)
>       private var mPhone: String? = null
>
>       @VerifyInput(error = "邮箱格式不正确", type = VerifyInputType.TYPE_EMAIL, index = 3)
>       private var mEmail: String? = null
>
>       @VerifyInput(error = "中文格式不正确", type = VerifyInputType.TYPE_CN, index = 4)
>       private var mCN: String? = null
>
>       @VerifyInput(error = "英文格式不正确", type = VerifyInputType.TYPE_EN, index = 5)
>       private var mEN: String? = null
>
>       @VerifyInput(error = "身份证号码格式不正确", type = VerifyInputType.TYPE_ID_CN, index = 6)
>       private var mID: String? = null
>
>       @VerifyInput(error = "数字格式不正确", type = VerifyInputType.TYPE_NUMBER, index = 7)
>       private var mNumber: EditText? = null
>
>       override fun onCreate(savedInstanceState: Bundle?) {
>           super.onCreate(savedInstanceState)
>           mViewBinding = ActivityMainBinding.inflate(layoutInflater)
>           setContentView(mViewBinding.root)
>
>           mViewBinding.run {
>               //控件赋值
>               mNumber = inputNumber
>
>               btnOk.setOnClickListener {
>   				//赋值字符串
>                   mText = inputText.text.toString().trim()
>                   mPhone = inputPhone.text.toString().trim()
>                   mEmail = inputEmail.text.toString().trim()
>                   mCN = inputCn.text.toString().trim()
>                   mEN = inputEn.text.toString().trim()
>                   mID = inputId.text.toString().trim()
>                   //非侵入式，不需要继承任何类
>                   if(verify()) {
>                       Toast.makeText(this@MainActivity, "通过", Toast.LENGTH_SHORT).show()
>                   }
>               }
>           }
>
>       }
>   }
>   ```
>
> + 效果
>
> <img src="images\1.jpg" style="zoom:25%;" />
>
> <img src="images\2.jpg" style="zoom:25%;" />

2. **原理**

> + 非侵入式，通过kotlin的扩展函数实现，从而达到不需要继承就能扩展方法。
>
> ```kotlin
> //View 扩展verify方法
> fun View.verify(): Boolean {
>     return verify(this.context.applicationContext, this)
> }
>
> //Dialog 扩展verify方法
> fun AppCompatDialog.verify(): Boolean {
>     return verify(this.context.applicationContext, this)
> }
>
> //Activity 扩展verify方法
> fun Activity.verify(): Boolean {
>     return verify(this.applicationContext, this)
> }
>
> //Fragment 扩展verify方法
> fun Fragment.verify(): Boolean {
>     return verify(requireContext().applicationContext, this)
> }
> ```
>
> + 校验是通过自定义注解+反射配合实现
>
> ```kotlin
> //注解源码
> /**
>  * 只支持 字段为String/TextView/EditText以及子类。
>  */
> @Keep
> @Retention(AnnotationRetention.RUNTIME)
> @Target(AnnotationTarget.FIELD)
> annotation class VerifyInput(
>     val error: String = "",
>     val maxLength: Int = -1,
>     val minLength: Int = -1,
>     val index: Int = 1,
>     val showToast: Boolean = true,
>     @VerifyInputType val type: Int = VerifyInputType.TYPE_EMPTY
> )
>
> @Keep
> @Target(AnnotationTarget.VALUE_PARAMETER)
> annotation class VerifyInputType {
>     companion object {
>
>         /**
>          * 非空判断包括空字符串
>          */
>         const val TYPE_EMPTY = 1
>
>         /**
>          * 邮箱判断
>          */
>         const val TYPE_EMAIL = 2
>
>         /**
>          * 中国手机号校验
>          */
>         const val TYPE_PHONE_CN = 3
>
>         /**
>          * 中国身份证号校验
>          */
>         const val TYPE_ID_CN = 4
>
>         /**
>          * 纯中文校验
>          */
>         const val TYPE_CN = 5
>
>         /**
>          * 纯英文校验
>          */
>         const val TYPE_EN = 6
>
>         /**
>          * 纯数字校验
>          */
>         const val TYPE_NUMBER = 7
>     }
> }
> ```
>
> ```kotlin
> //反射校验核心判断
>
> val ann = field.getAnnotation(VerifyInput::class.java)
>
> when (ann.type) {
>             VerifyInputType.TYPE_EMPTY -> {
>                 if (!verifyNull(context, ann, obj, "")) {
>                     return false
>                 }
>                 if (ann.maxLength > 0) {
>                     val str = getString(obj)
>                     if (str.length > ann.maxLength) {
>                         if(!ann.showToast) return false
>                         Toast.makeText(context, "长度不能超过 ${ann.maxLength}", Toast.LENGTH_SHORT).show()
>                         return false
>                     }
>                 }
>                 if (ann.minLength > 0) {
>                     val str = getString(obj)
>                     if (str.length < ann.minLength) {
>                         if(!ann.showToast) return false
>                         Toast.makeText(context, "长度不能少于 ${ann.minLength}", Toast.LENGTH_SHORT).show()
>                         return false
>                     }
>                 }
>             }
>             VerifyInputType.TYPE_PHONE_CN -> {
>                 if (!verifyNull(context, ann, obj, "手机号")) {
>                     return false
>                 }
>
>                 val phone = getString(obj)
>                 if (!verifyPhoneCN(phone)) {
>                     toast(context, ann, "")
>                     return false
>                 }
>             }
>             VerifyInputType.TYPE_ID_CN -> {
>                 if (!verifyNull(context, ann, obj, "身份证号码")) {
>                     return false
>                 }
>                 val id = getString(obj)
>                 if (!isIDNumber(id)) {
>                     toast(context, ann, "身份证号码格式不正确")
>                     return false
>                 }
>             }
>
>             VerifyInputType.TYPE_EMAIL -> {
>                 if (!verifyNull(context, ann, obj, "邮箱")) {
>                     return false
>                 }
>
>                 val id = getString(obj)
>                 if (!verifyEmail(id)) {
>                     toast(context, ann, "邮箱格式不正确")
>                     return false
>                 }
>             }
>
>             VerifyInputType.TYPE_CN -> {
>                 if (!verifyNull(context, ann, obj,"")) {
>                     return false
>                 }
>
>                 val cn = getString(obj)
>
>                 if (!verifyCN(cn)) {
>                     toast(context, ann, "中文格式不正确")
>                     return false
>                 }
>             }
>
>             VerifyInputType.TYPE_NUMBER -> {
>                 if (!verifyNull(context, ann, obj, "")) {
>                     return false
>                 }
>
>                 val number = getString(obj)
>
>                 if (!verifyNumber(number)) {
>                     toast(context, ann, "数字格式不正确")
>                     return false
>                 }
>             }
>
>             VerifyInputType.TYPE_EN -> {
>                 if (!verifyNull(context, ann, obj, "")) {
>                     return false
>                 }
>
>                 val en = getString(obj)
>
>                 if (!verifyEN(en)) {
>                     toast(context, ann, "英文格式不正确")
>                     return false
>                 }
>             }
>         }
> ```
>
>

