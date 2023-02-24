#import <Cordova/CDV.h>

#import "QQWalletSDK.h"

@interface CDVQQPay : CDVPlugin

@property(nonatomic, strong) NSString *qpayAppId;
@property(nonatomic, strong) NSString *callbackScheme;

- (void)isMobileQQInstalled:(CDVInvokedUrlCommand *)command;
- (void)isMobileQQSupportPay:(CDVInvokedUrlCommand *)command;
- (void)mqqPay:(CDVInvokedUrlCommand *)command;

@end