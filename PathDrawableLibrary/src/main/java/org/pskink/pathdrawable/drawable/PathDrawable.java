package org.pskink.pathdrawable.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;

import org.pskink.pathdrawable.lib.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PathDrawable extends Drawable {
    private final static String TAG = "PathDrawable";

    private Rect mPadding = new Rect();
    private RectF mPathRect = new RectF();
    private ScaleToFit mScaleType = ScaleToFit.FILL;
    private List<Layer> mLayers = new LinkedList<>();

    private OnBoundsChangeListener mListener;
    private Drawable mBackground;

    /**
     * Interface definition for a callback to be invoked when 
     * PathDrawable's bounds are changed.
     * Used with {@link #setOnBoundsChangeListener}
     */
    public interface OnBoundsChangeListener {
        /**
         * Called when bounds are changed.
         * @param drawable
         * @param bounds
         */
        public void onBoundsChange(PathDrawable drawable, Rect bounds);
    }
    
    /**
     * Create a new PathDrawable
     * @param pathWidth
     * @param pathHeight
     */
    public PathDrawable(float pathWidth, float pathHeight) {
        init(pathWidth, pathHeight);
    }
    
    /**
     * Create a new PathDrawable based on XML file
     * @param ctx Context to use
     * @param resId Xml file resource id
     */
    public PathDrawable(Context ctx, int resId) {
        Resources res = ctx.getResources();
        XmlResourceParser parser = res.getXml(resId);
        try {
            parse(parser, res);
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register a callback to be invoked when PathDrawable's bounds are changed.
     *
     * @param listener The callback that will run
     */
    public void setOnBoundsChangeListener(OnBoundsChangeListener listener) {
        mListener = listener;
    }

    /**
     * Add a new Layer
     * @param data Path data
     * @param strokeColor Path color
     * @param strokeWidth Path stroke width
     * @param style Path style
     * @param capType Path cap style
     * @return The new layer.
     */
    public Layer addLayer(String data, int strokeColor, float strokeWidth, Style style, Cap capType) {
        Layer layer = new Layer(data, strokeColor, strokeWidth, style, capType);
        mLayers.add(layer);
        return layer;
    }

    /**
     * Remove the Layer.
     * @param layer - the layer to be removed.
     */
    public void removeLayer(Layer layer) {
        mLayers.remove(layer);
    }

    /**
     * Look for a Layer with the given tag (used when creating PathDrawable from
     * the xml file).
     * @param tag - the tag to search for/
     * @return The layer or null if not found.
     */
    public Layer findLayerByTag(Object tag) {
        for (Layer layer : mLayers) {
            if (tag.equals(layer.mTag)) {
                return layer;
            }
        }
        return null;
    }
    
    /**
     * A helper method to be invoked in {@link LayoutInflater.Factory#onCreateView(String, Context, AttributeSet)}
     * in order to automatically set PathDrawable defined by {@link R.attr#xml_drawable} in xml layout file
     * 
     * @param inflater
     * @param name
     * @param context
     * @param attrs
     * @return
     */
    public static View createView(LayoutInflater inflater, String name, Context context, AttributeSet attrs) {
        String[] prefixes = {
            "android.widget.", "android.view.", "android.webkit.",
        };
        View v = null;
        int[] what = {
            R.attr.xml_drawable
        };
        TypedArray a = context.obtainStyledAttributes(attrs, what);
        int id = a.getResourceId(0, 0);
        if (id != 0) {
            try {
                if (name.indexOf('.') == -1) {
                    for (String prefix : prefixes) {
                        v = inflater.createView(name, prefix, attrs);
                        if (v != null) {
                            break;
                        }
                    }
                } else {
                    v = inflater.createView(name, null, attrs);
                }
                v.setBackgroundDrawable(new PathDrawable(context, id));
            } catch (Exception e) {
                Log.d(TAG, "createView error: ", e);
            }
        }
        a.recycle();
        return v;
    }

    private void init(float pathWidth, float pathHeight) {
        if (pathWidth <= 0 || pathHeight <= 0) {
            throw new RuntimeException("Both pathWidth and pathHeight must be >= 0");
        }
        mPathRect.set(0, 0, pathWidth, pathHeight);
    }
    
    @Override
    protected void onBoundsChange(Rect bounds) {
        if (mBackground != null) {
            mBackground.setBounds(bounds);
        }

        Rect tmpRect = new Rect();
        tmpRect.set(bounds);
        inset(tmpRect, mPadding);

        Matrix matrix = new Matrix();
        matrix.setRectToRect(mPathRect, new RectF(tmpRect), mScaleType);

        for (Layer layer : mLayers) {
            layer.mPath.transform(matrix, layer.mDrawPath);
        }

        if (mListener != null) {
            mListener.onBoundsChange(this, bounds);
        }
    }

    private void setBackground(Drawable background) {
        mBackground = background;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mBackground != null) {
            mBackground.draw(canvas);
        }
        for (Layer layer : mLayers) {
            layer.draw(canvas);
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private void inset(Rect what, Rect by) {
        what.left += by.left;
        what.top += by.top;
        what.right -= by.right;
        what.bottom -= by.bottom;
    }

    private void parse(XmlResourceParser parser, Resources res) throws XmlPullParserException, IOException {
        AttributeSet attrset = Xml.asAttributeSet(parser);
        int[] attrs = R.styleable.PathDrawable;
        TypedArray a;
        
        int type;
        while ((type=parser.next()) != XmlPullParser.START_TAG &&
                type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }
        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }
        String name = parser.getName();
        if (!name.equals("layers")) {
            throw new XmlPullParserException("No <layers> start tag found");
        }
        
        a = res.obtainAttributes(attrset, attrs);
        float pathWidth = a.getFloat(R.styleable.PathDrawable_path_width, 0);
        float pathHeight = a.getFloat(R.styleable.PathDrawable_path_height, 0);
        Drawable background = a.getDrawable(R.styleable.PathDrawable_android_background);
        Rect padding = new Rect();
        int pad;
        pad = a.getDimensionPixelSize(R.styleable.PathDrawable_android_padding, -1);
        if (pad >= 0) padding.set(pad, pad, pad, pad);
        pad = a.getDimensionPixelSize(R.styleable.PathDrawable_android_paddingLeft, -1);
        if (pad >= 0) padding.left = pad;
        pad = a.getDimensionPixelSize(R.styleable.PathDrawable_android_paddingTop, -1);
        if (pad >= 0) padding.top = pad;
        pad = a.getDimensionPixelSize(R.styleable.PathDrawable_android_paddingRight, -1);
        if (pad >= 0) padding.right = pad;
        pad = a.getDimensionPixelSize(R.styleable.PathDrawable_android_paddingBottom, -1);
        if (pad >= 0) padding.bottom = pad;
        mPadding.set(padding);
        mScaleType = ScaleToFit.values()[a.getInt(R.styleable.PathDrawable_scale_type, 0)];
        a.recycle();

        if (pathWidth <= 0 || pathHeight <= 0) {
            String msg = parser.getPositionDescription() + ": Both path_width and path_height must be >= 0";
            throw new XmlPullParserException(msg);
        }
        init(pathWidth, pathHeight);
        setBackground(background);
        
        for (type = parser.next();
                type != XmlPullParser.END_DOCUMENT;
                type = parser.next()) {
            if (type == XmlPullParser.START_TAG) {
                if (parser.getName().equals("layer")) {
                    a = res.obtainAttributes(attrset, attrs);
                    String data = a.getString(R.styleable.PathDrawable_data);
                    String tag = a.getString(R.styleable.PathDrawable_android_tag);
                    int strokeColor = a.getColor(R.styleable.PathDrawable_stroke_color, 0xffffffff);
                    float strokeWidth = a.getDimension(R.styleable.PathDrawable_stroke_width, 1);
                    int strokeStyleFags = a.getInt(R.styleable.PathDrawable_stroke_style, 1);
                    int capTypeInt = a.getInt(R.styleable.PathDrawable_cap_type, 2);
                    a.recycle();

                    Style style;
                    if (strokeStyleFags == 1) {
                        style = Style.STROKE;
                    } else
                    if (strokeStyleFags == 2) {
                        style = Style.FILL;
                    } else {
                        style = Style.FILL_AND_STROKE;
                    }
                    
                    Cap capType = Cap.SQUARE;
                    if (capTypeInt == 0) {
                        capType = Cap.BUTT;
                    } else
                    if (capTypeInt == 1) {
                        capType = Cap.ROUND;
                    }
                    Layer layer = addLayer(data, strokeColor, strokeWidth, style, capType);
                    layer.mTag = tag;
                }
            }
        }
    }

    public static class Layer {
        private Object mTag;
        private Path mPath;
        private Path mDrawPath;
        private Paint mPaint;

        public Layer(String data, int strokeColor, float strokeWidth, Style style, Cap capType) {
            data = data.trim();
            if (data.length() == 0) {
                throw new RuntimeException("path data is empty");
            }
            mDrawPath = new Path();
            mPath = PathParser.createPathFromPathData(data);
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(strokeColor);
            if (style == Style.FILL) {
                strokeWidth = 0;
            }
            mPaint.setStrokeWidth(strokeWidth);
            mPaint.setStyle(style);
            mPaint.setStrokeCap(capType);
            if (capType == Cap.BUTT || capType == Cap.SQUARE) {
                mPaint.setStrokeJoin(Join.BEVEL);
            } else {
                mPaint.setStrokeJoin(Join.ROUND);
            }
        }
        
        /**
         * Get the Paint object used for Path drawing
         * @return the path 
         */
        public Paint getPaint() {
            return mPaint;
        }

        private void draw(Canvas canvas) {
            canvas.drawPath(mDrawPath, mPaint);
        }
    }
}
