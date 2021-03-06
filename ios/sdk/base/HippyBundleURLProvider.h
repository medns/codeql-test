/*!
 * iOS SDK
 *
 * Tencent is pleased to support the open source community by making
 * Hippy available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <Foundation/Foundation.h>

@interface HippyBundleURLProvider : NSObject

@property (nonatomic, copy, readonly) NSString *localhostIP;
@property (nonatomic, copy, readonly) NSString *localhostPort;
@property (nonatomic, copy, readonly) NSString *debugPathUrl;
@property (nonatomic, copy, readonly) NSString *versionId;
@property (nonatomic, copy, readonly) NSString *scheme;
@property (nonatomic, copy, readonly) NSString *wsURL;

/**
 * @return instancetype
 */
+ (instancetype)sharedInstance;

+ (NSString *)parseVersionId:(NSString *)path;

/**
 * set local debug ip & port
 * default is localhost:38989
 @param localhostIP local host IP
 @param localhostPort local host port
*/
- (void)setLocalhostIP:(NSString *)localhostIP localhostPort:(NSString *)localhostPort;

- (void)setScheme:(NSString *)scheme;

- (void)setDebugPathUrl:(NSString *)debugPathUrl;

- (NSString *)localhost;

- (NSString *)debugPathUrl;

- (NSString *)versionId;

- (NSString *)bundleURLString;

@end
