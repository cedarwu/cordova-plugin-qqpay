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
import com.tencent.mobileqq.openpay.data.pay.v2.PayResponseV2;

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

        if (!(response instanceof PayResponseV2)) {
            Log.d(QQPay.TAG, "response is not PayResponse");
            ctx.error(QQPay.ERROR_RESPONSE_TYPE_INVALID);
            finish();
            return;
        }

        PayResponseV2 payResponse = (PayResponseV2) response;

        if (!payResponse.isSuccess()) {
            ctx.error(String.format("retCode: %d, retMsg: %s", payResponse.retCode, payResponse.retMsg));
            finish();
            return;
        }

        JSONObject resp = new JSONObject();
        try {
            resp.put("retCode", payResponse.retCode);
            resp.put("retMsg", payResponse.retMsg);
            resp.put("transaction", payResponse.transaction);
            resp.put("apiName", payResponse.apiName);
            resp.put("apiMark", payResponse.apiMark);
            resp.put("prepayId", payResponse.prepayId);
            resp.put("extData", payResponse.extData);
        } catch (JSONException e) {
            System.out.println(e);
        }

        ctx.success(resp); // 回调
        finish();
    }
}
