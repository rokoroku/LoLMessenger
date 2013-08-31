package com.rokoroku.lolmessenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by Youngrok Kim on 13. 8. 22.
 */
public class EditingActivity extends Activity {

    private EditText mEditText;
    private Button mButtonCancel;
    private Button mButtonConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editing);

        // Show the Up button in the action bar.
        mEditText = (EditText) findViewById(R.id.editText);
        mButtonCancel = (Button) findViewById(R.id.button);
        mButtonConfirm = (Button) findViewById(R.id.button2);

        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel();
            }
        });
        mButtonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirm();
            }
        });

        mEditText.setText( getIntent().getStringExtra("original_status_message") );

        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(false);

    }

    public void confirm() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        intent.putExtra("statusMsg", mEditText.getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void cancel() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

}
