package __PACKAGE_NAME__;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;

import com.tencent.mobileqq.openpay.api.IOpenApi;
import com.tencent.mobileqq.openpay.api.IOpenApiListener;
import com.tencent.mobileqq.openpay.api.OpenApiFactory;
import com.tencent.mobileqq.openpay.data.base.BaseResponse;
import com.tencent.mobileqq.openpay.data.pay.PayResponse;

import cedar.cordova.qqpay.QQPay;

public class CallbackActivity extends Activity implements IOpenApiListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        QQPay.getIOpenApi().handleIntent(getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        QQPay.getIOpenApi().handleIntent(intent, this);
    }

    @Override
    public void onOpenResponse(BaseResponse response) {
        Log.d(QQPay.TAG, "Callback:" + response.toString());
        CallbackContext ctx = QQPay.getCurrentCallbackContext();

        if (response == null) {
            Log.d(QQPay.TAG, "response is null");
            ctx.error(QQPay.ERROR_RESPONSE_NULL);
            finish();
            return;
        }

        if (!(response instanceof PayResponse)) {
            Log.d(QQPay.TAG, "response is not PayResponse");
            ctx.error(QQPay.ERROR_RESPONSE_TYPE_INVALID);
            finish();
            return;
        }

        PayResponse payResponse = (PayResponse) response;

        /*
        message = "apiName:" + payResponse.apiName + " serialnumber:"
                + payResponse.serialNumber + " isSucess:"
                + payResponse.isSuccess() + " retCode:"
                + payResponse.retCode + " retMsg:" + payResponse.retMsg;
        */

        JSONObject resp = new JSONObject();
        try {
            resp.put("apiName", payResponse.apiName);
            resp.put("serialNumber", payResponse.serialNumber);
            resp.put("isSuccess", payResponse.isSuccess());
            resp.put("isPayByWeChat", payResponse.isPayByWeChat());
            resp.put("retCode", payResponse.retCode);
            resp.put("retMsg", payResponse.retMsg);
        } catch (JSONException e) {
            System.out.println(e);
        }

        if (payResponse.isSuccess()) {
            if (!payResponse.isPayByWeChat()) {
                /*
                message += " transactionId:"
                        + payResponse.transactionId + " payTime:"
                        + payResponse.payTime + " callbackUrl:"
                        + payResponse.callbackUrl + " totalFee:"
                        + payResponse.totalFee + " spData:"
                        + payResponse.spData;
                */

                try {
                    resp.put("transactionId", payResponse.transactionId);
                    resp.put("payTime", payResponse.payTime);
                    resp.put("callbackUrl", payResponse.callbackUrl);
                    resp.put("totalFee", payResponse.totalFee);
                    resp.put("spData", payResponse.spData);
                } catch (JSONException e) {
                    System.out.println(e);
                }
            }
        }
        ctx.success(resp); // 回调
        finish();
    }
}
