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
package com.tencent.mtt.hippy.views.textinput;

import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import androidx.core.content.ContextCompat;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.javascriptmodules.EventDispatcher;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.tencent.mtt.hippy.utils.ContextHolder;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.common.CommonBackgroundDrawable;
import com.tencent.mtt.hippy.views.common.CommonBorder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;

@SuppressWarnings({"deprecation", "unused"})
public class HippyTextInput extends EditText implements HippyViewBase, CommonBorder,
    TextView.OnEditorActionListener, View.OnFocusChangeListener {

  private CommonBackgroundDrawable mReactBackgroundDrawable;
  final HippyEngineContext mHippyContext;
  boolean mHasAddWatcher = false;
  private String mPreviousText;
  TextWatcher mTextWatcher = null;
  boolean mHasSetOnSelectListener = false;

  private final int mDefaultGravityHorizontal;
  private final int mDefaultGravityVertical;
  //??????????????????????????????
  private final Rect mRect = new Rect();  //????????????RootView?????????????????????
  private int mLastRootViewVisibleHeight = -1;      //??????RootView??????????????????
  private boolean mIsKeyBoardShow = false;    //?????????????????????
  private ReactContentSizeWatcher mReactContentSizeWatcher = null;

  public HippyTextInput(Context context) {
    super(context);
    mHippyContext = ((HippyInstanceContext) context).getEngineContext();
    setFocusable(true);
    setFocusableInTouchMode(true);
    setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

    mDefaultGravityHorizontal =
        getGravity() & (Gravity.HORIZONTAL_GRAVITY_MASK | Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK);
    mDefaultGravityVertical = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
    // ??????????????????EditTextView??????hint??????????????????
    setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT));
    setPadding(0, 0, 0, 0);
  }

  @Override
  public void onEditorAction(int actionCode) {
    HippyMap hippyMap = new HippyMap();
    hippyMap.pushInt("actionCode", actionCode);
    hippyMap.pushString("text", getText().toString());
    switch (actionCode) {
      case EditorInfo.IME_ACTION_GO:
        hippyMap.pushString("actionName", "go");
        break;
      case EditorInfo.IME_ACTION_NEXT:
        hippyMap.pushString("actionName", "next");
        break;
      case EditorInfo.IME_ACTION_NONE:
        hippyMap.pushString("actionName", "none");
        break;
      case EditorInfo.IME_ACTION_PREVIOUS:
        hippyMap.pushString("actionName", "previous");
        break;
      case EditorInfo.IME_ACTION_SEARCH:
        hippyMap.pushString("actionName", "search");
        break;
      case EditorInfo.IME_ACTION_SEND:
        hippyMap.pushString("actionName", "send");
        break;
      case EditorInfo.IME_ACTION_DONE:
        hippyMap.pushString("actionName", "done");
        break;
      default:
        hippyMap.pushString("actionName", "unknown");
        break;
    }
    mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
        .receiveUIComponentEvent(getId(),
            "onEditorAction", hippyMap);
    super.onEditorAction(actionCode);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    //??????RootView???????????????,???????????????????????????
    if (getRootView() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      getRootView().getViewTreeObserver().addOnGlobalLayoutListener(globaListener);
    }

  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    //??????RootView???????????????,Listern??????
    if (getRootView() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      getRootView().getViewTreeObserver().removeOnGlobalLayoutListener(globaListener);
    }
  }

  void setGravityHorizontal(int gravityHorizontal) {
    if (gravityHorizontal == 0) {
      gravityHorizontal = mDefaultGravityHorizontal;
    }
    setGravity((getGravity() & ~Gravity.HORIZONTAL_GRAVITY_MASK
        & ~Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) | gravityHorizontal);
  }

  void setGravityVertical(int gravityVertical) {
    if (gravityVertical == 0) {
      gravityVertical = mDefaultGravityVertical;
    }
    setGravity((getGravity() & ~Gravity.VERTICAL_GRAVITY_MASK) | gravityVertical);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (mReactContentSizeWatcher != null) {
      mReactContentSizeWatcher.onLayout();
    }
  }

  @Override
  protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter);
    if (mReactContentSizeWatcher != null) {
      mReactContentSizeWatcher.onLayout();
    }
  }

  public class ReactContentSizeWatcher {

    private final EditText mEditText;
    final HippyEngineContext mHippyContext;
    private int mPreviousContentWidth = 0;
    private int mPreviousContentHeight = 0;

    public ReactContentSizeWatcher(EditText editText, HippyEngineContext hippyContext) {
      mEditText = editText;
      mHippyContext = hippyContext;
    }

    public void onLayout() {
      int contentWidth = mEditText.getWidth();
      int contentHeight = mEditText.getHeight();

      // Use instead size of text content within EditText when available
      if (mEditText.getLayout() != null) {
        contentWidth = mEditText.getCompoundPaddingLeft() + mEditText.getLayout().getWidth() < 0 ? 0
            : mEditText.getLayout().getWidth() +
                mEditText.getCompoundPaddingRight();
        contentHeight =
            mEditText.getCompoundPaddingTop() + mEditText.getLayout().getHeight() < 0 ? 0
                : mEditText.getLayout().getHeight() +
                    mEditText.getCompoundPaddingBottom();
      }

      if (contentWidth != mPreviousContentWidth || contentHeight != mPreviousContentHeight) {
        mPreviousContentHeight = contentHeight;
        mPreviousContentWidth = contentWidth;
        HippyMap contentSize = new HippyMap();
        contentSize.pushDouble("width", mPreviousContentWidth);
        contentSize.pushDouble("height", mPreviousContentWidth);
        HippyMap eventData = new HippyMap();
        eventData.pushMap("contentSize", contentSize);
        mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
            .receiveUIComponentEvent(getId(), "onContentSizeChange", eventData);

      }
    }
  }

  public void setOnContentSizeChange(boolean contentSizeChange) {
    if (contentSizeChange) {
      mReactContentSizeWatcher = new ReactContentSizeWatcher(this, mHippyContext);
    } else {
      mReactContentSizeWatcher = null;
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    //		if (handleTouch)
//		{
//			if (getParent() != null)
//			{
//				getParent().requestDisallowInterceptTouchEvent(true);
//			}
//		}
    return super.onTouchEvent(event);
  }

  public InputMethodManager getInputMethodManager() {
    return (InputMethodManager) this.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
  }

  public void hideInputMethod() {
    InputMethodManager imm = this.getInputMethodManager();
    if (imm != null && imm.isActive(this)) {
      try {
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

  //???????????????????????????????????????,????????????-1
  private int getScreenHeight() {
    try {
      Context context = ContextHolder.getAppContext();
      android.view.WindowManager manager = (android.view.WindowManager) context
          .getSystemService(Context.WINDOW_SERVICE);
      Display display = manager.getDefaultDisplay();

      if (display != null) {
        int width = manager.getDefaultDisplay().getWidth();
        int height = manager.getDefaultDisplay().getHeight();
        return Math.max(width, height);
      }

    } catch (SecurityException e) {
      LogUtils.d("HippyTextInput", "getScreenHeight: " + e.getMessage());
    }
    return -1;
  }

  /**
   * ??????RootView?????????,?????????????????????,??????????????????????????????????????????
   */
  private int getRootViewHeight() {
    int height = -1;
    View rootView = getRootView();
    if (rootView == null) {
      return height;
    }
    // ??????ID??? 106874510 ??????????????????ROM??????????????????????????????????????????
    try {
      rootView.getWindowVisibleDisplayFrame(mRect);
    } catch (Throwable e) {
      LogUtils.d("InputMethodStatusMonitor:", "getWindowVisibleDisplayFrame failed !" + e);
      e.printStackTrace();
    }

    int visibleHeight = mRect.bottom - mRect.top;
    if (visibleHeight < 0) {
      return -1;
    }
    return visibleHeight;
  }

  //??????RootView???????????????listener
  final ViewTreeObserver.OnGlobalLayoutListener globaListener = new ViewTreeObserver.OnGlobalLayoutListener() {
    @Override
    public void onGlobalLayout() {
      int rootViewVisibleHeight = getRootViewHeight(); //RootView?????????
      int screenHeight = getScreenHeight(); //????????????
      if (rootViewVisibleHeight == -1 || screenHeight == -1) //??????????????????????????? //TODO...??????????????????????????????
      {
        mLastRootViewVisibleHeight = rootViewVisibleHeight;
        return;
      }
      if (mLastRootViewVisibleHeight == -1) // ??????
      {
        //??????????????????????????????????????????20%
        if (rootViewVisibleHeight > screenHeight * 0.8f) {

          mIsKeyBoardShow = false; //??????????????????
        } else {
          if (!mIsKeyBoardShow) {
            HippyMap hippyMap = new HippyMap();
            hippyMap.pushInt("keyboardHeight", Math.abs(
                screenHeight - rootViewVisibleHeight)); //TODO ????????????????????????????????????statusbar?????????,??????????????????????????????
            mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
                .receiveUIComponentEvent(getId(),
                    "onKeyboardWillShow", hippyMap);
          }
          mIsKeyBoardShow = true; //???????????? ----s??????????????????
        }
      } else {
        //??????????????????????????????????????????20%
        if (rootViewVisibleHeight > screenHeight * 0.8f) {
          if (mIsKeyBoardShow) {
            HippyMap hippyMap = new HippyMap();
            mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
                .receiveUIComponentEvent(getId(),
                    "onKeyboardWillHide", hippyMap);
          }
          mIsKeyBoardShow = false; //??????????????????
        } else {
          if (!mIsKeyBoardShow) {
            HippyMap hippyMap = new HippyMap();
            hippyMap.pushInt("keyboardHeight",
                Math.abs(mLastRootViewVisibleHeight - rootViewVisibleHeight));
            mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
                .receiveUIComponentEvent(getId(),
                    "onKeyboardWillShow", hippyMap);
          }
          mIsKeyBoardShow = true; //???????????? ----s??????????????????
        }
      }

      mLastRootViewVisibleHeight = rootViewVisibleHeight;
    }
  };

  public void showInputMethodManager() {

    InputMethodManager imm = this.getInputMethodManager();

    try {
      imm.showSoftInput(this, 0, null);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }


  private String mValidator = "";    //???????????????,????????????,????????????????????????crash
  private String sRegrexValidBefore = "";
  private String sRegrexValidRepeat = "";    //??????????????????????????????,?????????.
  private boolean mTextInputed = false;  //?????????????????????

  public void setValidator(String validator) {
    mValidator = validator;
  }

  //changeListener == true ????????????????????? onTextChagne.
  //????????????
  public void setOnChangeListener(boolean changeListener) {
    if (changeListener) //???????????????????????????
    {
      if (mHasAddWatcher) //??????????????????????????????????????????
      {
        return;
      }
      //?????????????????????
      mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
          sRegrexValidBefore = s.toString();//??????????????????,???????????????????????????????????????.???????????????????????????????????????????????????.
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
          HippyTextInput.this.layout(HippyTextInput.this.getLeft(), HippyTextInput.this.getTop(),
              HippyTextInput.this.getRight(),
              HippyTextInput.this.getBottom());

          if (TextUtils.isEmpty((mValidator))) //????????????????????????
          {
            //?????????????????????,????????????????????????
            if (mTextInputed && TextUtils.equals(s.toString(), mPreviousText)) {
              return;
            }
            //?????????????????????sRegrexValidBefore,sRegrexValidBefore?????????????????????????????????????????????.
            mPreviousText = s.toString();
            mTextInputed = true;
            if (!bUserSetValue) //?????????????????????????????????,???????????????????????????.
            {
              HippyMap hippyMap = new HippyMap();
              hippyMap.pushString("text", s.toString());
              mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
                  .receiveUIComponentEvent(getId(),
                      "onChangeText", hippyMap);
              LogUtils.d("robinsli", "afterTextChanged ????????????????????????=" + s.toString());
            }
          } else //??????????????????????????????
          {
            try {
              //?????????????????????????????????????????????
              if (!s.toString().matches(mValidator) && !"".equals(s.toString())) {
                LogUtils.d("robinsli", "afterTextChanged ????????????????????????,??????????????????=" + s.toString());
                //?????????????????????,????????????????????????.????????????????????????,?????????????????????????????????.
                setText(sRegrexValidBefore);
                //????????????setText,?????????????????????beforeTextChanged,onTextChanged,afterTextChanged
                //???????????????????????????????????????????????????.??????????????????????????????????????????.
                sRegrexValidRepeat = sRegrexValidBefore;
                setSelection(getText().toString().length()); // TODO?????????????????????
                mTextInputed = true;
              } else {
                //?????????????????????,????????????????????????
                if (mTextInputed && TextUtils.equals(s.toString(), mPreviousText)) {
                  return;
                }
                mTextInputed = true;
                mPreviousText = s.toString();
                if (!bUserSetValue //???????????????????????????????????????
                    && (TextUtils.isEmpty(sRegrexValidRepeat) //????????????,????????????????????????
                    || !TextUtils.equals(sRegrexValidRepeat, mPreviousText) //??????????????????????????????????????????????????????
                )) {
                  HippyMap hippyMap = new HippyMap();
                  hippyMap.pushString("text", s.toString());
                  mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
                      .receiveUIComponentEvent(getId(),
                          "onChangeText", hippyMap);
                  LogUtils.d("robinsli", "afterTextChanged ????????????????????????=" + s.toString());
                  sRegrexValidRepeat = "";
                }
              }
            } catch (Throwable error) {
              // ?????????????????????????????????,???????????????
            }

          }

        }
      };

      //??????????????????
      mHasAddWatcher = true;
      addTextChangedListener(mTextWatcher);

    } else //????????????????????????????????????
    {
      mHasAddWatcher = false;
      removeTextChangedListener(mTextWatcher);
    }
  }

  @Override
  public void setBackgroundColor(int color) {
    int paddingBottom = getPaddingBottom();
    int paddingTop = getPaddingTop();
    int paddingLeft = getPaddingLeft();
    int paddingRight = getPaddingRight();

    if (color == Color.TRANSPARENT && mReactBackgroundDrawable == null) {
      // don't do anything, no need to allocate ReactBackgroundDrawable for transparent background
      LogUtils.d("HippyTextInput",
          "don't do anything, no need to allocate ReactBackgroundDrawable for transparent background");
    } else {
      getOrCreateReactViewBackground().setBackgroundColor(color);
    }
    // Android??????EditText????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
  }

  public void setBorderColor(int color, int position) {
    getOrCreateReactViewBackground().setBorderColor(color, position);
  }

  public void setBorderRadius(float borderRadius, int position) {
    getOrCreateReactViewBackground().setBorderRadius(borderRadius, position);
  }

  @Override
  public void setBorderStyle(int borderStyle) {
  }

  @Override
  public void setBorderWidth(float width, int position) {
    getOrCreateReactViewBackground().setBorderWidth(width, position);
  }

  private CommonBackgroundDrawable getOrCreateReactViewBackground() {
    if (mReactBackgroundDrawable == null) {
      mReactBackgroundDrawable = new CommonBackgroundDrawable();
      Drawable backgroundDrawable = getBackground();
      super.setBackgroundDrawable(
          null); // required so that drawable callback is cleared before we add the
      // drawable back as a part of LayerDrawable
      if (backgroundDrawable == null) {
        super.setBackgroundDrawable(mReactBackgroundDrawable);
      } else {
        LayerDrawable layerDrawable = new LayerDrawable(
            new Drawable[]{mReactBackgroundDrawable, backgroundDrawable});
        super.setBackgroundDrawable(layerDrawable);
      }
    }
    return mReactBackgroundDrawable;
  }

  @Override
  public NativeGestureDispatcher getGestureDispatcher() {
    return null;
  }

  @Override
  public void setGestureDispatcher(NativeGestureDispatcher dispatcher) {

  }


  public void setOnEndEditingListener(boolean onEndEditingLIstener) {
    if (onEndEditingLIstener) {
      setOnEditorActionListener(this);
    } else {
      setOnEditorActionListener(null);
    }
  }

  @Override
  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
    if ((actionId & EditorInfo.IME_MASK_ACTION) > 0 || actionId == EditorInfo.IME_NULL) {
      HippyMap hippyMap = new HippyMap();
      hippyMap.pushString("text", getText().toString());
      mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
          .receiveUIComponentEvent(getId(), "onEndEditing", hippyMap);
    }
    return false;
  }

  public void setBlurOrOnFocus(boolean blur) {
    if (blur) {
      setOnFocusChangeListener(this);
    } else {
      setOnFocusChangeListener(null);
    }
  }

  @Override
  public void onFocusChange(View v, boolean hasFocus) {

    HippyMap hippyMap = new HippyMap();
    hippyMap.pushString("text", getText().toString());
    if (hasFocus) {
      mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
          .receiveUIComponentEvent(getId(), "onFocus", hippyMap);
    } else {
      mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
          .receiveUIComponentEvent(getId(), "onBlur", hippyMap);
      // harryguo: ???????????????onEndEditing?????????????????????????????????????????????onBlur????????????onEndEditing????????????????????????????????????????????????????????????????????????????????????send???search???next...????????????onEditorAction??????
      // mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class).receiveUIComponentEvent(getId(), "onEndEditing", hippyMap);
    }
  }


  @Override
  protected void onSelectionChanged(int selStart, int selEnd) {
    super.onSelectionChanged(selStart, selEnd);
    if (mHasSetOnSelectListener) {
      HippyMap selection = new HippyMap();
      selection.pushInt("start", selStart);
      selection.pushInt("end", selEnd);
      HippyMap hippyMap = new HippyMap();
      hippyMap.pushMap("selection", selection);
      mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
          .receiveUIComponentEvent(getId(), "onSelectionChange", hippyMap);
    }
  }

  public HippyMap jsGetValue() {
    HippyMap hippyMap = new HippyMap();
    hippyMap.pushString("text", getText().toString());
    return hippyMap;
//		mHippyContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
//				.receiveUIComponentEvent(getId(), "getValue", hippyMap);
  }

  public boolean bUserSetValue = false;

  public void jsSetValue(String value, int pos) {
    bUserSetValue = true;
    setText(value);
    if (value != null) {
      if (pos < 0) {
        pos = value.length();
      }
      if (pos >= value.length()) {
        pos = value.length();
      }
      setSelection(pos);
    }
    bUserSetValue = false;
  }

  public void setOnSelectListener(boolean change) {
    mHasSetOnSelectListener = change;
  }

  @SuppressWarnings("JavaReflectionMemberAccess")
  public void setCursorColor(int color) {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
      // Pre-Android 10, there was no supported API to change the cursor color programmatically.
      // In Android 9.0, they changed the underlying implementation,
      // but also "dark greylisted" the new field, rendering it unusable.
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      Drawable cursorDrawable = getTextCursorDrawable();
      if (cursorDrawable != null) {
        cursorDrawable.setColorFilter(new BlendModeColorFilter(color, BlendMode.SRC_IN));
        setTextCursorDrawable(cursorDrawable);
      }
    } else {
      try {
        Field field = TextView.class.getDeclaredField("mCursorDrawableRes");
        field.setAccessible(true);
        int drawableResId = field.getInt(this);
        field = TextView.class.getDeclaredField("mEditor");
        field.setAccessible(true);
        Object editor = field.get(this);
        Drawable drawable = null;
        final int version = Build.VERSION.SDK_INT;
        if (version >= 21) {
          drawable = this.getContext().getDrawable(drawableResId);
        } else if (version >= 16) {
          drawable = this.getContext().getResources().getDrawable(drawableResId);
        }
        if (drawable == null) {
          return;
        }
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        assert editor != null;
        Class<?> editorClass = editor
            .getClass(); //??????ROM??????????????????Editor??????????????????????????????mDrawableForCursor?????????????????????
        while (editorClass != null) {
          try {
            if (version >= 28) {
              field = editorClass.getDeclaredField("mDrawableForCursor");//mCursorDrawable
              field.setAccessible(true);
              field.set(editor, drawable);
            } else {
              Drawable[] drawables = {drawable, drawable};
              field = editorClass.getDeclaredField("mCursorDrawable");//mCursorDrawable
              field.setAccessible(true);
              field.set(editor, drawables);
            }
            break;
          } catch (Throwable e) {
            LogUtils.d("HippyTextInput", "setCursorColor: " + e.getMessage());
          }
          editorClass = editorClass.getSuperclass(); //????????????????????????
        }
      } catch (Throwable e) {
        LogUtils.d("HippyTextInput", "setCursorColor: " + e.getMessage());
      }
    }
  }
}
