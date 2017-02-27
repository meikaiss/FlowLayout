package com.zhy.view.flowlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout extends ViewGroup {

    /**
     * 收缩模式下的 最大显示行数
     */
    private static final int MAX_SHRINK_LINE_COUNT = 3;

    private static final int LEFT = -1;
    private static final int CENTER = 0;
    private static final int RIGHT = 1;

    protected List<List<View>> mAllViews = new ArrayList<List<View>>();
    protected List<Integer> mLineHeight = new ArrayList<Integer>();
    protected List<Integer> mLineWidth = new ArrayList<Integer>();
    private int mGravity;
    private List<View> lineViews = new ArrayList<>();

    protected Mode mode = Mode.SHRINK;
    /**
     * 标记在 Mode.SHRINK 模式下，是否需要显示 "更多"
     */
    private boolean needShowMore = false;
    /**
     * 标记在 Mode.SHRINK 模式下，是否已经完成宽高的测试
     */
    private boolean hasMeasuredShrink = false;

    private int maxShowCount = 0;
    protected TextView tvMoreOrShrink;
    protected TagView tvMoreOrShrinkContainer;


    public enum Mode {
        SHRINK,
        EXPAND
    }


    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TagFlowLayout);
        mGravity = ta.getInt(R.styleable.TagFlowLayout_gravity, LEFT);
        ta.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        int width = 0;
        int height = 0;

        int lineWidth = 0;
        int lineHeight = 0;

        measureChild(getChildAt(0), widthMeasureSpec, heightMeasureSpec);

        int childCount = getChildCount();

        maxShowCount = 0;
        int lineCount = 1;
        for (int i = 1; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                if (i == childCount - 1) {
                    width = Math.max(lineWidth, width);
                    height += lineHeight;
                }
                continue;
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

            if (mode == Mode.EXPAND) {
                if (lineWidth + childWidth > sizeWidth - getPaddingLeft() - getPaddingRight()) {
                    // 此行超出了，要换行了
                    lineCount++;

                    width = Math.max(width, lineWidth);
                    lineWidth = childWidth; //要换新一行了， 所以lineWidth的初始宽度=childWidth
                    height += lineHeight; //要换新一行了，所以把上一行的高度加到总高度上
                    lineHeight = childHeight; // 新一行的高度=当前新一行第一个View的高度
                } else {
                    lineWidth += childWidth;
                    lineHeight = Math.max(lineHeight, childHeight);
                }
                if (i == childCount - 1) {
                    width = Math.max(lineWidth, width);
                    height += lineHeight;
                }
                maxShowCount++;
            } else if (mode == Mode.SHRINK) {
                if (lineCount == MAX_SHRINK_LINE_COUNT && needShowMore) {
                    // 注意：收缩模式下 到达 最大行，需要显示"更多"
                    hasMeasuredShrink = true;
                    if (lineWidth + childWidth + tvMoreOrShrinkContainer.getMeasuredWidth()
                            > sizeWidth - getPaddingLeft() - getPaddingRight()) {

                        // 超了，要换行了
                        lineCount++;
                        maxShowCount++;

                        width = Math.max(width, lineWidth);
                        lineWidth = childWidth;
                        height += lineHeight;
                        lineHeight = tvMoreOrShrinkContainer.getMeasuredHeight();

                        break;
                    } else {
                        maxShowCount++;

                        lineWidth += childWidth;
                        lineHeight = Math.max(lineHeight, childHeight);
                    }
                } else {
                    if (lineWidth + childWidth > sizeWidth - getPaddingLeft() - getPaddingRight()) {
                        // 此行超出了，要换行了
                        lineCount++;
                        maxShowCount++;

                        width = Math.max(width, lineWidth);
                        lineWidth = childWidth;
                        height += lineHeight;
                        lineHeight = childHeight;
                    } else {
                        maxShowCount++;

                        lineWidth += childWidth;
                        lineHeight = Math.max(lineHeight, childHeight);
                    }
                    if (i == childCount - 1) {
                        width = Math.max(lineWidth, width);
                        height += lineHeight;
                    }
                }
            }
        }
        setMeasuredDimension(
                modeWidth == MeasureSpec.EXACTLY ? sizeWidth : width + getPaddingLeft() + getPaddingRight(),
                modeHeight == MeasureSpec.EXACTLY ? sizeHeight : height + getPaddingTop() + getPaddingBottom()
        );

        if (lineCount > MAX_SHRINK_LINE_COUNT && mode == Mode.SHRINK && !hasMeasuredShrink) {
            //第一遍测量，如果发现全部显示的情况下 总行数超过三行，则重新测量，第二遍测量时 最后一行最后 固定显示"更多"
            lineCount = 1;
            needShowMore = true;
            //设置为 true 后，重新测量
            measure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mAllViews.clear();
        mLineHeight.clear();
        mLineWidth.clear();
        lineViews.clear();

        int width = getWidth();

        int lineWidth = 0;
        int lineHeight = 0;

        int cCount = 0;
        if (mode == Mode.EXPAND) {
            cCount = getChildCount();
        } else if (mode == Mode.SHRINK) {
            if (needShowMore){
                cCount = Math.min(maxShowCount, getChildCount());
            }else{
                // 如果测量过程中，发现不会超过SHRINK的最大行数时，则所有child全部显示
                cCount = getChildCount();
            }
            // 注意：这里需要将之前状态下 已经onLayout的child重新设置出 父view的位置
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).layout(-1, -1, -1, -1);
            }
        }

        for (int i = 1; i < cCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) { continue; }
            MarginLayoutParams lp = (MarginLayoutParams) child
                    .getLayoutParams();

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (childWidth + lineWidth + lp.leftMargin + lp.rightMargin
                    > width - getPaddingLeft() - getPaddingRight()) {
                mLineHeight.add(lineHeight);
                mAllViews.add(lineViews);
                mLineWidth.add(lineWidth);

                lineWidth = 0;
                lineHeight = childHeight + lp.topMargin + lp.bottomMargin;
                lineViews = new ArrayList<View>();
            }
            lineWidth += childWidth + lp.leftMargin + lp.rightMargin;
            lineHeight = Math.max(lineHeight, childHeight + lp.topMargin
                    + lp.bottomMargin);
            lineViews.add(child);
        }

        if (needShowMore){
            lineViews.add(tvMoreOrShrinkContainer);
        }

        mLineHeight.add(lineHeight);
        mLineWidth.add(lineWidth);
        mAllViews.add(lineViews);

        int left = getPaddingLeft();
        int top = getPaddingTop();

        int lineNum = mAllViews.size();

        for (int i = 0; i < lineNum; i++) {
            lineViews = mAllViews.get(i);
            lineHeight = mLineHeight.get(i);

            // set gravity
            int currentLineWidth = this.mLineWidth.get(i);
            switch (this.mGravity) {
                case LEFT:
                    left = getPaddingLeft();
                    break;
                case CENTER:
                    left = (width - currentLineWidth) / 2 + getPaddingLeft();
                    break;
                case RIGHT:
                    left = width - currentLineWidth + getPaddingLeft();
                    break;
            }

            for (int j = 0; j < lineViews.size(); j++) {
                View child = lineViews.get(j);
                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                MarginLayoutParams lp = (MarginLayoutParams) child
                        .getLayoutParams();

                int lc = left + lp.leftMargin;
                int tc = top + lp.topMargin;
                int rc = lc + child.getMeasuredWidth();
                int bc = tc + child.getMeasuredHeight();

                child.layout(lc, tc, rc, bc);

                left += child.getMeasuredWidth() + lp.leftMargin
                        + lp.rightMargin;
            }
            top += lineHeight;
        }

    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();

    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(0, 0);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
