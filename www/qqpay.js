var exec = require('cordova/exec');

module.exports = {
    isMobileQQInstalled: function (onSuccess, onError) {
        exec(onSuccess, onError, "QQPay", "isMobileQQInstalled", []);
    },

    isMobileQQSupportPay: function (onSuccess, onError) {
        exec(onSuccess, onError, "QQPay", "isMobileQQSupportPay", []);
    },

    mqqPay: function (params, onSuccess, onError) {
        exec(onSuccess, onError, "QQPay", "mqqPay", [params]);
    }

};
