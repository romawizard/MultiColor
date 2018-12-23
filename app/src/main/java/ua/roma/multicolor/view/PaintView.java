package ua.roma.multicolor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PaintView extends View {

    public static final String TAG = PaintView.class.getCanonicalName();

    private final int DEFAULT_STROKE_WIDTH = 10;
    private int strokeWidth = DEFAULT_STROKE_WIDTH;
    private Paint paint;
    private SerializablePath path;
    private List<SerializablePath> pathList = new ArrayList<>();
    private List<Integer> colorList = new ArrayList<>();
    private List<Integer> strokeWidthList = new ArrayList<>();
    private int position;
    private StateListener listener;

    public PaintView(Context context) {
        super(context);
        init();
    }

    public PaintView(Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    public void setStateListener(StateListener listener) {
        this.listener = listener;
    }

    public void undo() {
        if (position >= 1) {
            --position;
            invalidate();
        }
        notifyListener();
    }

    public void redo() {
        if (pathList.size() > position) {
            position++;
            invalidate();
        }
        notifyListener();
    }

    public void clear() {
        pathList.clear();
        colorList.clear();
        strokeWidthList.clear();
        position = 0;
        invalidate();
       notifyListener();
    }

    public boolean isUndo() {
        return position > 0;
    }

    public boolean isRedo() {
        return position < pathList.size();
    }

    public boolean isClear() {
        return pathList.isEmpty();
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        paint.setStrokeWidth(strokeWidth);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "onTouchEvent action down");
                touchStart(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent action move");
                touchMove(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouchEvent action up");
                touchEnd();
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getSuggestedMinimumWidth() + getPaddingStart() + getPaddingEnd();
        int height = getSuggestedMinimumWidth() + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(measureDimension(width, widthMeasureSpec), measureDimension(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "size list = " + pathList.size());
        canvas.drawColor(Color.WHITE);
        for (int i = 0; i < position; i++) {
            paint.setColor(colorList.get(i));
            paint.setStrokeWidth(strokeWidthList.get(i));
            canvas.drawPath(pathList.get(i),paint);
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable savedState = super.onSaveInstanceState();
        SavedState ss = new SavedState(savedState);
        ss.savedList = pathList;
        ss.savedPosition = position;
        ss.savedColorList = colorList;
        ss.savedStrokeWidthList = strokeWidthList;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        pathList = savedState.savedList;
        colorList = savedState.savedColorList;
        position = savedState.savedPosition;
        strokeWidthList = savedState.savedStrokeWidthList;
        notifyListener();
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

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);

    }

    private void touchStart(float x, float y) {
        invalidateListPath();
        path = new SerializablePath();
        pathList.add(path);
        colorList.add(paint.getColor());
        strokeWidthList.add(strokeWidth);
        position++;
        path.reset();
        path.moveTo(x, y);
    }

    private void touchMove(float x, float y) {
        path.lineTo(x, y);
    }

    private void touchEnd() {
        notifyListener();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onUndo(isUndo());
            listener.onClear(isClear());
            listener.onRedo(isRedo());
        }
    }

    private void invalidateListPath() {
        if (position < pathList.size()) {
            pathList.subList(position, pathList.size()).clear();
        }
    }

    public void setPaintColor(int paintColor) {
        this.paint.setColor(paintColor);
    }

    public int getCurrentColor() {
        if (colorList.size()<1){
            return Color.BLACK;
        }
        return colorList.get(colorList.size()-1);
    }


    public interface StateListener {
        void onUndo(boolean undo);

        void onRedo(boolean redo);

        void onClear(boolean clear);
    }

    static class SavedState extends BaseSavedState {

        public final static Parcelable.Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        List<SerializablePath> savedList;
        List<Integer> savedColorList;
        List<Integer> savedStrokeWidthList;
        int savedPosition;

        public SavedState(Parcel source) {
            super(source);
            source.readList(savedList, null);
            source.readList(savedColorList,null);
            source.readList(savedStrokeWidthList,null);
            savedPosition = source.readInt();

        }

        public SavedState(Parcelable savedState) {
            super(savedState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeList(savedList);
            out.writeList(savedColorList);
            out.writeList(savedStrokeWidthList);
            out.writeInt(savedPosition);
        }
    }

    static class SerializablePath extends Path implements Serializable {
    }
}
