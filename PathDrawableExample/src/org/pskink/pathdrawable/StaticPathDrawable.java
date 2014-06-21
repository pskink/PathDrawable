package org.pskink.pathdrawable;

import org.pskink.pathdrawable.drawable.PathDrawable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class StaticPathDrawable extends Activity {
    private static final String TAG = "StaticPathDrawable";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLayoutInflater().setFactory(this);
        setContentView(R.layout.static_layout);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        View v = PathDrawable.createView(getLayoutInflater(), name, context, attrs);
        Log.d(TAG, "onCreateView " + name + " " + v);
        return v;
    }
}
