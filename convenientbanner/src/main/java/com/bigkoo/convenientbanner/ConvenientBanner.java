package com.bigkoo.convenientbanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.PageTransformer;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 页面翻转控件
 *
 * @author Sai 支持自动翻页
 */
public class ConvenientBanner<T> extends LinearLayout {
    private CBViewHolderCreator holderCreator;
    private List<T> mDatas;
    private int[] page_indicatorId = {R.drawable.icon_page_indicator_normal, R.drawable.icon_page_indicator_focused};
    private ArrayList<ImageView> mPointViews = new ArrayList<ImageView>();
    private CBPageChangeListener pageChangeListener;
    private CBPageAdapter pageAdapter;
    private CBLoopViewPager viewPager;
    private ViewGroup loPageTurningPoint;
    private long autoTurningTime;
    private boolean turning;
    private boolean canTurn = false;

    private static final float ASPECT_RATIO_DEFAULT_VALUE = -1;
    private float cbAspectRatio = ASPECT_RATIO_DEFAULT_VALUE;
    private int cbIndicatorMarginLeft = 0;
    private int cbIndicatorMarginBottom = 0;
    private int cbIndicatorMarginRight = 0;
    private int cbIndicatorMarginTop = 0;

    private static final int LEFT = 0;
    private static final int CENTER = 1;
    private static final int RIGHT = 2;
    private static final int INDICATOR_GRAVITY_DEFAULT_VALUE = CENTER;
    private int cbIndicatorGravity = INDICATOR_GRAVITY_DEFAULT_VALUE;

    public enum Transformer {
        DefaultTransformer("DefaultTransformer"), AccordionTransformer(
                "AccordionTransformer"), BackgroundToForegroundTransformer(
                "BackgroundToForegroundTransformer"), CubeInTransformer(
                "CubeInTransformer"), CubeOutTransformer("CubeOutTransformer"), DepthPageTransformer(
                "DepthPageTransformer"), FlipHorizontalTransformer(
                "FlipHorizontalTransformer"), FlipVerticalTransformer(
                "FlipVerticalTransformer"), ForegroundToBackgroundTransformer(
                "ForegroundToBackgroundTransformer"), RotateDownTransformer(
                "RotateDownTransformer"), RotateUpTransformer(
                "RotateUpTransformer"), StackTransformer("StackTransformer"), TabletTransformer(
                "TabletTransformer"), ZoomInTransformer("ZoomInTransformer"), ZoomOutSlideTransformer(
                "ZoomOutSlideTransformer"), ZoomOutTranformer(
                "ZoomOutTranformer");

        private final String className;

        // 构造器默认也只能是private, 从而保证构造函数只能在内部使用
        Transformer(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }
    }

    private Handler timeHandler = new Handler();
    private Runnable adSwitchTask = new Runnable() {
        @Override
        public void run() {
            if (viewPager != null && turning) {
                int page = viewPager.getCurrentItem() + 1;
                viewPager.setCurrentItem(page);
                timeHandler.postDelayed(adSwitchTask, autoTurningTime);
            }
        }
    };

    public ConvenientBanner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConvenientBanner(Context context, AttributeSet attrs, int defStyleRes) {
        super(context, attrs, defStyleRes);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ConvenientBanner, 0, defStyleRes);
        cbAspectRatio = a.getFloat(R.styleable.ConvenientBanner_cb_aspect_ratio, ASPECT_RATIO_DEFAULT_VALUE);
        cbIndicatorGravity = a.getInteger(R.styleable.ConvenientBanner_cb_indicator_gravity, INDICATOR_GRAVITY_DEFAULT_VALUE);
        switch( cbIndicatorGravity ) {
            case LEFT :
                cbIndicatorGravity = Gravity.LEFT;
                break;
            case CENTER:
                cbIndicatorGravity = Gravity.CENTER_HORIZONTAL;
                break;
            case RIGHT:
                cbIndicatorGravity = Gravity.RIGHT;
                break;
        }
        cbIndicatorGravity = cbIndicatorGravity | Gravity.BOTTOM;
        int margin = a.getDimensionPixelSize(R.styleable.ConvenientBanner_cb_indicator_margin, 0);
        cbIndicatorMarginBottom = a.getDimensionPixelSize(R.styleable.ConvenientBanner_cb_indicator_margin_bottom, margin);
        cbIndicatorMarginTop = a.getDimensionPixelSize(R.styleable.ConvenientBanner_cb_indicator_margin_top, margin);
        cbIndicatorMarginLeft = a.getDimensionPixelSize(R.styleable.ConvenientBanner_cb_indicator_margin_left, margin);
        cbIndicatorMarginRight = a.getDimensionPixelSize(R.styleable.ConvenientBanner_cb_indicator_margin_right, margin);
        a.recycle();
        init(context);
    }

    private void init(Context context) {
        View hView = LayoutInflater.from(context).inflate(R.layout.include_viewpager, this, true);
        viewPager = (CBLoopViewPager) hView.findViewById(R.id.cbLoopViewPager);
        loPageTurningPoint = (ViewGroup) hView.findViewById(R.id.loPageTurningPoint);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) loPageTurningPoint.getLayoutParams();
        layoutParams.gravity = cbIndicatorGravity;
        layoutParams.leftMargin = cbIndicatorMarginLeft;
        layoutParams.rightMargin = cbIndicatorMarginRight;
        layoutParams.topMargin = cbIndicatorMarginTop;
        layoutParams.bottomMargin = cbIndicatorMarginBottom;
        initViewPagerScroll();
    }

    public ConvenientBanner setPages(CBViewHolderCreator holderCreator,List<T> datas){
        this.mDatas = datas;
        this.holderCreator = holderCreator;
        pageAdapter = new CBPageAdapter(holderCreator,mDatas);
        viewPager.setAdapter(pageAdapter);
        viewPager.setBoundaryCaching(true);
        if (page_indicatorId != null)
            setPageIndicator(page_indicatorId);
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if( cbAspectRatio > 0 ) {
            int width = getMeasuredWidth();
            int height = (int)(width * cbAspectRatio);
            setMeasuredDimension(width, height);
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec)),
                             MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)));
        }
    }

    /**
     * 设置底部指示器是否可见
     *
     * @param visible
     */
    public ConvenientBanner setPointViewVisible(boolean visible) {
        loPageTurningPoint.setVisibility(visible ? View.VISIBLE : View.GONE);
        return this;
    }

    /**
     * 底部指示器资源图片
     *
     * @param page_indicatorId
     */
    public ConvenientBanner setPageIndicator(int[] page_indicatorId) {
        loPageTurningPoint.removeAllViews();
        mPointViews.clear();
        this.page_indicatorId = page_indicatorId;
        for (int count = 0; count < mDatas.size(); count++) {
            // 翻页指示的点
            ImageView pointView = new ImageView(getContext());
            pointView.setPadding(5, 0, 5, 0);
            if (mPointViews.isEmpty())
                pointView.setImageResource(page_indicatorId[1]);
            else
                pointView.setImageResource(page_indicatorId[0]);
            mPointViews.add(pointView);
            loPageTurningPoint.addView(pointView);
        }
        pageChangeListener = new CBPageChangeListener(mPointViews,
                page_indicatorId);
        viewPager.setOnPageChangeListener(pageChangeListener);

        return this;
    }

    public ConvenientBanner startTurning(long autoTurningTime) {
        canTurn = true;
        this.autoTurningTime = autoTurningTime;
        turning = true;
        timeHandler.postDelayed(adSwitchTask, autoTurningTime);
        return this;
    }

    public void stopTurning() {
        turning = false;
        timeHandler.removeCallbacks(adSwitchTask);
    }

    /**
     * 自定义翻页动画效果
     *
     * @param transformer
     * @return
     */
    public ConvenientBanner setPageTransformer(PageTransformer transformer) {
        viewPager.setPageTransformer(true, transformer);
        return this;
    }

    /**
     * 自定义翻页动画效果，使用已存在的效果
     *
     * @param transformer
     * @return
     */
    public ConvenientBanner setPageTransformer(Transformer transformer) {
        try {
            viewPager.setPageTransformer(
                            true,
                            (PageTransformer) Class.forName("com.bigkoo.convenientbanner.transforms." + transformer.getClassName()).newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 设置ViewPager的滑动速度
     * */
    private void initViewPagerScroll() {
        try {
            Field mScroller = null;
            mScroller = ViewPager.class.getDeclaredField("mScroller");
            mScroller.setAccessible(true);
            ViewPagerScroller scroller = new ViewPagerScroller(
                    viewPager.getContext());
//			scroller.setScrollDuration(1500);
            mScroller.set(viewPager, scroller);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (ev.getAction() == MotionEvent.ACTION_UP) {
            // 开始翻页
            if (canTurn)startTurning(autoTurningTime);
        } else if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // 停止翻页
            if (canTurn)stopTurning();
        }
        return super.dispatchTouchEvent(ev);
    }

}
