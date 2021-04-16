package com.violet.verifyinput

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.violet.verifyinput.databinding.ActivityMainBinding
import com.violet.verifyinputlib.VerifyInput
import com.violet.verifyinputlib.VerifyInputType
import com.violet.verifyinputlib.verify

class MainActivity : AppCompatActivity() {

    private lateinit var mViewBinding: ActivityMainBinding

    @VerifyInput(error = "名字不能为空")
    private var mText: String? = null

    @VerifyInput(error = "手机号格式不正确", type = VerifyInputType.TYPE_PHONE_CN, index = 2)
    private var mPhone: String? = null

    @VerifyInput(error = "邮箱格式不正确", type = VerifyInputType.TYPE_EMAIL, index = 3)
    private var mEmail: String? = null

    @VerifyInput(error = "中文格式不正确", type = VerifyInputType.TYPE_CN, index = 4)
    private var mCN: String? = null

    @VerifyInput(error = "英文格式不正确", type = VerifyInputType.TYPE_EN, index = 5)
    private var mEN: String? = null

    @VerifyInput(error = "身份证号码格式不正确", type = VerifyInputType.TYPE_ID_CN, index = 6)
    private var mID: String? = null

    @VerifyInput(error = "数字格式不正确", type = VerifyInputType.TYPE_NUMBER, index = 7)
    private var mNumber: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)

        mViewBinding.run {
            mNumber = inputNumber

            btnOk.setOnClickListener {

                mText = inputText.text.toString().trim()
                mPhone = inputPhone.text.toString().trim()
                mEmail = inputEmail.text.toString().trim()
                mCN = inputCn.text.toString().trim()
                mEN = inputEn.text.toString().trim()
                mID = inputId.text.toString().trim()
                if(verify()) {
                    Toast.makeText(this@MainActivity, "通过", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
}