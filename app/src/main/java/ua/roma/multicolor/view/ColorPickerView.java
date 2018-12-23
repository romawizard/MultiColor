package ua.roma.multicolor.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import ua.roma.multicolor.R;

public class ColorPickerView extends View {

    public static final String TAG = ColorPickerView.class.getCanonicalName();
    private static final float PI = 3.1415926f;
    private int centerRadius;
    private int paletteWidth;
    private Paint palettePaint;
    private Paint selectedPaint;
    private int[] colors;
    private OnColorChangedListener listener;

    public ColorPickerView(Context context) {
        super(context);
        colors = new int[]{
                0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
                0xFFFFFF00, 0xFFFF0000
        };
        init();
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        colors = new int[]{
                0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
                0xFFFFFF00, 0xFFFF0000
        };
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ColorPickerView,
                0, 0);
        try {
            centerRadius = array.getInteger(R.styleable.ColorPickerView_center_radius, 32);
            paletteWidth = array.getInteger(R.styleable.ColorPickerView_palette_width, 64);
        } finally {
            array.recycle();
        }
        init();
    }

    private void init() {
        palettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        palettePaint.setStyle(Paint.Style.STROKE);
        palettePaint.setStrokeWidth(paletteWidth);

        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setColor(Color.WHITE);
        selectedPaint.setStyle(Paint.Style.FILL);
        selectedPaint.setStrokeWidth(5);
    }

    public int getCenterRadius() {
        return centerRadius;
    }

    public void setCenterRadius(int centerRadius) {
        this.centerRadius = centerRadius;
        invalidate();
    }

    public void setListener(OnColorChangedListener listener) {
        this.listener = listener;
        listener.colorChanged(palettePaint.getColor());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float centerX = getWidth() / 2 + getPaddingStart() - getPaddingEnd();
        float centerY = getHeight() / 2 + getPaddingTop() - getPaddingBottom();
        float r = Math.min(centerX, centerY) - paletteWidth / 2;
        SweepGradient gradient = new SweepGradient(centerX, centerY, colors, null);
        palettePaint.setShader(gradient);
        canvas.drawCircle(centerX, centerY, r, palettePaint);
        canvas.drawCircle(centerX, centerY, centerRadius, selectedPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getSuggestedMinimumWidth() + getPaddingStart() + getPaddingEnd();
        int height = getSuggestedMinimumWidth() + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(measureDimension(width, widthMeasureSpec), measureDimension(height, heightMeasureSpec));
    }

    private int measureDimension(int desiredSize, int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = desiredSize;
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    private int floatToByte(float x) {
        int n = Math.round(x);
        return n;
    }

    private int pinToByte(int n) {
        if (n < 0) {
            n = 0;
        } else if (n > 255) {
            n = 255;
        }
        return n;
    }

    private int ave(int s, int d, float p) {
        return s + java.lang.Math.round(p * (d - s));
    }

    private int interpColor(int colors[], float unit) {
        if (unit <= 0) {
            return colors[0];
        }
        if (unit >= 1) {
            return colors[colors.length - 1];
        }

        float p = unit * (colors.length - 1);
        int i = (int) p;
        p -= i;

        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i + 1];
        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);

        return Color.argb(a, r, g, b);
    }

    private int rotateColor(int color, float rad) {
        float deg = rad * 180 / 3.1415927f;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        ColorMatrix cm = new ColorMatrix();
        ColorMatrix tmp = new ColorMatrix();

        cm.setRGB2YUV();
        tmp.setRotate(0, deg);
        cm.postConcat(tmp);
        tmp.setYUV2RGB();
        cm.postConcat(tmp);

        final float[] a = cm.getArray();

        int ir = floatToByte(a[0] * r + a[1] * g + a[2] * b);
        int ig = floatToByte(a[5] * r + a[6] * g + a[7] * b);
        int ib = floatToByte(a[10] * r + a[11] * g + a[12] * b);

        return Color.argb(Color.alpha(color), pinToByte(ir),
                pinToByte(ig), pinToByte(ib));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - getWidth() / 2;
        float y = event.getY() - getHeight() / 2;
        boolean inCenter = Math.sqrt(x * x + y * y) <= getWidth() / 2;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (inCenter) {
                    invalidate();
                }

            case MotionEvent.ACTION_MOVE:
                if (inCenter) {
                    float angle = (float) java.lang.Math.atan2(y, x);
                    // need to turn angle [-PI ... PI] into unit [0....1]
                    float unit = angle / (2 * PI);
                    if (unit < 0) {
                        unit += 1;
                    }
                    selectedPaint.setColor(interpColor(colors, unit));
                    listener.colorChanged(selectedPaint.getColor());
                    invalidate();
                }

                break;
            case MotionEvent.ACTION_UP:
                if (inCenter) {
                    invalidate();
                }
                break;
        }
        return true;
    }

    public int getColor() {
        return selectedPaint.getColor();
    }

    public void setInitialColor(int initialColor) {
        selectedPaint.setColor(initialColor);
        if (listener != null) {
            listener.colorChanged(initialColor);
        }
        invalidate();
    }


    public interface OnColorChangedListener {
        void colorChanged(int color);
    }
}
