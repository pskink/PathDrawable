package org.pskink.pathdrawable;

import org.pskink.pathdrawable.drawable.PathDrawable;
import org.pskink.pathdrawable.drawable.PathDrawable.OnBoundsChangeListener;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.MaskFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;

public class Test extends Activity {
    private final static String TAG = "Main";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        GridView gv = new GridView(this);
        gv.setNumColumns(2);
        gv.setVerticalSpacing(8);
        gv.setHorizontalSpacing(8);
        ListAdapter adapter = new Adapter();
        gv.setAdapter(adapter);
        setContentView(gv);
    }
    
    class Adapter extends BaseAdapter {
        private static final int SQUARE = 0;
        private static final int TRIANGLE = 1;
        private static final int PENTAGON = 2;
        private static final int PENTAGON_OUTLINE = 3;
        private static final int DIAGONAL = 20;
        private static final int DYNAMIC_GRADIENT = 21;
        private static final int DYNAMIC_GRADIENT_PATTERN = 22;
        private static final int PATTERN = 23;
        int[] mSamples = {
                SQUARE,
                TRIANGLE,
                PENTAGON,
                PENTAGON_OUTLINE,
                DYNAMIC_GRADIENT,
                DYNAMIC_GRADIENT_PATTERN,
                DIAGONAL,
                PATTERN,
        };
        @Override
        public int getCount() {
            return mSamples.length;
        }

        @Override
        public Object getItem(int position) {
            return mSamples[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            V v = (V) convertView;
            if (v == null) {
                v = new V(Test.this);
            }
            v.setSample(mSamples[position]);
            v.setBackground();
            return v;
        }
    }
    
    class V extends View implements OnBoundsChangeListener {
        private int mSample;

        public V(Context context) {
            super(context);
        }
        
        public void setSample(int sample) {
            mSample = sample;
        }

        public void setBackground() {
            PathDrawable pd = null;
            Context ctx = getContext();
            switch (mSample) {
            case Adapter.SQUARE:
                pd = new PathDrawable(ctx, R.xml.square);
                break;

            case Adapter.TRIANGLE:
                pd = new PathDrawable(ctx, R.xml.triangle);
                break;

            case Adapter.PENTAGON:
                pd = new PathDrawable(ctx, R.xml.pentagon);
                break;
                
            case Adapter.PENTAGON_OUTLINE:
                pd = new PathDrawable(ctx, R.xml.pentagon_outline);
                break;
                
            case Adapter.DYNAMIC_GRADIENT:
            case Adapter.DYNAMIC_GRADIENT_PATTERN:
                pd = new PathDrawable(ctx, R.xml.dynamic_gradient);
                pd.setOnBoundsChangeListener(this);
                break;
                
            case Adapter.DIAGONAL:
                pd = new PathDrawable(ctx, R.xml.diagonal);
                MaskFilter filter = new BlurMaskFilter(20, Blur.NORMAL);
                pd.findLayerByTag("layer1").getPaint().setMaskFilter(filter);
                break;
                
            case Adapter.PATTERN:
                pd = new PathDrawable(ctx, R.xml.pattern);
                pd.setOnBoundsChangeListener(this);
                break;

            default:
                break;
            }
            if (pd != null) {
                setBackgroundDrawable(pd);
            } else {
                setBackgroundColor(0xff888888);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }

        @Override
        public void onBoundsChange(PathDrawable drawable, Rect bounds) {
            switch (mSample) {
            case Adapter.DYNAMIC_GRADIENT:
                Shader shader = new LinearGradient(bounds.left, 0, bounds.right, 0, 0x00ffffff, 0xffffffff, TileMode.CLAMP);
                drawable.findLayerByTag("layer1").getPaint().setShader(shader);
                break;
            case Adapter.DYNAMIC_GRADIENT_PATTERN:
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hexagon);
                Shader shader0 = new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
                Matrix m = new Matrix();
                m.postScale(1/3f, 1/3f);
                m.postRotate(20);
                shader0.setLocalMatrix(m);
                Shader shader1 = new LinearGradient(bounds.left, 0, bounds.right, 0, 0x00ffffff, 0xffffffff, TileMode.CLAMP);
                Shader cs = new ComposeShader(shader0, shader1, Mode.SRC_IN);
                drawable.findLayerByTag("layer1").getPaint().setShader(cs);
                break;
            case Adapter.PATTERN:
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hexagon);
                shader0 = new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
                m = new Matrix();
                m.postScale(1/3f, 1/3f);
                m.postRotate(20);
                shader0.setLocalMatrix(m);
                shader1 = new LinearGradient(bounds.left, 0, bounds.right, 0, 0x66ffd700, 0xffffd700, TileMode.CLAMP);
                cs = new ComposeShader(shader0, shader1, Mode.SRC_IN);
                drawable.findLayerByTag("layer0").getPaint().setShader(cs);
                break;
            }
        }
    }
}
