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
#import "HippyInvalidating.h"

@class HippyExtAnimation;

@interface HippyExtAnimationIdCount : NSObject
- (void)addCountForAnimationId:(NSNumber *)animationID;
- (BOOL)subtractionCountForAnimationId:(NSNumber *)animationID;
- (NSUInteger)countForAnimationId:(NSNumber *)animationID;
@end

@interface HippyExtAnimationModule : NSObject <HippyBridgeModule, HippyInvalidating>
- (NSDictionary *)bindAnimaiton:(NSDictionary *)params viewTag:(NSNumber *)viewTag rootTag:(NSNumber *)rootTag;
- (void)connectAnimationToView:(UIView *)view;
- (HippyExtAnimation *)animationFromID:(NSNumber *)animationID;
@end
