/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.views.waterfalllist;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;

public class HippyWaterfallItemRenderNode extends RenderNode {

  static final String TAG = "HippyWaterfallItemNode";
  IRecycleItemTypeChange mRecycleItemTypeChangeListener;

  public HippyWaterfallItemRenderNode(int mId, HippyMap mPropsToUpdate, String className,
    HippyRootView mRootView,
    ControllerManager componentManager, boolean isLazyLoad) {
    super(mId, mPropsToUpdate, className, mRootView, componentManager, isLazyLoad);
  }

  @Override
  public String toString() {
    return "[type:" + getType() + "]" + super.toString();
  }

  public int getType() {
    int type = -1;
    HippyMap props = getProps();
    if (props != null && props.containsKey("type")) {
      type = props.getInt("type");
    }
    return type;
  }

  @Override
  public void updateNode(HippyMap map) {
    int oldType = getProps().getInt("type");
    int newType = map.getInt("type");
    if (mRecycleItemTypeChangeListener != null && oldType != newType) {
      mRecycleItemTypeChangeListener.onRecycleItemTypeChanged(oldType, newType, this);
    }
    super.updateNode(map);
  }

  public void setRecycleItemTypeChangeListener(
    IRecycleItemTypeChange recycleItemTypeChangeListener) {
    mRecycleItemTypeChangeListener = recycleItemTypeChangeListener;
  }

  public interface IRecycleItemTypeChange {

    void onRecycleItemTypeChanged(int oldType, int newType,
      HippyWaterfallItemRenderNode listItemNode);
  }

}
