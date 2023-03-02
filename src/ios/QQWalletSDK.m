//
//  QQWalletSDK.m
//  QQWalletSDK
//
//  Created by Eric on 17/9/1.
//  Copyright (c) 2017年 Teccent. All rights reserved.
//

#import "QQWalletSDK.h"

#if ! __has_feature(objc_arc)
#error This file must be compiled with ARC. Use -fobjc-arc flag (or convert project to ARC).
#endif

/**
 *  QQ钱包SDK
 */

///  旧版QQ支付参数key定义
#define QQWalletURLScheme        @"mqqwallet"
#define QQWALLET_PAY_APPID       @"appId"
#define QQWalletBargainorId      @"bargainorId"
#define QQWalletNonce            @"nonce"
#define QQWalletPubAcc           @"pubAcc"
#define QQWalletTokenId          @"tokenId"
#define QQWalletSignature        @"sig"

/// 公用参数key 定义
#define QQWALLET_PAY_APP_VERSION   @"appVersion"
#define QQWALLET_PAY_PARAMS        @"params"
#define QQWALLET_PAY_APPLICATION   @"application"
#define QQWALLET_PAY_APP_SCHEME    @"urlScheme"
#define QQWALLET_PAY_APP_RET_CODE  @"code"
#define QQWALLET_PAY_APP_RET_MSG   @"message"

/// 新版QQ支付参数key定义
#define QQWALLET_PAY_URL_SCHEME_V3   @"mqqwalletv3" // 新版本QQ支付scheme
#define QQWALLET_PAY_PARTNERID       @"partnerId" // 商户id
#define QQWALLET_PAY_PREPAYID        @"prepayId" // 预支付订单号
#define QQWALLET_PAY_NONCE           @"nonceStr" // 随机串，防重发
#define QQWALLET_PAY_TIMESTAMP       @"timeStamp" // 时间戳，防重发
#define QQWALLET_PAY_PACKAGE         @"package"
#define QQWALLET_PAY_SIGN            @"sign"
#define QQWALLET_PAY_SIGN_TYPE       @"signType"
#define QQWALLET_PAY_EXT_DATA        @"extData"

NSString *const URLFormatString = @"://";

NSInteger ExtDataMaxLength = 1024; // 透传字段最大长度

@interface QQWalletSDK()<UIAlertViewDelegate>

@property(nonatomic, strong) NSString *urlScheme;
@property(nonatomic, copy) QQWalletPayResult payResult;

@end

void (^_completion)(QQWalletErrCode errCode, NSString *errStr);
static dispatch_once_t once;
static QQWalletSDK *defaultInstance;

@implementation QQWalletSDK

+ (instancetype)sharedInstance{
    dispatch_once(&once, ^{
        defaultInstance = [QQWalletSDK new];
    });
    return defaultInstance;
}

#pragma mark -新版QQ支付API

- (void)startPay:(QQWalletPayReq *)payReq completion:(QQWalletPayResult)payResult {
    if (!payReq || !payResult) {
        NSAssert(NO, @"payReq or payResult must not be nil.");
    }
    QQWalletPayResp *errResp = [[QQWalletPayResp alloc] init];
    
    if (![self.class isSupportQQWalletV3]) {
        errResp.errCode = QQWalletErrCodeNotSupport;
        errResp.errStr = @"current qq not support qq pay";
        payResult(errResp);
        return;
    }
    
    if (payReq.callbackScheme.length == 0) {
        errResp.errCode = QQWalletErrCodeCommon;
        errResp.errStr = @"completion must not nil";
        payResult(errResp);
        return;
    }
    
    if (payReq.extData.length > ExtDataMaxLength) {
        errResp.errCode = QQWalletErrCodeCommon;
        errResp.errStr = @"extdata length must less than 1024";
        payResult(errResp);
        return;
    }
    
    self.urlScheme = payReq.callbackScheme;
    self.payResult = payResult;
    NSString *paySchemeUrl = [self createPaySchemaWithPayReq:payReq];
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:paySchemeUrl] options:@{} completionHandler:nil];
    
}

- (NSString *)createPaySchemaWithPayReq:(QQWalletPayReq *)payReq {
    
    /// 订单相关信息
    NSMutableDictionary *params = [NSMutableDictionary dictionary];
    params[QQWALLET_PAY_APPID] = payReq.appId;
    params[QQWALLET_PAY_PARTNERID] = payReq.partnerId;
    params[QQWALLET_PAY_PREPAYID] = payReq.prepayId;
    params[QQWALLET_PAY_SIGN] = payReq.sign;
    params[QQWALLET_PAY_SIGN_TYPE] = payReq.signType;
    params[QQWALLET_PAY_PACKAGE] = payReq.package;
    params[QQWALLET_PAY_NONCE] = payReq.nonceStr;
    params[QQWALLET_PAY_TIMESTAMP] = payReq.timeStamp;

    
    NSString *appVersion = [[NSBundle mainBundle].infoDictionary valueForKey:@"CFBundleVersion"];
    NSMutableDictionary *application = [NSMutableDictionary dictionary];
    application[QQWALLET_PAY_APP_VERSION] = appVersion;
    application[QQWALLET_PAY_APP_SCHEME] = payReq.callbackScheme;
    application[QQWALLET_PAY_EXT_DATA] = payReq.extData;

    NSMutableDictionary *payInfo = [NSMutableDictionary dictionary];
    payInfo[QQWALLET_PAY_APPLICATION] = application;
    payInfo[QQWALLET_PAY_PARAMS] = params;
    
    NSMutableString* urlString = [NSMutableString stringWithFormat:@"%@%@",QQWALLET_PAY_URL_SCHEME_V3,URLFormatString];
    NSError *error = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:payInfo options:1 error:&error];
    if (!error) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 70000
        [urlString appendString:[jsonData base64EncodedStringWithOptions:0]];
#else
        [urlString appendString:[jsonData base64Encoding]];
#endif
    }
    return urlString;
}

- (BOOL)hanldeOpenURL:(NSURL *)url{
    if (![url.scheme isEqualToString: self.urlScheme]) {
        return NO;
    }
    NSString *base64String = [url.absoluteString substringFromIndex:(url.scheme.length + URLFormatString.length)];
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 70000
    NSData *originData = [[NSData alloc] initWithBase64EncodedString:base64String options:0];
#else
    NSData *originData = [[NSData alloc] initWithBase64Encoding:base64String];
#endif
    NSError* parserError = nil;
    NSDictionary* infos = [NSJSONSerialization JSONObjectWithData:originData options:0 error:&parserError];
    if (parserError) {
        return NO;
    }
    NSDictionary* params =  infos[QQWALLET_PAY_PARAMS];
    QQWalletErrCode code = (QQWalletErrCode)[params[QQWALLET_PAY_APP_RET_CODE] integerValue];
    if (code != QQWalletErrCodeUserCancel && code!= QQWalletErrCodeSuccess && code!= QQWalletErrCodeNotSupport) {
        code = QQWalletErrCodeCommon;
    }
    NSString *message = params[QQWALLET_PAY_APP_RET_MSG];
    if (self.payResult) {
        QQWalletPayResp *payResp = [[QQWalletPayResp alloc] init];
        payResp.errCode = code;
        payResp.errStr = message;
        payResp.extData = params[QQWALLET_PAY_EXT_DATA];
        payResp.prepayId = params[QQWALLET_PAY_PREPAYID];
        self.payResult(payResp);
        self.payResult = nil;
    } else if (_completion) {
        _completion(code,message);
        _completion = nil;
    }
    
    return YES;
}

+ (BOOL)isSupportQQWalletV3 {
    NSURL* url = [NSURL URLWithString:[QQWALLET_PAY_URL_SCHEME_V3 stringByAppendingString:URLFormatString]];
    return [[UIApplication sharedApplication] canOpenURL:url];
}

+ (BOOL)isSupportQQWallet{
    NSURL* url = [NSURL URLWithString:[QQWalletURLScheme stringByAppendingString:URLFormatString]];
    return [[UIApplication sharedApplication] canOpenURL:url];
}

#pragma mark -旧版QQ支付API

- (void)startPayWithAppId:(NSString *)appId
              bargainorId:(NSString *)bargainorId
                  tokenId:(NSString *)tokenId
                signature:(NSString *)sig
                    nonce:(NSString *)nonce
                   scheme:(NSString *)scheme
               completion:(void (^)(QQWalletErrCode errCode, NSString *errStr))completion{
    self.urlScheme = scheme;
    NSString *appVersion = [[NSBundle mainBundle].infoDictionary valueForKey:@"CFBundleVersion"];
    NSDictionary *params = @{QQWalletTokenId : tokenId?:@"",
                             QQWalletSignature : sig?:@"",
                             QQWalletBargainorId : bargainorId?:@"",
                             QQWalletNonce : nonce?:@"",
                             QQWALLET_PAY_APP_VERSION : appVersion?:@""};
    NSDictionary *application = @{QQWALLET_PAY_APPID : appId?:@"",
                                  QQWALLET_PAY_APP_SCHEME : self.urlScheme?:@""};
    NSDictionary *infos = @{QQWALLET_PAY_APPLICATION : application?:@"",
                            QQWALLET_PAY_PARAMS : params?:@""};
    
    NSMutableString* urlString = [NSMutableString stringWithFormat:@"%@%@",QQWalletURLScheme,URLFormatString];
    NSError *jsonError = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:infos options:1 error:&jsonError];
#if DEBUG
    NSString *jsonStr = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    NSLog(@"QQWallet pay param: %@", jsonStr);
#endif
    if (!jsonError) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 70000
        [urlString appendString:[jsonData base64EncodedStringWithOptions:0]];
#else
        [urlString appendString:[jsonData base64Encoding]];
#endif
    }
    NSURL* url = [NSURL URLWithString:urlString];
    _completion = completion;
    [[UIApplication sharedApplication] openURL:url];
}



@end

@implementation QQWalletPayReq


@end


@implementation QQWalletPayResp



@end
