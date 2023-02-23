package cedar.cordova.qqpay;

import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPreferences;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.tencent.mobileqq.openpay.api.IOpenApi;
import com.tencent.mobileqq.openpay.api.IOpenApiListener;
import com.tencent.mobileqq.openpay.api.OpenApiFactory;
import com.tencent.mobileqq.openpay.constants.OpenConstants;
import com.tencent.mobileqq.openpay.data.base.BaseResponse;
import com.tencent.mobileqq.openpay.data.pay.v2.PayApiV2;

/**
 * This class echoes a string called from JavaScript.
 */
public class QQPay extends CordovaPlugin {

    public static final String TAG = "Cordova.Plugin.QQPay";

    public static final String APPID_PROPERTY_KEY = "qqappid";

    public static final String ERROR_QQ_NOT_INSTALLED = "未安装QQ";
    public static final String ERROR_PARAMETERS_INVALID = "参数格式错误";
    public static final String ERROR_PARAMETERS_CHECK = "参数检查错误";
    public static final String ERROR_PARAMETER_APPID = "参数错误: appId";
    public static final String ERROR_PARAMETER_PARTNERID = "参数错误: partnerid";
    public static final String ERROR_PARAMETER_PREPAYID = "参数错误: prepayid";
    public static final String ERROR_PARAMETER_PACKAGE = "参数错误: package";
    public static final String ERROR_PARAMETER_NONCESTR = "参数错误: noncestr";
    public static final String ERROR_PARAMETER_TIMESTAMP = "参数错误: timestamp";
    public static final String ERROR_PARAMETER_SIGN = "参数错误: sign";
    public static final String ERROR_SEND_REQUEST_FAILED = "发送请求失败";
    public static final String ERROR_RESPONSE_NULL = "回调结果为空";
    public static final String ERROR_RESPONSE_TYPE_INVALID = "回调类型错误";

    protected static IOpenApi openApi;
    protected static CallbackContext currentCallbackContext;

    protected static String appId;
    protected static String callbackScheme;

    int paySerial = 1;

    @Override
    protected void pluginInitialize() {

        super.pluginInitialize();
        initQQPay();
    }

    protected void initQQPay() {
        if (openApi == null) {
            String appId = getAppId();
            openApi = OpenApiFactory.getInstance(this.cordova.getActivity());
        }
    }

    protected String getAppId() {
        if (appId == null) {
            appId = preferences.getString(APPID_PROPERTY_KEY, "");
            callbackScheme = "qwallet" + appId;
        }
        return appId;
    }

    public static CallbackContext getCurrentCallbackContext() {
        return currentCallbackContext;
    }

    public static IOpenApi getIOpenApi() {
        return openApi;
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, String.format("%s is called. Callback ID: %s.", action, callbackContext.getCallbackId()));

        if (action.equals("isMobileQQInstalled")) {
            return isMobileQQInstalled(callbackContext);
        } else if (action.equals("isMobileQQSupportApi")) {
            return isMobileQQSupportApi(callbackContext);
        } else if (action.equals("isMobileQQSupportPay")) {
            return isMobileQQSupportApi(callbackContext);
        } else if (action.equals("mqqPay")) {
            return mqqPay(args, callbackContext);
        }

        return false;
    }

    protected boolean isMobileQQInstalled(CallbackContext callbackContext) {
        boolean isInstalled = openApi.isMobileQQInstalled();

        if (!isInstalled) {
            callbackContext.success(0);
        } else {
            callbackContext.success(1);
        }

        return true;
    }

    protected boolean isMobileQQSupportApi(CallbackContext callbackContext) {
        boolean isSupport = openApi.isMobileQQSupportApi(OpenConstants.ApiName.PAY_V2);

        if (!isSupport) {
            callbackContext.success(0);
        } else {
            callbackContext.success(1);
        }

        return true;
    }

    protected boolean mqqPay(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        // check if # of arguments is correct
        final JSONObject params;
        try {
            params = args.getJSONObject(0);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            callbackContext.error(ERROR_PARAMETERS_INVALID);
            return true;
        }

        PayApiV2 api = new PayApiV2();

        if (params.has("appid")) {
            api.appId = params.getString("appid");
        } else {
            api.appId = getAppId();
        }

        if (params.has("partnerid")) {
            api.partnerId = params.getString("partnerid");
        } else {
            callbackContext.error(ERROR_PARAMETER_PARTNERID);
            return true;
        }

        if (params.has("prepayid")) {
            api.prepayId = params.getString("prepayid");
        } else {
            callbackContext.error(ERROR_PARAMETER_PREPAYID);
            return true;
        }

        if (params.has("package")) {
            api.packageValue = params.getString("package");
        } else {
            callbackContext.error(ERROR_PARAMETER_PACKAGE);
            return true;
        }

        if (params.has("noncestr")) {
            api.nonceStr = params.getString("noncestr");
        } else {
            callbackContext.error(ERROR_PARAMETER_NONCESTR);
            return true;
        }

        if (params.has("timestamp")) {
            api.timeStamp = params.getString("timestamp");
        } else {
            callbackContext.error(ERROR_PARAMETER_TIMESTAMP);
            return true;
        }

        if (params.has("sign")) {
            api.sign = params.getString("sign");
        } else {
            callbackContext.error(ERROR_PARAMETER_SIGN);
            return true;
        }

        if (params.has("signType")) {
            api.signType = params.getString("signType");
        }
        if (params.has("extData")) {
            api.extData = params.getString("extData");
        }
        if (params.has("transaction")) {
            api.transaction = params.getString("transaction");
        }

        if (!api.checkParams()) {
            Log.e(TAG, ReflectionToStringBuilder.toString(api));
            Log.e(TAG, api.toString());
            callbackContext.error(ERROR_PARAMETERS_CHECK);
            return true;
        }

        // 调用QPay
        if (openApi.execApi(api)) {
            Log.i(TAG, "Payment request has been sent successfully.");

            // save current callback context
            currentCallbackContext = callbackContext;

            // send no result and keep callback
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
        } else {
            Log.i(TAG, "Payment request has been sent unsuccessfully.");

            // send error
            callbackContext.error(ERROR_SEND_REQUEST_FAILED);
        }

        return true;
    }
}
