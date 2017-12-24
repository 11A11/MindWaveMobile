package com.neurosky.mindwavemobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

/**
 * Created by Mukesh on 11/3/2016.
 */
public class MainActivity extends Activity implements View.OnClickListener {
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        askPermission();
        initView();
    }

    // function to initialise view
    private void initView(){
        findViewById(R.id.btn_recordNew).setOnClickListener(this);
        findViewById(R.id.btn_viewPrev).setOnClickListener(this);
        findViewById(R.id.btn_at).setOnClickListener(this);
        findViewById(R.id.cmp_att).setOnClickListener(this);
        findViewById(R.id.btn_at_2).setOnClickListener(this);
        findViewById(R.id.btn_at_3).setOnClickListener(this);
        findViewById(R.id.btn_at_4).setOnClickListener(this);
        findViewById(R.id.btn_at_5).setOnClickListener(this);
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    // this app needs permission to read and write
    private void askPermission(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { //permission already granted
            return;
        }
        if (!Settings.System.canWrite(getApplicationContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 200);

        }
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showMessageOKCancel("You need to allow access to External Storage",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { //permission already granted
                                    return;
                                }
                                requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_CODE_ASK_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.btn_recordNew :
                intent = new Intent(this, BluetoothDeviceActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_viewPrev :
                intent = new Intent(this, ViewPrevious.class);
                startActivity(intent);
                break;
            case R.id.btn_at :
                intent = new Intent(this, RecordAttention.class);
                startActivity(intent);
                break;
            case R.id.cmp_att :
                intent = new Intent(this, CompareAttention.class);
                startActivity(intent);
                break;
            case R.id.btn_at_2 :
                intent = new Intent(this, RecordAttention2.class);
                startActivity(intent);
                break;
            case R.id.btn_at_3 :
                intent = new Intent(this, RecordAttentionImg.class);
                startActivity(intent);
                break;
            case R.id.btn_at_4 :
                intent = new Intent(this, RecordAttentionPdf.class);
                startActivity(intent);
                break;
            case R.id.btn_at_5 :
                intent = new Intent(this,RecordAttentionPdfCopy.class);
                startActivity(intent);
                break;
        }
    }
}
