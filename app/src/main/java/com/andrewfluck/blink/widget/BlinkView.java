package com.andrewfluck.blink.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.andrewfluck.blink.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlinkView extends LinearLayout {
    private final String TAG = BlinkView.class.getSimpleName();

    private boolean mOpen;

    private int mViewWidth;
    private int mViewHeight;

    private int mBlinkColorResId;
    private Drawable mBackDrawable;
    private Drawable mRotateDrawable;

    private long lastDown = 0;
    private long lastUp = 0;

    private float mBlinkWidth;
    private float mBlinkHeight;
    private float mBlinkHalfWidth;
    private float mBlinkHalfHeight;

    private int mQuickstepHomeWidth;
    private int mQuickstepHomeHeight;
    private float mAssistantDotRadius;
    private float mAssistantDotGap;

    private float mOffsetX;
    private float mOffsetY;

    private Resources mResources;
    private Context mContext;
    private Paint mBlinkBackground;
    private Paint mQuickstepHomeFill;
    private Vibrator mVibrator;

    private enum Actions {
        BACK,
        HOME,
        RECENTS
    }

    private final List<Actions> mActions = new ArrayList<>();

    public BlinkView(Context context) {
        super(context);
        init(context);
    }

    public BlinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BlinkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(LinearLayout.HORIZONTAL);

        mOpen = false;

        mContext = context;
        mResources = mContext.getResources();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        /* Blink blob dimensions */
        mBlinkWidth = mResources.getDimension(R.dimen.blink_width);
        mBlinkHeight = mResources.getDimension(R.dimen.blink_height);
        mBlinkHalfWidth = mBlinkWidth / 2;
        mBlinkHalfHeight = mBlinkHeight / 2;

        mBlinkColorResId = mResources.getColor(R.color.blink_blob_bg_color);

        mBackDrawable = mResources.getDrawable(R.drawable.ic_sysbar_back_button);
        mRotateDrawable = mResources.getDrawable(R.drawable.ic_sysbar_rotate_button);

        /* Blink blob offsets */
        mOffsetX = 0;
        mOffsetY = mBlinkHeight;

        /* Quickstep home dimensions */
        mQuickstepHomeWidth = mResources.getDimensionPixelSize(R.dimen.quickstep_home_width);
        mQuickstepHomeHeight = mResources.getDimensionPixelSize(R.dimen.quickstep_home_height);
        mAssistantDotRadius = mResources.getDimension(R.dimen.assistant_dot_radius);
        mAssistantDotGap = mResources.getDimension(R.dimen.assistant_dot_gap);

        /* Blink blob paint */
        mBlinkBackground = new Paint();
        mBlinkBackground.setAntiAlias(true);
        mBlinkBackground.setStyle(Paint.Style.FILL);
        mBlinkBackground.setColor(mBlinkColorResId);

        /* Quickstep home paint */
        mQuickstepHomeFill = new Paint();
        mQuickstepHomeFill.setAntiAlias(true);
        mQuickstepHomeFill.setStyle(Paint.Style.FILL);
        mQuickstepHomeFill.setColor(Color.BLACK);
        mQuickstepHomeFill.setStrokeWidth(mQuickstepHomeHeight);
        mQuickstepHomeFill.setStrokeCap(Paint.Cap.ROUND);
        mQuickstepHomeFill.setAlpha(98);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setPadding((int)  mBlinkHalfWidth, 0, (int) mBlinkHalfWidth, 0);

        mActions.add(Actions.BACK);
        mActions.add(Actions.HOME);
        mActions.add(Actions.RECENTS);

        int N = mActions.size();
        float weightSum = 1f;// / N;

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
            android.util.Log.e("ANAS","weightSum="+weightSum);
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

    private void drawBlinkBlob(Canvas canvas, float offsetX, float offsetY) {
//        final float blinkOffsetX = offsetX * 0.3f;
//        final float blinkOffsetY = offsetY * 1.0f;
//        final float qsOffsetX = offsetX * 0.25f;
//        final float qsOffsetY = offsetY * 0.3f;
//        final int threshold = (int) Math.abs(qsOffsetX / (mBlinkHalfWidth / 4) * 100);
//
//        final Path mBlinkPath = new Path();
//
//        float fourthWidth = mBlinkWidth / 4;
//        mBlinkPath.moveTo(-mBlinkHalfWidth, mBlinkHeight);
//        mBlinkPath.cubicTo(-fourthWidth + blinkOffsetX, mBlinkHeight, -fourthWidth + blinkOffsetX, blinkOffsetY, blinkOffsetX, blinkOffsetY);
//        mBlinkPath.cubicTo(fourthWidth + blinkOffsetX, blinkOffsetY, fourthWidth + blinkOffsetX, mBlinkHeight, mBlinkHalfWidth, mBlinkHeight);
//        mBlinkPath.close();
//
//        int alpha = (int) clamp(100 - threshold, 0, 100);
//        int N = getChildCount();
//        for (int i = 0; i < N; i++) {
//            View child = getChildAt(i);
//            child.setAlpha(alpha);
//        }
//
//        canvas.clipPath(mBlinkPath);
//        canvas.drawPath(mBlinkPath, mBlinkBackground);

//        for (int i = 0; mBlinkAssistantColorsResId.length > i; i++) {
//            Paint dotPaint = new Paint();
//            dotPaint.setAntiAlias(true);
//            dotPaint.setStyle(Paint.Style.FILL);
//            dotPaint.setColor(mBlinkAssistantColorsResId[i]);
//
//            canvas.drawCircle((i - (mBlinkAssistantColorsResId.length / 2.666666666f)) * 36, mBlinkHalfHeight + qsOffsetY, mAssistantDotRadius, dotPaint);
//        }
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mViewWidth == 0) {
            post(this::requestLayout);

            return;
        }



        final float blinkOffsetX = mOffsetX * 0.3f;
        final float blinkOffsetY = mOffsetY * 1.0f;
        final float qsOffsetX = mOffsetX * 0.25f;
        final float qsOffsetY = mOffsetY * 0.3f;
        final int threshold = (int) Math.abs(qsOffsetX / (mBlinkHalfWidth / 4) * 100);

        final Path mBlinkPath = new Path();

        float fourthWidth = mBlinkWidth / 4;
        mBlinkPath.moveTo(-mBlinkHalfWidth, mBlinkHeight);
        mBlinkPath.cubicTo(-fourthWidth + blinkOffsetX, mBlinkHeight, -fourthWidth + blinkOffsetX, blinkOffsetY, blinkOffsetX, blinkOffsetY);
        mBlinkPath.cubicTo(fourthWidth + blinkOffsetX, blinkOffsetY, fourthWidth + blinkOffsetX, mBlinkHeight, mBlinkHalfWidth, mBlinkHeight);
        mBlinkPath.close();

        int alpha = (int) clamp(100 - threshold, 0, 100);
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View child = getChildAt(i);
            child.setAlpha(alpha);
        }



        canvas.translate(mViewWidth / 2, 0);
        canvas.clipPath(mBlinkPath);
        canvas.drawPath(mBlinkPath, mBlinkBackground);
        canvas.translate(-(mViewWidth / 2), 0);

    }

    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                lastDown = System.currentTimeMillis();
//                Log.d(TAG, Long.toString(lastDown));
//                Log.d(TAG, "Down");
                break;
            }
            case MotionEvent.ACTION_UP: {
                lastUp = System.currentTimeMillis();
//                Log.d(TAG, Long.toString(lastUp));
//                Log.d(TAG, "Up");
                Log.d(TAG, Long.toString(lastUp - lastDown));
                if ((lastUp - lastDown) > 250 && event.getY() <= 0) {
//                    mVibrator.vibrate(250);
//                    mContext.startActivity(new Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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

    private boolean assistantAvailable(Context context) {
        final String setting = Settings.Secure.getString(context.getContentResolver(), "assistant");

        return setting != null;
    }
}
