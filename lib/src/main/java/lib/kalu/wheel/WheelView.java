package lib.kalu.wheel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * description: 选择器
 * created by kalu on 2018/5/27 15:43
 */
public final class WheelView extends View {

    private final Paint mPaint = new Paint();
    private final Scroller mScroller = new Scroller(getContext());
    private final ArrayList<String> mWheelDatas = new ArrayList<>();

    // 本次滑动的y坐标偏移值
    private float offsetY;
    // 当前选中项
    private int selectPosition = 0;

    // 边框
    private boolean isFill;
    private float stockSize = 0f;
    private int stockColor = Color.TRANSPARENT;
    // private final Vibrator mVibrator = (Vibrator) getContext().getSystemService(getContext().VIBRATOR_SERVICE);

    // 文字大小
    private float textSize = 14f;
    // 颜色，默认Color.BLACK
    private int textColorNormal, textColorSelect;
    // 文字最大放大比例，默认2.0f
    private float textMaxScale;
    // 文字最小alpha值，范围0.0f~1.0f，默认0.4f
    private float textMinAlpha;
    // 是否循环模式，默认是
    private boolean isLoop;
    // 正常状态下最多显示几个文字，默认3（偶数时，边缘的文字会截断）
    private int mItemCount = 5;

    // 按下时的y坐标
    private float downY;
    // 在fling之前的offsetY
    private float oldOffsetY;
    private int offsetIndex;

    // 回弹距离
    private float bounceDistance;
    // 是否正处于滑动状态
    private boolean isSliding = false;

    private String mCurText = "";

    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = null;
        try {
            a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WheelView, defStyleAttr, 0);
            textSize = a.getDimension(R.styleable.WheelView_wv_text_size, textSize);
            textColorNormal = a.getColor(R.styleable.WheelView_wv_text_color_normal, Color.BLACK);
            textColorSelect = a.getColor(R.styleable.WheelView_wv_text_color_select, Color.RED);
            textMaxScale = a.getFloat(R.styleable.WheelView_wv_text_scale, 1.1f);
            textMinAlpha = a.getFloat(R.styleable.WheelView_wv_text_alpha, 0.4f);
            isLoop = a.getBoolean(R.styleable.WheelView_wv_text_loop, false);
            mItemCount = a.getInteger(R.styleable.WheelView_wv_text_count, mItemCount);
            stockColor = a.getColor(R.styleable.WheelView_wv_stock_color, stockColor);
            stockSize = a.getDimension(R.styleable.WheelView_wv_stock_size, stockSize);
            isFill = a.getBoolean(R.styleable.WheelView_wv_fill, isFill);
        } catch (Exception e) {
            if (null == a) return;
            a.recycle();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // 多点不响应
        if (mWheelDatas.isEmpty() || event.getPointerCount() != 1)
            return super.onTouchEvent(event);

        mSimpleOnGestureListener.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                    stopScroll();
                }
                downY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                stopScroll();
                break;
        }
        return true;
    }

    private final GestureDetector mSimpleOnGestureListener = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // 向下滑动, distanceY<0
            if (distanceY < 0 && getSelectPosition() == 0 && !isLoop) {
                return false;
            }
            // 向上滑动, distanceY>0
            else if (distanceY > 0 && getSelectPosition() == (mWheelDatas.size() - 1) && !isLoop) {
                return false;
            }

            offsetY -= distanceY;
            final int scaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (isSliding || Math.abs(offsetY) > scaledTouchSlop) {
                isSliding = true;
                resetDraw();
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Log.e("onFling", "velocityY = " + velocityY + ", selectPosition = " + selectPosition);

            final int velocity = (int) (velocityY * 0.3f);

            oldOffsetY = offsetY;
            mScroller.fling(0, 0, 0, velocity, 0, 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);

            // 没有滑动，则判断点击事件
            if (!isSliding) {
                if (downY < getHeight() / 3)
                    moveBy(-1);
                else if (downY > 2 * getHeight() / 3)
                    moveBy(1);
            }

            isSliding = false;
            return false;
        }
    });

    @Override
    protected void onDraw(Canvas canvas) {

        if (mWheelDatas.isEmpty())
            return;

        final float height = getHeight();
        final float width = getWidth();
        final float itemHeight = getHeight() * 0.2f;
        final float centerY = height * 0.5f;
        final float centerX = width * 0.5f;

        // 选中状态边框
        if (stockSize != 0f) {
            final float top = centerY - height * 0.1f;
            final float bottom = centerY + height * 0.1f;
            final float left = getPaddingLeft() + stockSize * 0.5f;
            final float right = getWidth() - getPaddingRight() - stockSize * 0.5f;
            mPaint.reset();
            mPaint.clearShadowLayer();
            mPaint.setAntiAlias(true);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStyle(isFill ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
            mPaint.setFakeBoldText(true);
            mPaint.setStrokeWidth(stockSize);
            mPaint.setColor(stockColor);
            canvas.drawRect(left, top, right, bottom, mPaint);
        }

        // LogUtil.e("wheel", "onDraw ==> selectPosition = " + selectPosition);

        // 绘制文字，从当前中间项往前、后一共绘制maxShowNum个字
        int size = mWheelDatas.size();
        int half = mItemCount / 2 + 1;
        for (int i = -half; i <= half; i++) {
            int index = selectPosition - offsetIndex + i;

            if (isLoop) {
                if (index < 0)
                    index = (index + 1) % mWheelDatas.size() + mWheelDatas.size() - 1;
                else if (index > mWheelDatas.size() - 1)
                    index = index % mWheelDatas.size();
            }

            if (index >= 0 && index < size) {
                // 计算每个字的中间y坐标
                float tempY = centerY + i * itemHeight;
                tempY += offsetY % itemHeight;

                // 根据每个字中间y坐标到cy的距离，计算出scale值
                float scale = 1.0f - (1.0f * Math.abs(tempY - centerY) / itemHeight);

                // 根据textMaxScale，计算出tempScale值，即实际text应该放大的倍数，范围 1~textMaxScale
                float tempScale = scale * (textMaxScale - 1.0f) + 1.0f;
                tempScale = tempScale < 1.0f ? 1.0f : tempScale;
                float tempAlpha = (tempScale - 1) / (textMaxScale - 1);
                float textAlpha = (1 - textMinAlpha) * tempAlpha + textMinAlpha;

                mPaint.reset();
                mPaint.clearShadowLayer();
                mPaint.setAntiAlias(true);
                mPaint.setStrokeJoin(Paint.Join.ROUND);
                mPaint.setStrokeCap(Paint.Cap.ROUND);
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mPaint.setFakeBoldText(false);
                mPaint.setTextSize(textSize);
                mPaint.setTextSize(textSize * tempScale);
                mPaint.setAlpha((int) (255 * textAlpha));

                // 绘制
                Paint.FontMetrics tempFm = mPaint.getFontMetrics();
                String text = mWheelDatas.get(index);
                float textWidth = mPaint.measureText(text);
                if (tempScale == 1.0f) {
                    mPaint.setColor(textColorNormal);
                } else {
                    mPaint.setColor(textColorSelect);
                    //   mVibrator.vibrate(10);
                }
                canvas.drawText(text, centerX - textWidth / 2, tempY - (tempFm.ascent + tempFm.descent) / 2, mPaint);
            }
        }
    }

    @Override
    public void computeScroll() {
        if (!mScroller.computeScrollOffset())
            return;

        offsetY = oldOffsetY + mScroller.getCurrY();

        // 向下滑动, distanceY<0
        if (selectPosition == 0 && !isLoop) {
            stopScroll();
        }
        // 向上滑动, distanceY>0
        else if (selectPosition == (mWheelDatas.size() - 1) && !isLoop) {
            stopScroll();
        } else if (mScroller.isFinished()) {
            stopScroll();
        } else {
            resetDraw();
        }
    }

    /********************************************************************************************/

    private final void resetDraw() {
        
        if(mWheelDatas.isEmpty())
            return;

        final float itemHeight = getHeight() * 0.2f;
        final int i = (int) (offsetY / (itemHeight));

        if (isLoop || (selectPosition - i >= 0 && selectPosition - i < mWheelDatas.size())) {
            if (offsetIndex != i) {
                offsetIndex = i;

                final int index = getNowIndex(-offsetIndex);
                mCurText = mWheelDatas.get(index);
                if (null != mWheelDatasener) {
                    //   mVibrator.vibrate(10);
                    // selectPosition = index;
                    mWheelDatasener.onChange(index, mCurText);
                }
            }
            postInvalidate();
        } else {
            stopScroll();
        }
    }

    private final void stopScroll() {
        // 判断结束滑动后应该停留在哪个位置

        final float itemHeight = getHeight() * 0.2f;

        float v = offsetY % itemHeight;
        if (v > 0.5f * itemHeight)
            ++offsetIndex;
        else if (v < -0.5f * itemHeight)
            --offsetIndex;

        // 重置selectPosition
        selectPosition = getNowIndex(-offsetIndex);

        // 计算回弹的距离
        bounceDistance = offsetIndex * itemHeight - offsetY;
        offsetY += bounceDistance;

        // 更新
        mCurText = mWheelDatas.get(selectPosition);
        if (null != mWheelDatasener) {
            //  mVibrator.vibrate(10);
            mWheelDatasener.onChange(selectPosition, mCurText);
        }

        // 重绘
        reset();
        postInvalidate();
    }

    private final int getNowIndex(int offsetIndex) {
        int index = selectPosition + offsetIndex;
        if (isLoop) {
            if (index < 0)
                index = (index + 1) % mWheelDatas.size() + mWheelDatas.size() - 1;
            else if (index > mWheelDatas.size() - 1)
                index = index % mWheelDatas.size();
        } else {
            if (index < 0)
                index = 0;
            else if (index > mWheelDatas.size() - 1)
                index = mWheelDatas.size() - 1;
        }
        return index;
    }

    private final void reset() {
        offsetY = 0;
        oldOffsetY = 0;
        offsetIndex = 0;
        bounceDistance = 0;
    }

    /********************************************************************************************/

    /**
     * 获取当前状态下，选中的下标
     *
     * @return 选中的下标
     */
    public int getSelectPosition() {
        return selectPosition - offsetIndex;
    }

    public String getCurText() {
        return mCurText;
    }

    public String getDefaultText() {
        return mWheelDatas.get(0);
    }

    public void setDefault(String str) {

        if (null == mWheelDatas || mWheelDatas.size() == 0 || !mWheelDatas.contains(str))
            return;

        mCurText = str;
        selectPosition = mWheelDatas.indexOf(str);
    }

    public void setDefault(int index) {
        if (index < 0 || index >= mWheelDatas.size() || selectPosition == index)
            return;
        mCurText = mWheelDatas.get(index - 1);
        selectPosition = (index - 1);
    }

    public void moveTo(String str) {

        if (null == mWheelDatas || mWheelDatas.size() == 0 || !mWheelDatas.contains(str)) return;
        selectPosition = mWheelDatas.indexOf(str);
        postInvalidate();
    }

    public void moveTo(int index) {
        if (index < 0 || index >= mWheelDatas.size() || selectPosition == index)
            return;
        selectPosition = (index - 1);
        postInvalidate();
    }

    public void moveTo(int index, int smoothTime) {
        if (index < 0 || index >= mWheelDatas.size() || selectPosition == index)
            return;

        if (!mScroller.isFinished())
            mScroller.forceFinished(true);

        stopScroll();

        final float itemHeight = getHeight() * 0.2f;

        float dy = 0f;
        if (!isLoop) {
            dy = (selectPosition - index) * itemHeight;
        } else {
            float offsetIndex = selectPosition - index;
            float d1 = Math.abs(offsetIndex) * itemHeight;
            float d2 = (mWheelDatas.size() - Math.abs(offsetIndex)) * itemHeight;

            if (offsetIndex > 0) {
                if (d1 < d2)
                    dy = d1; // ascent
                else
                    dy = -d2; // descent
            } else {
                if (d1 < d2)
                    dy = -d1; // descent
                else
                    dy = d2; // ascent
            }
        }
        mScroller.startScroll(0, 0, 0, (int) dy, smoothTime);
        postInvalidate();
    }

    public final void setLoop(boolean loop) {
        isLoop = loop;
    }

    /**
     * 滚动指定的偏移量
     *
     * @param offsetIndex 指定的偏移量
     */
    public void moveBy(int offsetIndex) {
        moveTo(getNowIndex(offsetIndex));
    }

    /**********************************************************************************************/

    private OnWheelChangeListener mWheelDatasener;

    public interface OnWheelChangeListener {
        void onChange(int index, String str);
    }

    public void setOnWheelChangeListener(OnWheelChangeListener mWheelDatasener) {
        this.mWheelDatasener = mWheelDatasener;
    }

    public interface WheelModel {
        String getWheel();
    }

    /**********************************************************************************************/

    public final void setList(List<String> mWheelDatas1) {
        setList(mWheelDatas1, 0);
    }

    public final void setList(List<String> mWheelDatas1, int select) {

        if (null == mWheelDatas1 || mWheelDatas1.size() == 0)
            return;

        mWheelDatas.clear();
        mWheelDatas.addAll(mWheelDatas1);

        // 更新maxTextWidth
        if (null != mWheelDatas && mWheelDatas.size() > 0) {

            mCurText = mWheelDatas.get(select);
            if (null != mWheelDatasener) {
                //   mVibrator.vibrate(10);
                selectPosition = 0;
                mWheelDatasener.onChange(0, mWheelDatas.get(0));
            }
            selectPosition = select;
        }
        // requestLayout();
        postInvalidate();
    }
}
