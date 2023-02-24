#import "CDVQQPay.h"
#import <CommonCrypto/CommonHMAC.h>
@implementation CDVQQPay

#pragma mark "API"

- (void)pluginInitialize {
    NSString* appId = [[self.commandDelegate settings] objectForKey:@"qqappid"];
    
    NSString* universalLink = [[self.commandDelegate settings] objectForKey:@"universallink"];
    
    if (appId && ![appId isEqualToString:self.qpayAppId]) {
        self.qpayAppId = appId;
        self.callbackScheme = [NSString stringWithFormat:@"qwallet%@", appId];
        NSLog(@"cordova-plugin-qqpay has been initialized. APP_ID: %@. universalLink: %@", appId, universalLink);
    }  
}

- (void)isMobileQQInstalled:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[QQWalletSDK isSupportQQWallet]];
    [self.commandDelegate sendPluginResult:commandResult callbackId:command.callbackId];
}
- (void)isMobileQQSupportPay:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[QQWalletSDK isSupportQQWallet]];
    [self.commandDelegate sendPluginResult:commandResult callbackId:command.callbackId];
}
- (void)mqqPay:(CDVInvokedUrlCommand *)command 
{

    // check arguments
    NSDictionary *params = [command.arguments objectAtIndex:0];
    if (!params)
    {
        [self failWithCallbackID:command.callbackId withMessage:@"参数格式错误"];
        return;
    }

    // check required parameters
    NSArray *requiredParams = @[@"appid", @"partnerid", @"prepayid", @"package", @"noncestr", @"timestamp", @"sign"];
    for (NSString *key in requiredParams)
    {
        if (![params objectForKey:key])
        {
            [self failWithCallbackID:command.callbackId withMessage:@"参数格式错误"];
            return ;
        }
    }
    // 组装请求
    QQWalletPayReq *req = [[QQWalletPayReq alloc] init];
    req.appId = params[@"appid"];
    req.partnerId = params[@"partnerid"];
    req.prepayId = params[@"prepayid"];
    req.package = params[@"package"];
    req.nonceStr = params[@"noncestr"];
    req.timeStamp = params[@"timestamp"];
    req.sign = params[@"sign"];
    req.signType = params[@"signType"];
    if([params objectForKey:@"callbackScheme"] != nil) {
        req.callbackScheme = params[@"callbackScheme"];
    } else {
        req.callbackScheme = self.callbackScheme;
    }
    // 发起支付
    NSLog(@"Start perform payment requeset!");
    [[QQWalletSDK sharedInstance] startPay:req completion:^(QQWalletPayResp *payResp) {
        if(payResp.errCode != QQWalletErrCodeSuccess) {
            [self failWithCallbackID:command.callbackId withMessage:payResp.errStr];
        } else {
            [self successWithCallbackID:command.callbackId];
        }
    }];
}


#pragma mark "CDVPlugin Overrides"

- (void)handleOpenURL:(NSNotification *)notification
{
    NSURL* url = [notification object];
    NSString *scheme = [NSString stringWithFormat:@"qwallet%@", self.qpayAppId];
    if ([url isKindOfClass:[NSURL class]] && [url.scheme isEqualToString:scheme])
    {
        [[QQWalletSDK sharedInstance] hanldeOpenURL:url];
    }
}

#pragma mark "Private methods"

- (void)successWithCallbackID:(NSString *)callbackID
{
    NSLog(@"successWithCallbackID:%@", callbackID);
    [self successWithCallbackID:callbackID withMessage:@"OK"];
}

- (void)successWithCallbackID:(NSString *)callbackID withMessage:(NSString *)message
{
    NSLog(@"successWithCallbackID:%@, message:%@", callbackID, message);
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

- (void)failWithCallbackID:(NSString *)callbackID withError:(NSError *)error
{
    NSLog(@"failWithCallbackID:%@, error:%@!", callbackID, error);
    [self failWithCallbackID:callbackID withMessage:[error localizedDescription]];
}

- (void)failWithCallbackID:(NSString *)callbackID withMessage:(NSString *)message
{
    NSLog(@"failWithCallbackID:%@, message:%@!", callbackID, message);
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

@end
