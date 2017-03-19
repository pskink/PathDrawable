package org.pskink.pathdrawable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Main extends Activity implements OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView lv = new ListView(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        a.add("Dynamic PathDrawable");
        a.add("Static PathDrawable");
        lv.setAdapter(a);
        lv.setOnItemClickListener(this);
        setContentView(lv);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            startActivity(new Intent(this, DynamicPathDrawable.class));
        } else if (position == 1) {
            startActivity(new Intent(this, StaticPathDrawable.class));
        }
    }
}
