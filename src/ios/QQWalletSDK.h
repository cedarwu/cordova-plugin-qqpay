//
//  QQWalletSDK.h
//  QQWalletSDK
//
//  Created by Eric on 17/9/1.
//  Copyright (c) 2017年 Teccent. All rights reserved.
//

#import <UIKit/UIKit.h>


/**
 *  错误码定义
 */
typedef enum{
    QQWalletErrCodeSuccess    = 0,          /**< 成功    */
    QQWalletErrCodeCommon     = -1,         /**< 普通错误类型    */
    QQWalletErrCodeNotSupport = -2,         /**< 当前手Q版本不支持QQ支付    */
    QQWalletErrCodeUserCancel = -11001,     /**< 用户点击取消并返回    */
} QQWalletErrCode;


@class QQWalletPayReq,QQWalletPayResp;

typedef void(^QQWalletPayResult)(QQWalletPayResp *payResp);

@interface QQWalletSDK : NSObject

/**
 *  获取QQWalletSDK的单例
 */
+ (instancetype)sharedInstance;

#pragma mark -新版QQ支付API

/// 第三方调用QQ钱包进行支付,  新版API
/// @param payReq 第三方向QQ终端发起支付的参数结构
/// @param payResult 第三方向QQ终端发起支付后，QQ终端回调处理结果，包含错误码和错误信息
- (void)startPay:(QQWalletPayReq *)payReq completion:(QQWalletPayResult) payResult;

/**
 *  在手机QQ完成支付后，对本APP进行回调，传递支付执行结果
 *  @param url QQ钱包跳回第三方应用时传递过来的URL
 *  @return 是否能够响应该回调
 */
- (BOOL)hanldeOpenURL:(NSURL *)url;

/**
 *  检查当前系统环境是否支持QQWalletV3
 *  @return BOOL 当前系统环境是否支持QQWallet调用
 */
+ (BOOL)isSupportQQWalletV3;

#pragma mark -旧版QQ支付API

/**
 *  调起QQ钱包进行支付，参数为从第三方APP从服务器获取的参数，透传到手机QQ内，唤起支付功能
 *  @param appId           第三方APP在QQ钱包开放平台申请的appID
 *  @param bargainorId     第三方APP在财付通后台的商户号
 *  @param tokenId         在财付通后台下单的订单号
 *  @param signature       参数按照规则签名后的字符串
 *  @param nonce           签名过程中使用的随机串
 *  @param scheme          在您的工程中的plist文件中创建用于回调的URL SCHEMA。此URL SCHEMA用于手机QQ完成功能后，传递结果信息用。请尽量保证此URL SCHEMA不会与其他冲突。
 *  @param completion      支付完成后调用的block，包含错误码和错误信息
 */
- (void)startPayWithAppId:(NSString *)appId
              bargainorId:(NSString *)bargainorId
                  tokenId:(NSString *)tokenId
                signature:(NSString *)sig
                    nonce:(NSString *)nonce
                   scheme:(NSString *)scheme
               completion:(void (^)(QQWalletErrCode errCode, NSString *errStr))completion;

/**
 *  检查当前系统环境是否支持QQWallet
 *  @return BOOL 当前系统环境是否支持QQWallet调用
 */
+ (BOOL)isSupportQQWallet;

@end


#pragma mark - QQWalletPayReq

/*! @brief 第三方向QQ终端发起支付的消息结构体
 *
 *  第三方向QQ终端发起支付的消息结构体，QQ终端处理后会向第三方返回处理结果
 *
 *  @see QQWalletPayResp
 */
@interface QQWalletPayReq : NSObject

/** 第三方APP在QQ钱包开放平台申请的appId */
@property (nonatomic, copy) NSString *appId;

/** 商家向财付通申请的商家id */
@property (nonatomic, copy) NSString *partnerId;

/** 预支付订单 */
@property (nonatomic, copy) NSString *prepayId;

/** 商家根据财付通文档填写的数据和签名 */
@property (nonatomic, copy) NSString *package;

/** 随机串，防重发 */
@property (nonatomic, copy) NSString *nonceStr;

/** 时间戳，防重发 */
@property (nonatomic, copy) NSString *timeStamp;

/** 商家根据开放平台文档对数据做的签名 */
@property (nonatomic, copy) NSString *sign;

/** 签名方式 */
@property (nonatomic, copy) NSString *signType;

/** 在您的工程中的plist文件中创建用于回调的URL SCHEME。此URL SCHEME用于手机QQ完成功能后，传递结果信息用。请尽量保证此URL SCHEME不会与其他冲突。*/
@property (nonatomic, copy) NSString *callbackScheme;

/**透传字段，在startPay:回调时会原样返回,最大长度1024字节**/
@property (nonatomic, copy) NSString *extData;


@end

#pragma mark - QQWalletPayResp
/*! @brief QQ终端返回给第三方的关于支付结果的结构体
 *
 *  QQ终端返回给第三方的关于支付结果的结构体
 */
@interface QQWalletPayResp : NSObject

/** 错误码 */
@property (nonatomic, assign) QQWalletErrCode errCode;

/** 错误提示字符串 */
@property (nonatomic, copy) NSString *errStr;

/** 预支付订单 */
@property (nonatomic, copy) NSString *prepayId;

/**回传QQWalletPayReq中的extData字段**/
@property (nonatomic, copy) NSString *extData;

@end
