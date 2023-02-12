package bose.ankush.payment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.razorpay.ExternalWalletListener
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class PaymentActivity : AppCompatActivity(), PaymentResultWithDataListener, ExternalWalletListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)
    }

    override fun onPaymentSuccess(p0: String?, p1: PaymentData?) {
        TODO("Not yet implemented")
    }

    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
        TODO("Not yet implemented")
    }

    override fun onExternalWalletSelected(p0: String?, p1: PaymentData?) {
        TODO("Not yet implemented")
    }
}
