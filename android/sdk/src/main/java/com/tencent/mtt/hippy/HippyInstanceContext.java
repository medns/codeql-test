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
package com.tencent.mtt.hippy;

import android.content.Context;
import android.content.ContextWrapper;
import android.text.TextUtils;

import com.tencent.mtt.hippy.bridge.bundleloader.HippyAssetBundleLoader;
import com.tencent.mtt.hippy.bridge.bundleloader.HippyBundleLoader;
import com.tencent.mtt.hippy.bridge.bundleloader.HippyFileBundleLoader;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.supportui.utils.struct.WeakEventHub;

import java.util.Map;

@SuppressWarnings({"deprecation", "unused"})
public final class HippyInstanceContext extends ContextWrapper {

  private static final String TAG = "HippyInstanceContext";

  private HippyEngineContext mEngineContext;
  private WeakEventHub<InstanceDestroyListener> mDestroyListeners;
  HippyEngine.ModuleLoadParams mModuleParams;
  private HippyBundleLoader mBundleLoader;

  public HippyInstanceContext(Context context, HippyEngine.ModuleLoadParams params) {
    super(context);
    setModuleParams(params);
    mDestroyListeners = new WeakEventHub<>();
  }

  public HippyBundleLoader getBundleLoader() {
    return mBundleLoader;
  }

  public HippyInstanceContext(Context context) {
    super(context);
    mDestroyListeners = new WeakEventHub<>();
  }

  public HippyEngine.ModuleLoadParams getModuleParams() {
    return mModuleParams;
  }

  public void setEngineContext(HippyEngineContext context) {
    this.mEngineContext = context;
  }

  public void setModuleParams(HippyEngine.ModuleLoadParams params) {
    mModuleParams = params;
    if (mModuleParams != null && mModuleParams.bundleLoader != null) {
      mBundleLoader = mModuleParams.bundleLoader;
    } else {
      assert params != null;
      if (!TextUtils.isEmpty(params.jsAssetsPath)) {
        mBundleLoader = new HippyAssetBundleLoader(params.context, params.jsAssetsPath,
            !TextUtils.isEmpty(params.codeCacheTag), params.codeCacheTag);
      } else if (!TextUtils.isEmpty(params.jsFilePath)) {
        mBundleLoader = new HippyFileBundleLoader(params.jsFilePath,
            !TextUtils.isEmpty(params.codeCacheTag), params.codeCacheTag);
      }
    }
  }

  public HippyEngineContext getEngineContext() {
    return mEngineContext;
  }

  @SuppressWarnings("rawtypes")
  public Map getNativeParams() {
    return mModuleParams != null ? mModuleParams.nativeParams : null;
  }

  public void registerInstanceDestroyListener(InstanceDestroyListener listener) {
    if (listener != null && mDestroyListeners != null) {
      mDestroyListeners.registerListener(listener);
    }
  }

  public void unregisterInstanceDestroyListener(InstanceDestroyListener listener) {
    if (listener != null && mDestroyListeners != null) {
      mDestroyListeners.unregisterListener(listener);
    }
  }

  void notifyInstanceDestroy() {
    if (mDestroyListeners != null && mDestroyListeners.size() > 0) {
      Iterable<InstanceDestroyListener> listeners = mDestroyListeners.getNotifyListeners();
      for (InstanceDestroyListener l : listeners) {
        if (l != null) {
          try {
            l.onInstanceDestroy();
          } catch (Exception e) {
            LogUtils.e(TAG, "notifyInstanceDestroy: " + e);
          }
        }
      }
    }
    // harryguo???????????????????????????????????????????????????????????????Android?????????Context?????????????????????????????????????????????
    // ????????????????????????SDK????????????????????????????????????????????????????????????View....???????????????????????????
    // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    // new ????????????Context????????????Activity?????????Context???????????????????????????????????????????????????????????????
    mDestroyListeners = null;
  }

  HippyEngine mHippyEngineManager;

  public void attachEngineManager(HippyEngine hippyEngineManager) {
    mHippyEngineManager = hippyEngineManager;
  }

  public HippyEngine getEngineManager() {
    return mHippyEngineManager;
  }

  public interface InstanceDestroyListener {

    void onInstanceDestroy();
  }
}
