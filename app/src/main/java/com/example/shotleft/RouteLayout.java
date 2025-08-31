package com.example.shotleft;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class RouteLayout extends LinearLayout {
    TextView id, name, mark;
    public RouteLayout(Context context) {
        super(context);


        LayoutInflater.from(context).inflate(R.layout.layout_route_one,this,true);

        id = findViewById(R.id.id);
        name = findViewById(R.id.name);
        mark = findViewById(R.id.mark);

    }

    public void populate(JSONObject jo) throws JSONException {
        id.setText(jo.getString("id"));
        name.setText(jo.getString("name"));
        mark.setText(jo.getString("mark"));
    }
}
