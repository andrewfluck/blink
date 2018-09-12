package com.andrewfluck.blink.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.andrewfluck.blink.R;

import java.util.ArrayList;
import java.util.List;

public class BlinkView extends LinearLayout {
    private final String TAG = BlinkView.class.getSimpleName();

    private boolean mOpen;

    private int mViewWidth;
    private int mViewHeight;

    private float mBlinkHeight;
    private float mBlinkWidth;
    private float mBlinkHalfWidth;
    private float mBlinkHalfHeight;

    private int mQuickstepHomeHeight;

    private float mOffsetX;
    private float mOffsetY;

    private Paint mBlinkBackground;

    private final Path mBlinkPath = new Path();

    private enum Actions {
        BACK,
        HOME,
        RECENTS
    }

    private final List<Actions> mActions = new ArrayList<>();

    public BlinkView(Context context) {
        this(context, null);
    }

    public BlinkView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlinkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(LinearLayout.HORIZONTAL);

        mOpen = false;

        Resources res = context.getResources();

        /* Blink blob dimensions */
        mBlinkWidth = res.getDimension(R.dimen.blink_width);
        mBlinkHeight = res.getDimension(R.dimen.blink_height);
        mBlinkHalfWidth = mBlinkWidth / 2;
        mBlinkHalfHeight = mBlinkHeight / 2;

        /* Blink blob offsets */
        mOffsetX = 0;
        mOffsetY = mBlinkHeight;

        /* Quickstep home dimensions */
        mQuickstepHomeHeight = res.getDimensionPixelSize(R.dimen.quickstep_home_height);

        /* Blink blob paint */
        mBlinkBackground = new Paint();
        mBlinkBackground.setAntiAlias(true);
        mBlinkBackground.setStyle(Paint.Style.FILL);
        mBlinkBackground.setColor(res.getColor(R.color.blink_blob_bg_color));

        /* Quickstep home paint */
        final Paint quickstepHomeFill = new Paint();
        quickstepHomeFill.setAntiAlias(true);
        quickstepHomeFill.setStyle(Paint.Style.FILL);
        quickstepHomeFill.setColor(Color.BLACK);
        quickstepHomeFill.setStrokeWidth(res.getDimensionPixelSize(R.dimen.quickstep_home_height));
        quickstepHomeFill.setStrokeCap(Paint.Cap.ROUND);
        quickstepHomeFill.setAlpha(98);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        setPadding((int) mBlinkHalfWidth, 0, (int) mBlinkHalfWidth, 0);

        layoutItems();
    }

    private void layoutItems() {
        mActions.add(Actions.BACK);
        mActions.add(Actions.HOME);
        mActions.add(Actions.RECENTS);

        int N = mActions.size();
        float weightSum = 1f;

        for (int i = 0; i < N; i++) {
            Actions action = mActions.get(i);
            ImageView imageView = new ImageView(getContext());
            imageView.setTag(action);

            int drawableResId = 0;
            switch (action) {
                case BACK:
                    drawableResId = R.drawable.ic_sysbar_back_button;
                    break;
                case RECENTS:
                    drawableResId = R.drawable.ic_sysbar_rotate_button;
                    break;
                case HOME:
                    drawableResId = R.drawable.ic_sysbar_home_button;
                    break;
            }

            imageView.setImageResource(drawableResId);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
            );
            lp.weight = weightSum;
            lp.gravity = Gravity.CENTER_VERTICAL;
            addView(imageView, lp);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mViewWidth = w;
        mViewHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mViewWidth == 0) {
            post(this::requestLayout);

            return;
        }

        mBlinkPath.reset();

        final float blinkOffsetX = mOffsetX * 0.3f;
        final float blinkOffsetY = mOffsetY * 1.0f;

        float fourthWidth = mBlinkWidth / 4;
        mBlinkPath.moveTo(-mBlinkHalfWidth, mBlinkHeight);
        mBlinkPath.cubicTo(-fourthWidth + blinkOffsetX, mBlinkHeight, -fourthWidth + blinkOffsetX,
                blinkOffsetY, blinkOffsetX, blinkOffsetY);
        mBlinkPath.cubicTo(fourthWidth + blinkOffsetX, blinkOffsetY, fourthWidth + blinkOffsetX,
                mBlinkHeight, mBlinkHalfWidth, mBlinkHeight);
        mBlinkPath.close();

        canvas.translate(mViewWidth / 2, 0);
        canvas.clipPath(mBlinkPath);
        canvas.drawPath(mBlinkPath, mBlinkBackground);
        canvas.translate(-(mViewWidth / 2), 0);
    }

    @Override
    @SuppressWarnings("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP: {
                if (mOpen) {
                    int x = (int) event.getX();

                    int N = getChildCount();
                    for (int i = 0; i < N; i++) {
                        View v = getChildAt(i);
                        Rect clickZone = new Rect();
                        v.getGlobalVisibleRect(clickZone);

                        if (clickZone.left < clickZone.right && clickZone.top < clickZone.bottom) {
                            if (x >= clickZone.left && x < clickZone.right) {
                                switch ((Actions) v.getTag()) {
                                    case BACK:
                                    case HOME:
                                    case RECENTS:
                                }
                                // Action triggered
                                //android.util.Log.e("ANAS", "tag="+v.getTag());
                            }
                        }
                    }
                }

                animateClose();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (event.getPointerCount() > 1) {
                    break;
                }

                float newOffsetX = event.getX() - (mViewWidth / 2);
                float newOffsetY = event.getY();

                if (newOffsetY <= (mViewHeight - mBlinkHeight)) {
                    mOffsetY = mViewHeight - mBlinkHeight;
                } else {
                    mOffsetY = newOffsetY;
                }

                mOffsetX = newOffsetX;

                mOpen = mOffsetY <= (mBlinkHalfHeight - mQuickstepHomeHeight);

                invalidate();
                break;
            }
        }

        return true;
    }

    private void animateClose() {
        ValueAnimator resetX = ValueAnimator.ofFloat(mOffsetX, 0);
        resetX.setDuration(500);
        resetX.setInterpolator(new OvershootInterpolator());
        resetX.addUpdateListener(va -> {
            mOffsetX = (float) va.getAnimatedValue();
            invalidate();
        });

        ValueAnimator resetY = ValueAnimator.ofFloat(mOffsetY, mBlinkHeight);
        resetY.setDuration(500);
        resetY.setInterpolator(new OvershootInterpolator());
        resetY.addUpdateListener(va -> {
            mOffsetY = (float) va.getAnimatedValue();
            invalidate();
        });

        mOpen = false;

        resetX.start();
        resetY.start();
    }
}
