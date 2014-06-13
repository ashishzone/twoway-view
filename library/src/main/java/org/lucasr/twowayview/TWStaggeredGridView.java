/*
 * Copyright (C) 2014 Lucas Rocha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lucasr.twowayview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;

public class TWStaggeredGridView extends TWView {
    private static final String LOGTAG = "TwoWayStaggeredGridView";

    private static final int NUM_COLS = 2;
    private static final int NUM_ROWS = 2;

    private static final int NO_LANE = -1;

    private TWLayoutState mLayoutState;

    private SparseIntArray mItemLanes;
    private int mNumColumns;
    private int mNumRows;
    private int mLaneSize;

    private boolean mIsVertical;

    private final Rect mTempRect = new Rect();

    public TWStaggeredGridView(Context context) {
        this(context, null);
    }

    public TWStaggeredGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TWStaggeredGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TWGridView, defStyle, 0);
        mNumColumns = Math.max(NUM_COLS, a.getInt(R.styleable.TWGridView_numColumns, -1));
        mNumRows = Math.max(NUM_ROWS, a.getInt(R.styleable.TWGridView_numRows, -1));
        a.recycle();

        Orientation orientation = getOrientation();
        mIsVertical = (orientation == Orientation.VERTICAL);
        // TODO: avoid double allocation
        mLayoutState = new TWLayoutState(orientation, getLaneCount());
        mItemLanes = new SparseIntArray(10);
    }

    private int getLaneCount() {
        return (mIsVertical ? mNumColumns : mNumRows);
    }

    private int getLaneForPosition(int position, Flow flow) {
        int lane = mItemLanes.get(position, NO_LANE);
        if (lane != NO_LANE) {
            return lane;
        }

        final int laneCount = getLaneCount();
        int targetEdge = (flow == Flow.FORWARD ? Integer.MAX_VALUE : Integer.MIN_VALUE);

        for (int i = 0; i < laneCount; i++) {
            mLayoutState.get(i, mTempRect);

            final int laneEdge;
            if (mIsVertical) {
                laneEdge = (flow == Flow.FORWARD ? mTempRect.bottom : mTempRect.top);
            } else {
                laneEdge = (flow == Flow.FORWARD ? mTempRect.right : mTempRect.left);
            }

            if ((flow == Flow.FORWARD && laneEdge < targetEdge) ||
                (flow == Flow.BACKWARD && laneEdge > targetEdge)) {
                targetEdge = laneEdge;
                lane = i;
            }
        }

        mItemLanes.put(position, lane);
        return lane;
    }

    private void clearLayout() {
        mLayoutState = new TWLayoutState(getOrientation(), getLaneCount());
        if (mItemLanes != null) {
            mItemLanes.clear();
        }
    }

    @Override
    public void setOrientation(Orientation orientation) {
        final boolean changed = (getOrientation() != orientation);
        super.setOrientation(orientation);

        if (changed) {
            mIsVertical = (orientation == Orientation.VERTICAL);
            clearLayout();
        }
    }

    @Override
    public void setSelection(int position) {
        if (position != 0) {
            throw new IllegalArgumentException("You can only set selection to first position (0)" +
                                               "on a TWStaggeredGridView");
        }

        super.setSelection(position);
    }

    @Override
    public void setSelectionFromOffset(int position, int offset) {
        if (position != 0) {
            throw new IllegalArgumentException("You can only set selection to first position (0)" +
                                               "on a TWStaggeredGridView");
        }

        super.setSelectionFromOffset(position, offset);
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public void setNumColumns(int numColumns) {
        if (mNumColumns == numColumns) {
            return;
        }

        mNumColumns = numColumns;
        if (mIsVertical) {
            clearLayout();
        }
    }

    public int getNumRows() {
        return mNumRows;
    }

    public void setNumRows(int numRows) {
        if (mNumRows == numRows) {
            return;
        }

        mNumRows = numRows;
        if (!mIsVertical) {
            clearLayout();
        }
    }

    @Override
    protected void offsetLayout(int offset) {
        mLayoutState.offset(offset);
    }

    @Override
    protected void resetLayout(int offset) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();

        final int laneCount = getLaneCount();

        if (mIsVertical) {
            mLaneSize = (getWidth() - paddingLeft - paddingRight) / laneCount;
        } else {
            mLaneSize = (getHeight() - paddingTop - paddingBottom) / laneCount;
        }

        for (int i = 0; i < laneCount; i++) {
            final int l = paddingLeft + (mIsVertical ? i * mLaneSize : offset);
            final int t = paddingTop + (mIsVertical ? offset : i * mLaneSize);
            final int r = (mIsVertical ? l + mLaneSize : l);
            final int b = (mIsVertical ? t : t + mLaneSize);

            mLayoutState.set(i, l, t, r, b);
        }
    }

    @Override
    protected int getOuterStartEdge() {
        return mLayoutState.getOuterStartEdge();
    }

    @Override
    protected int getInnerStartEdge() {
        return mLayoutState.getInnerStartEdge();
    }

    @Override
    protected int getInnerEndEdge() {
        return mLayoutState.getInnerEndEdge();
    }

    @Override
    protected int getOuterEndEdge() {
        return mLayoutState.getOuterEndEdge();
    }

    @Override
    protected int getChildWidthMeasureSpec(View child, int position, LayoutParams lp) {
        if (!mIsVertical && lp.width == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else if (mIsVertical) {
            return MeasureSpec.makeMeasureSpec(mLaneSize, MeasureSpec.EXACTLY);
        } else {
            return MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        }
    }

    @Override
    protected int getChildHeightMeasureSpec(View child, int position, LayoutParams lp) {
        if (mIsVertical && lp.height == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else if (!mIsVertical) {
            return MeasureSpec.makeMeasureSpec(mLaneSize, MeasureSpec.EXACTLY);
        } else {
            return MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        }
    }

    @Override
    protected void detachChildFromLayout(View child, int position, Flow flow) {
        final int lane = mItemLanes.get(position, NO_LANE);
        if (lane == NO_LANE) {
            return;
        }

        if (flow == Flow.FORWARD) {
            mLayoutState.offset(lane, mIsVertical ? child.getHeight() : child.getWidth());
        }

        if (mIsVertical) {
            mLayoutState.reduceHeightBy(lane, child.getHeight());
        } else {
            mLayoutState.reduceWidthBy(lane, child.getWidth());
        }
    }

    @Override
    protected void attachChildToLayout(View child, int position, Flow flow, Rect childRect) {
        final int childWidth = child.getMeasuredWidth();
        final int childHeight = child.getMeasuredHeight();

        final int lane = getLaneForPosition(position, flow);
        mLayoutState.get(lane, mTempRect);

        final int l, t, r, b;
        if (mIsVertical) {
            l = mTempRect.left;
            t = (flow == Flow.FORWARD ? mTempRect.bottom : mTempRect.top - childHeight);
            r = mTempRect.right;
            b = t + childHeight;
        } else {
            l = (flow == Flow.FORWARD ? mTempRect.right : mTempRect.left - childWidth);
            t = mTempRect.top;
            r = l + childWidth;
            b = mTempRect.bottom;
        }

        childRect.left = l;
        childRect.top = t;
        childRect.right = r;
        childRect.bottom = b;

        if (flow == Flow.BACKWARD) {
            mLayoutState.offset(lane, mIsVertical ? -childHeight : -childWidth);
        }

        if (mIsVertical) {
            mLayoutState.increaseHeightBy(lane, childHeight);
        } else {
            mLayoutState.increaseWidthBy(lane, childWidth);
        }
    }
}
