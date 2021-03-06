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

package com.tencent.mtt.hippy.views.hippylist;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.HippyItemTypeHelper;
import androidx.recyclerview.widget.ItemLayoutParams;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.LayoutParams;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.uimanager.DiffUtils;
import com.tencent.mtt.hippy.uimanager.DiffUtils.PatchType;
import com.tencent.mtt.hippy.uimanager.ListItemRenderNode;
import com.tencent.mtt.hippy.uimanager.PullFooterRenderNode;
import com.tencent.mtt.hippy.uimanager.PullHeaderRenderNode;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.list.IRecycleItemTypeChange;
import com.tencent.mtt.hippy.views.refresh.HippyPullFooterView;
import com.tencent.mtt.hippy.views.refresh.HippyPullHeaderView;
import com.tencent.mtt.hippy.views.hippylist.recyclerview.helper.skikcy.IStickyItemsProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2020/12/22.
 * Description RecyclerView??????View??????????????????RenderNode????????????????????????????????????RecyclerViewItem???
 * ???????????????renderNode?????????header???sticky?????????????????????????????????????????????
 */
public class HippyRecyclerListAdapter<HRCV extends HippyRecyclerView> extends Adapter<HippyRecyclerViewHolder>
        implements IRecycleItemTypeChange, IStickyItemsProvider, ItemLayoutParams, OnTouchListener {

    private static final int STICK_ITEM_VIEW_TYPE_BASE = -100000;
    protected final HippyEngineContext hpContext;
    protected final HRCV hippyRecyclerView;
    protected final HippyItemTypeHelper hippyItemTypeHelper;
    protected int positionToCreateHolder;
    protected PullFooterRefreshHelper footerRefreshHelper;
    protected PullHeaderRefreshHelper headerRefreshHelper;
    protected PreloadHelper preloadHelper;

    public HippyRecyclerListAdapter(HRCV hippyRecyclerView, HippyEngineContext hpContext) {
        this.hpContext = hpContext;
        this.hippyRecyclerView = hippyRecyclerView;
        hippyItemTypeHelper = new HippyItemTypeHelper(hippyRecyclerView);
        preloadHelper = new PreloadHelper(hippyRecyclerView);
    }

    /**
     * ???????????????RenderNode??????????????????
     * ?????????View???????????????ViewGroup????????????????????????ViewGroup???RenderNode???View????????????????????????
     * ???RenderNode???View???????????????Header???????????????????????????????????????ViewHolder???renderView??????????????????
     * ??????????????????????????????renderViewContainer????????????viewHolder??????????????????????????????header?????????View???????????????
     * ViewHolder?????????
     */
    @NonNull
    @Override
    public HippyRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ListItemRenderNode renderNode = getChildNodeByAdapterPosition(positionToCreateHolder);
        boolean isViewExist = renderNode.isViewExist();
        boolean needsDelete = renderNode.needDeleteExistRenderView();
        View renderView = createRenderView(renderNode);
        if (isPullHeader(positionToCreateHolder)) {
            ((HippyPullHeaderView) renderView).setRecyclerView(hippyRecyclerView);
            initHeaderRefreshHelper(renderView, renderNode);
            return new HippyRecyclerViewHolder(headerRefreshHelper.getView(), renderNode);
        } else if (renderView instanceof HippyPullFooterView) {
            ((HippyPullFooterView) renderView).setRecyclerView(hippyRecyclerView);
            initFooterRefreshHelper(renderView, renderNode);
            return new HippyRecyclerViewHolder(footerRefreshHelper.getView(), renderNode);
        } else if (isStickyPosition(positionToCreateHolder)) {
            return new HippyRecyclerViewHolder(getStickyContainer(parent, renderView), renderNode);
        } else {
            if (renderView == null) {
                throw new IllegalArgumentException("createRenderView error!"
                        + ",isDelete:" + renderNode.isDelete()
                        + ",isViewExist:" + isViewExist
                        + ",needsDelete:" + needsDelete
                        + ",className:" + renderNode.getClassName()
                        + ",isLazy :" + renderNode.isIsLazyLoad()
                        + ",itemCount :" + getItemCount()
                        + ",getNodeCount:" + getRenderNodeCount()
                        + ",notifyCount:" + hippyRecyclerView.renderNodeCount
                        + "curPos:" + positionToCreateHolder
                        + ",rootView:" + renderNode.hasRootView()
                        + ",parentNode:" + (renderNode.getParent() != null)
                        + ",offset:" + hippyRecyclerView.computeVerticalScrollOffset()
                        + ",range:" + hippyRecyclerView.computeVerticalScrollRange()
                        + ",extent:" + hippyRecyclerView.computeVerticalScrollExtent()
                        + ",viewType:" + viewType
                        + ",id:" + renderNode.getId()
                        + ",attachedIds:" + getAttachedIds()
                        + ",nodeOffset:" + hippyRecyclerView.getNodePositionHelper().getNodeOffset()
                        + ",view:" + hippyRecyclerView
                );
            }
            return new HippyRecyclerViewHolder(renderView, renderNode);
        }
    }

    String getAttachedIds() {
        StringBuilder attachedIds = new StringBuilder();
        int childCount = hippyRecyclerView.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View attachedView = hippyRecyclerView.getChildAt(i);
            attachedIds.append("|p_" + hippyRecyclerView.getChildAdapterPosition(attachedView));
            attachedIds.append("_i_" + attachedView.getId());
        }
        return attachedIds.toString();
    }


    /**
     * deleteExistRenderView ????????????????????????????????????????????????????????????
     * ?????????{@link HippyRecyclerView#onViewAbound(HippyRecyclerViewHolder)}
     * ??????????????????????????????deleteExistRenderView,deleteExistRenderView???????????????????????????
     * ??????????????????View???shouldSticky???true?????????????????????????????????????????????????????????deleteExistRenderView
     *
     * @param renderNode
     * @return
     */
    protected View createRenderView(ListItemRenderNode renderNode) {
        if (renderNode.needDeleteExistRenderView() && !renderNode.shouldSticky()) {
            deleteExistRenderView(renderNode);
        }
        renderNode.setLazy(false);
        View view = renderNode.createViewRecursive();
        renderNode.updateViewRecursive();
        return view;
    }

    public void deleteExistRenderView(ListItemRenderNode renderNode) {
        renderNode.setLazy(true);
        RenderNode parentNode = getParentNode();
        if (parentNode != null) {
            hpContext.getRenderManager().getControllerManager()
                    .deleteChild(parentNode.getId(), renderNode.getId());
        } else {
            hpContext.getRenderManager().getControllerManager().removeViewFromRegistry(renderNode.getId());
        }
        renderNode.setRecycleItemTypeChangeListener(null);
    }

    private FrameLayout getStickyContainer(ViewGroup parent, View renderView) {
        FrameLayout container = new FrameLayout(parent.getContext());
        if (renderView != null) {
            container.addView(renderView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        }
        return container;
    }

    @Override
    public String toString() {
        return "HippyRecyclerAdapter: itemCount:" + getItemCount();
    }

    /**
     * ???????????? ???????????????viewHolder???isCreated ???true?????????updateViewRecursive????????????????????????????????????????????????
     * ????????????????????????viewHolder?????????view??????diff??????????????????????????????view?????????
     *
     * @param hippyRecyclerViewHolder position?????????viewHolder
     * @param position ???????????????????????????
     */
    @Override
    public void onBindViewHolder(HippyRecyclerViewHolder hippyRecyclerViewHolder, int position) {
        setLayoutParams(hippyRecyclerViewHolder.itemView, position);
        RenderNode oldNode = hippyRecyclerViewHolder.bindNode;
        ListItemRenderNode newNode = getChildNodeByAdapterPosition(position);
        oldNode.setLazy(true);
        newNode.setLazy(false);
        if (oldNode != newNode) {
            //step 1: diff
            ArrayList<PatchType> patchTypes = DiffUtils.diff(oldNode, newNode);
            //step:2 delete unUseful views
            DiffUtils.deleteViews(hpContext.getRenderManager().getControllerManager(), patchTypes);
            //step:3 replace id
            DiffUtils.replaceIds(hpContext.getRenderManager().getControllerManager(), patchTypes);
            //step:4 create view is do not  reUse
            DiffUtils.createView(patchTypes);
            //step:5 patch the dif result
            DiffUtils.doPatch(hpContext.getRenderManager().getControllerManager(), patchTypes);
        }
        newNode.setRecycleItemTypeChangeListener(this);
        hippyRecyclerViewHolder.bindNode = newNode;
    }

    public void onFooterRefreshCompleted() {
        if (footerRefreshHelper != null) {
            footerRefreshHelper.onRefreshCompleted();
        }
    }

    public void onFooterDestroy() {
        if (footerRefreshHelper != null) {
            footerRefreshHelper.onDestroy();
            footerRefreshHelper = null;
        }
    }

    public void onHeaderRefreshCompleted() {
        if (headerRefreshHelper != null) {
            headerRefreshHelper.onRefreshCompleted();
        }
    }

    public void onHeaderDestroy() {
        if (headerRefreshHelper != null) {
            headerRefreshHelper.onDestroy();
            headerRefreshHelper = null;
        }
    }

    public void enableHeaderRefresh() {
        if (headerRefreshHelper != null) {
            headerRefreshHelper.enableRefresh();
        }
    }

    private void initHeaderRefreshHelper(View itemView, RenderNode node) {
        if (headerRefreshHelper == null) {
            headerRefreshHelper = new PullHeaderRefreshHelper(hippyRecyclerView, node);
        }
        headerRefreshHelper.setItemView(itemView);
    }

    /**
     * ??????????????????item?????????footer??????????????????????????????itemView???????????????footer????????????????????????????????????
     */
    private void initFooterRefreshHelper(View itemView, RenderNode node) {
        if (footerRefreshHelper == null) {
            footerRefreshHelper = new PullFooterRefreshHelper(hippyRecyclerView, node);
        }
        footerRefreshHelper.setItemView(itemView);
    }

    /**
     * ??????View???LayoutParams????????????????????????render????????????
     * ??????LinearLayout?????????????????????????????????????????????????????????????????????????????????
     */
    protected void setLayoutParams(View itemView, int position) {
        LayoutParams childLp = getLayoutParams(itemView);
        RenderNode childNode = getChildNodeByAdapterPosition(position);
        if (childNode instanceof PullFooterRenderNode || childNode instanceof PullHeaderRenderNode) {
            return;
        }
        if (HippyListUtils.isLinearLayout(hippyRecyclerView)) {
            boolean isVertical = HippyListUtils.isVerticalLayout(hippyRecyclerView);
            childLp.height = isVertical ? childNode.getHeight() : MATCH_PARENT;
            childLp.width = isVertical ? MATCH_PARENT : childNode.getWidth();
        } else {
            childLp.height = childNode.getHeight();
            childLp.width = childNode.getWidth();
        }
        itemView.setLayoutParams(childLp);
    }


    protected LayoutParams getLayoutParams(View itemView) {
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        LayoutParams childLp = null;
        if (params instanceof LayoutParams) {
            childLp = (LayoutParams) params;
        }
        if (childLp == null) {
            childLp = new LayoutParams(MATCH_PARENT, 0);
        }
        return childLp;
    }

    @Override
    public int getItemViewType(int position) {
        //?????????onCreateViewHolder????????????????????????getItemViewType??????????????????position?????????
        //??????onCreateViewHolder??????????????????View?????????onCreateViewHolder???????????????RenderNode???View???
        setPositionToCreate(position);
        ListItemRenderNode node = getChildNodeByAdapterPosition(position);
        if (node == null) {
            return 0;
        }
        if (node.shouldSticky()) {
            return STICK_ITEM_VIEW_TYPE_BASE - position;
        }
        return node.getItemViewType();
    }

    protected void setPositionToCreate(int position) {
        positionToCreateHolder = position;
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param position adapter?????????item??????
     */
    public ListItemRenderNode getChildNodeByAdapterPosition(int position) {
        return getChildNode(hippyRecyclerView.getNodePositionHelper().getRenderNodePosition(position));
    }

    /**
     * ???????????????renderNode????????????
     *
     * @param position ???????????????????????????
     */
    public ListItemRenderNode getChildNode(int position) {
        RenderNode parentNode = getParentNode();
        if (parentNode != null && position < parentNode.getChildCount() && position >= 0) {
            return (ListItemRenderNode) parentNode.getChildAt(position);
        }
        return null;
    }

    /**
     * listItemView?????????
     */
    @Override
    public int getItemCount() {
        return getRenderNodeCount();
    }

    /**
     * ???????????????list?????????Item??????
     *
     * @return
     */
    public int getRenderNodeCount() {
        RenderNode listNode = getParentNode();
        if (listNode != null) {
            return listNode.getChildCount();
        }
        return 0;
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    public int getRenderNodeTotalHeight() {
        int renderCount = getRenderNodeCount();
        int renderNodeTotalHeight = 0;
        for (int i = 0; i < renderCount; i++) {
            renderNodeTotalHeight += getRenderNodeHeight(i);
        }
        return renderNodeTotalHeight;
    }

    public int getItemHeight(int position) {
        Integer itemHeight = getRenderNodeHeight(position);
        if (itemHeight != null) {
            return itemHeight;
        }
        return 0;
    }

    public int getRenderNodeHeight(int position) {
        ListItemRenderNode childNode = getChildNode(position);
        if (childNode != null) {
            if (childNode.isPullHeader()) {
                if (headerRefreshHelper != null) {
                    return headerRefreshHelper.getVisibleHeight();
                }

                return 0;
            }
            if (childNode.isPullFooter()) {
                if (footerRefreshHelper != null) {
                    return footerRefreshHelper.getVisibleHeight();
                }

                return 0;
            }
            return childNode.getHeight();
        }
        return 0;
    }

    public int getItemWidth(int position) {
        Integer renderNodeWidth = getRenderNodeWidth(position);
        if (renderNodeWidth != null) {
            return renderNodeWidth;
        }
        return 0;
    }

    public int getRenderNodeWidth(int position) {
        ListItemRenderNode childNode = getChildNode(position);
        if (childNode != null) {
            if (childNode.isPullHeader()) {
                if (headerRefreshHelper != null) {
                    return headerRefreshHelper.getVisibleWidth();
                }

                return 0;
            }
            if (childNode.isPullFooter()) {
                if (footerRefreshHelper != null) {
                    return footerRefreshHelper.getVisibleWidth();
                }

                return 0;
            }
            return childNode.getWidth();
        }
        return 0;
    }

    protected RenderNode getParentNode() {
        return hpContext.getRenderManager().getRenderNode(getHippyListViewId());
    }

    private int getHippyListViewId() {
        return ((View) hippyRecyclerView.getParent()).getId();
    }

    @Override
    public void onRecycleItemTypeChanged(int oldType, int newType, ListItemRenderNode listItemNode) {
        hippyItemTypeHelper.updateItemType(oldType, newType, listItemNode);
    }

    @Override
    public long getItemId(int position) {
        return getChildNodeByAdapterPosition(position).getId();
    }

    /**
     * ???position?????????renderNode????????????????????????
     */
    @Override
    public boolean isStickyPosition(int position) {
        if (position >= 0 && position < getItemCount()) {
            return getChildNodeByAdapterPosition(position).shouldSticky();
        }
        return false;
    }

    /**
     * ???position?????????renderNode?????????Header?????????????????????????????????
     */
    private boolean isPullHeader(int position) {
        if (position == 0) {
            return getChildNodeByAdapterPosition(0).isPullHeader();
        }
        return false;
    }

    public PreloadHelper getPreloadHelper() {
        return preloadHelper;
    }

    public void setPreloadItemNumber(int preloadItemNumber) {
        preloadHelper.setPreloadItemNumber(preloadItemNumber);
    }

    @Override
    public void getItemLayoutParams(int position, LayoutParams lp) {
        if (lp == null) {
            return;
        }
        lp.height = getItemHeight(position);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (headerRefreshHelper != null) {
            headerRefreshHelper.onTouch(v, event);
        }
        if (footerRefreshHelper != null) {
            footerRefreshHelper.onTouch(v, event);
        }
        return false;
    }
}
