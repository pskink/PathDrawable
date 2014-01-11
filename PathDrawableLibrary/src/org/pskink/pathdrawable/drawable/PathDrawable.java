package org.pskink.pathdrawable.drawable;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.pskink.pathdrawable.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
import android.util.Xml;
import android.view.Gravity;

public class PathDrawable extends Drawable {
    private final static String TAG = "PathDrawable";
    
    private RectF mPathRect;
    private LinkedList<Layer> mLayers;
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
        return addLayer(data, strokeColor, strokeWidth, style, capType, null);
    }

    /**
     * Add a new Layer
     * @param data Path data
     * @param strokeColor Path color
     * @param strokeWidth Path stroke width
     * @param style Path style
     * @param capType Path cap style
     * @param parent Parent layer to be used when mapping the path
     * @return The new layer.
     */
    public Layer addLayer(String data, int strokeColor, float strokeWidth, Style style, Cap capType, Layer parent) {
        Layer layer = new Layer(data, strokeColor, strokeWidth, style, capType, parent);
        mLayers.add(layer);
        return layer;
    }

    /**
     * Remove the Layer.
     * @param The layer to be removed.
     */
    public void removeLayer(Layer layer) {
        if (mLayers.remove(layer)) {
            // remove children
            Iterator<Layer> iter = mLayers.iterator();
            while (iter.hasNext()) {
                Layer child = iter.next();
                if (child.mParent == layer) {
                    iter.remove();
                }
            }
        }
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
    
    private void init(float pathWidth, float pathHeight) {
        if (pathWidth <= 0 || pathHeight <= 0) {
            throw new RuntimeException("Both pathWidth and pathHeight must be >= 0");
        }
        mPathRect = new RectF(0, 0, pathHeight, pathHeight);
        mLayers = new LinkedList<Layer>(); 
    }
    
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        
        if (mBackground != null) {
            mBackground.setBounds(bounds);
        }

        // process "root" layers
        for (Layer layer : mLayers) {
            if (layer.mParent == null && layer.mGravity == Gravity.NO_GRAVITY) {
                RectF dst = new RectF(bounds);
                float w2 = layer.mPaint.getStrokeWidth() / 2;
                // easiest solution just to inset all the sides
                dst.inset(w2, w2);
                layer.mMatrix.setRectToRect(mPathRect, dst, ScaleToFit.FILL);
                layer.mPath.transform(layer.mMatrix, layer.mDrawPath);
            }
        }
        // process "child" layers
        for (Layer layer : mLayers) {
            if (layer.mParent != null && layer.mGravity == Gravity.NO_GRAVITY) {
                layer.mPath.transform(layer.mParent.mMatrix, layer.mDrawPath);
            }
        }
        // process "gravity" layers
        
        RectF b = new RectF();
        RectF outRectF = new RectF();
        Rect outRect = new Rect();
        Matrix m = new Matrix();
        for (Layer layer : mLayers) {
            if (layer.mGravity != Gravity.NO_GRAVITY) {
                layer.mPath.computeBounds(b, true);
                b.round(outRect);
                Gravity.apply(layer.mGravity, outRect.width(), outRect.height(),
                        bounds, layer.mXOffset, layer.mYOffset, outRect);
                outRectF.set(outRect);
                float w2 = layer.mPaint.getStrokeWidth() / 2;
                // easiest solution just to inset all the sides
                outRectF.inset(w2, w2);
                m.setRectToRect(b, outRectF, ScaleToFit.FILL);
                layer.mPath.transform(m, layer.mDrawPath);
            }
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
        a.recycle();
        if (pathWidth <= 0 || pathHeight <= 0) {
            String msg = parser.getPositionDescription() + ": Both path_width and path_height must be >= 0";
            throw new XmlPullParserException(msg);
        }
        init(pathWidth, pathHeight);
        setBackground(background);
        
        int level = -1;
        Layer parent = null;
        for (type = parser.next();
                type != XmlPullParser.END_DOCUMENT;
                type = parser.next()) {
            if (type == XmlPullParser.START_TAG) {
                if (parser.getName().equals("layer")) {
                    level++;
                    if (level > 1) {
                        String msg = parser.getPositionDescription() + ": The parent must be a \"root\" <Layer>";
                        throw new XmlPullParserException(msg);
                    }
                    a = res.obtainAttributes(attrset, attrs);
                    String data = a.getString(R.styleable.PathDrawable_data);
                    String tag = a.getString(R.styleable.PathDrawable_android_tag);
                    int strokeColor = a.getColor(R.styleable.PathDrawable_stroke_color, 0xffffffff);
                    float strokeWidth = a.getDimension(R.styleable.PathDrawable_stroke_width, 1);
                    int strokeStyleFags = a.getInt(R.styleable.PathDrawable_stroke_style, 1);
                    int capTypeInt = a.getInt(R.styleable.PathDrawable_cap_type, 2);
                    int gravity = a.getInt(R.styleable.PathDrawable_android_gravity, Gravity.NO_GRAVITY);
                    int xOffset = a.getDimensionPixelOffset(R.styleable.PathDrawable_x_offset, 0);
                    int yOffset = a.getDimensionPixelOffset(R.styleable.PathDrawable_y_offset, 0);
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
                    Layer layer;
                    if (level == 0) {
                        layer = parent = addLayer(data, strokeColor, strokeWidth, style, capType, null);
                    } else {
                        layer = addLayer(data, strokeColor, strokeWidth, style, capType, parent);
                    }
                    layer.setGravity(gravity, xOffset, yOffset);
                    layer.mTag = tag;
                }
            } else
            if (type == XmlPullParser.END_TAG) {
                if (parser.getName().equals("layer")) {
                    level--;
                }
            }
        }
    }

    public static class Layer {
        private Object mTag;
        private Path mPath;
        private Path mDrawPath;
        private Paint mPaint;
        private Layer mParent;
        private Matrix mMatrix;
        private int mGravity = Gravity.NO_GRAVITY;
        private int mXOffset;
        private int mYOffset;

        public Layer(String data, int strokeColor, float strokeWidth, Style style, Cap capType, Layer parent) {
            data = data.trim();
            if (data.length() == 0) {
                throw new RuntimeException("path data is empty");
            }
            if (parent == null) {
                mMatrix = new Matrix();
            } else {
                if (parent.mParent != null) {
                    throw new RuntimeException("The parent must be a \"root\" Layer");
                }
            }
            mDrawPath = mPath = new Path();
            readPath(data, mPath);
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
            mParent = parent;
        }
        
        /**
         * Get the Paint object used for Path drawing
         * @return the path 
         */
        public Paint getPaint() {
            return mPaint;
        }
        
        /**
         * Set the layer's Gravity with optional offsets
         * (see: {@link Gravity.apply(int, int, int, Rect, int, int, Rect)}}
         * @param gravity
         * @param xOffset
         * @param yOffset
         */
        public void setGravity(int gravity, int xOffset, int yOffset) {
            mGravity = gravity;
            mXOffset = xOffset; 
            mYOffset = yOffset; 
        }

        private void draw(Canvas canvas) {
            canvas.drawPath(mDrawPath, mPaint);
        }

        private void readPath(String data, Path p) {
            try {
                String[] tokens = data.split("[ ,]");
                int i = 0;
                while (i < tokens.length) {
                    String token = tokens[i++];
                    if (token.equals("M")) {
                        float x = Float.valueOf(tokens[i++]);
                        float y = Float.valueOf(tokens[i++]);
                        p.moveTo(x, y);
                    } else
                    if (token.equals("L")) {
                        float x = Float.valueOf(tokens[i++]);
                        float y = Float.valueOf(tokens[i++]);
                        p.lineTo(x, y);
                    } else
                    if (token.equals("C")) {
                        float x1 = Float.valueOf(tokens[i++]);
                        float y1 = Float.valueOf(tokens[i++]);
                        float x2 = Float.valueOf(tokens[i++]);
                        float y2 = Float.valueOf(tokens[i++]);
                        float x3 = Float.valueOf(tokens[i++]);
                        float y3 = Float.valueOf(tokens[i++]);
                        p.cubicTo(x1, y1, x2, y2, x3, y3);
                    } else
                    if (token.equals("z")) {
                        p.close();
                    } else {
                        throw new RuntimeException("unknown command [" + token + "]");
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException("bad data ", e);
            }
        }
    }
}
