
package com.example.mapdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MarkerDetailsActivity extends AppCompatActivity {

    TextView tvTitle;
    TextView tvSnippet;
    Button btnUploadPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.markerdetails_activity);
        // retrieve intent & setup
        String ID = getIntent().getStringExtra("title");
        String snippet = getIntent().getStringExtra("snippet");
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvSnippet = (TextView) findViewById(R.id.tvSnippet);
        btnUploadPic = (Button) findViewById(R.id.btnUploadPic);
        btnUploadPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoTakeIntent = new Intent(getApplicationContext(), PhotoTakeActivity.class);
                startActivity(photoTakeIntent);
            }
        });
        // set information
        tvTitle.setText(ID);
        tvSnippet.setText(snippet);
    }

}
