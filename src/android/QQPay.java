package cedar.cordova.qqpay;

import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.tencent.mobileqq.openpay.api.IOpenApi;
import com.tencent.mobileqq.openpay.api.IOpenApiListener;
import com.tencent.mobileqq.openpay.api.OpenApiFactory;
import com.tencent.mobileqq.openpay.constants.OpenConstants;
import com.tencent.mobileqq.openpay.data.base.BaseResponse;
import com.tencent.mobileqq.openpay.data.pay.PayResponse;
import com.tencent.mobileqq.openpay.data.pay.PayApi;

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
    public static final String ERROR_PARAMETER_NONCE = "参数错误: nonce";
    public static final String ERROR_PARAMETER_TIMESTAMP = "参数错误: timeStamp";
    public static final String ERROR_PARAMETER_TOKENID = "参数错误: tokenId";
    public static final String ERROR_PARAMETER_BARGAINORID = "参数错误: bargainorId";
    public static final String ERROR_PARAMETER_SIGTYPE = "参数错误: sigType";
    public static final String ERROR_PARAMETER_SIG = "参数错误: sig";
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
            openApi = OpenApiFactory.getInstance(this.cordova.getActivity(), appId);
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
        boolean isSupport = openApi.isMobileQQSupportApi(OpenConstants.API_NAME_PAY);

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
            callbackContext.error(ERROR_PARAMETERS_INVALID);
            return true;
        }

        PayApi api = new PayApi();

        api.serialNumber = "" + paySerial++;
        api.callbackScheme = callbackScheme;

        if (params.has("appId")) {
            api.appId = params.getString("appId"); // 应用唯一id
        } else {
            callbackContext.error(ERROR_PARAMETER_APPID);
            return true;
        }

        if (params.has("nonce")) {
            api.nonce = params.getString("nonce"); // 随机串
        } else {
            callbackContext.error(ERROR_PARAMETER_NONCE);
            return true;
        }

        if (params.has("timeStamp")) {
            api.timeStamp = params.getInt("timeStamp"); // 时间戳
        } else {
            callbackContext.error(ERROR_PARAMETER_TIMESTAMP);
            return true;
        }

        if (params.has("tokenId")) {
            api.tokenId = params.getString("tokenId"); // QQ钱包的预支付会话标识
        } else {
            callbackContext.error(ERROR_PARAMETER_TOKENID);
            return true;
        }

        if (params.has("pubAcc")) {
            api.pubAcc = params.getString("pubAcc"); // 手Q公众帐号，暂时未对外开放申请。
        }

        if (params.has("pubAccHint")) {
            api.pubAccHint = params.getString("pubAccHint"); // 关注手Q公众帐号提示语
        }

        if (params.has("bargainorId")) {
            api.bargainorId = params.getString("bargainorId"); // QQ钱包支付商户号
        } else {
            callbackContext.error(ERROR_PARAMETER_BARGAINORID);
            return true;
        }

        if (params.has("sigType")) {
            api.sigType = params.getString("sigType"); // 加密方式
        } else {
            callbackContext.error(ERROR_PARAMETER_SIGTYPE);
            return true;
        }

        if (params.has("sig")) {
            api.sig = params.getString("sig"); // 签名串
        } else {
            callbackContext.error(ERROR_PARAMETER_SIG);
            return true;
        }

        if (!api.checkParams()) {
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
