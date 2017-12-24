package com.neurosky.mindwavemobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Mukesh on 11/3/2016.
 */
public class ViewPrevious extends Activity implements View.OnClickListener {

    final int COMPRESSED_FILE = 0;
    final int TEXT_FILE = 1;
    private static final int FILE_SELECT_CODE = 0;
    private static final String TAG = "MindWave Select File";
    DrawWaveView waveView = null;
    private LinearLayout wave_layout;
    boolean fileSelected = false;
    String fileContent = null ;
    int fileType;


    Button selectCompressed, start, stop, selectText;
    TextView filename;
    BackgroundWork backgroundWork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_previous);
        initView();
        setUpDrawWaveView();
    }

    // function to initialise view
    private void initView(){
        wave_layout = (LinearLayout) findViewById(R.id.wave_layout);
        selectCompressed = (Button) findViewById(R.id.btn_select_com);
        selectText = (Button) findViewById(R.id.btn_select_txt);
        start = (Button) findViewById(R.id.btn_start);
        stop = (Button) findViewById(R.id.btn_stop);
        filename = (TextView) findViewById(R.id.tv_file_name);
        selectCompressed.setOnClickListener(this);
        start.setOnClickListener(this);
        selectText.setOnClickListener(this);
        stop.setOnClickListener(this);
    }

    // function to initialise graph
    public void setUpDrawWaveView() {
        waveView = new DrawWaveView(getApplicationContext());
        wave_layout.addView(waveView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        waveView.setValue(2048, 2048, -2048);
    }

    // function to draw graph as per new data
    public void updateWaveView(int data) {
        if (waveView != null) {
            waveView.updateData(data);
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult( Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();

                    // Get the path
                    String path = uri.getPath();
                    int index = path.indexOf(":");
                    String name = path.substring(index + 1);
                    filename.setText(name);


                    try {
                        File myFile = new File(Environment.getExternalStorageDirectory().toString() + "/" + name);
                        Log.v(TAG, "FILE Name : " + myFile.getName() + " " + myFile.getAbsolutePath());

                        if(fileType==COMPRESSED_FILE){
                            File decompressedFile = new File("/sdcard/D_" + name + ".txt" );
                            decompressedFile.createNewFile();

                            HuffmanCompression huffmanCompression = new HuffmanCompression();
                            huffmanCompression.decode(myFile, decompressedFile);

                            FileInputStream fis = null;
                            fis = new FileInputStream(decompressedFile);
                            InputStreamReader isr = new InputStreamReader(fis);
                            BufferedReader bufferedReader = new BufferedReader(isr);
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                sb.append(line);
                            }
                            Log.v(TAG, "FILE Com Content : " + sb.toString() );
                            fileContent = sb.toString();
                            fileSelected = true;
                        } else if ( fileType == TEXT_FILE ){
                            FileInputStream fis = null;
                            fis = new FileInputStream(myFile);
                            InputStreamReader isr = new InputStreamReader(fis);
                            BufferedReader bufferedReader = new BufferedReader(isr);
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                sb.append(line);
                            }
                            Log.v(TAG, "FILE Text Content : " + sb.toString() );
                            fileContent = sb.toString();
                            fileSelected = true;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    List<Integer> getRawData( String str ){
        Scanner scanner = new Scanner(str);
        List<Integer> list = new ArrayList<Integer>();
        while (scanner.hasNextInt()) {
            list.add(scanner.nextInt());
        }
        return list;
    }

    @Override
    public void onClick(View v) {
        if(v.equals(selectCompressed)){
            fileType = COMPRESSED_FILE;
            showFileChooser();
        } else if (v.equals(selectText)){
            fileType = TEXT_FILE;
            showFileChooser();
        } else if (v.equals(start)){
            if( !fileSelected ){
                Toast.makeText(getApplicationContext(), "Please Select a File " , Toast.LENGTH_SHORT ).show();
            } else {
                backgroundWork = new BackgroundWork();
                backgroundWork.execute(fileContent);
            }
        } else if(v.equals(stop)) {
            backgroundWork.cancel(true);
        }
    }

    public class BackgroundWork extends AsyncTask<String, Integer, Void >{

        @Override
        protected Void doInBackground(String... params) {
            List<Integer> list = getRawData(fileContent);
            for( int i=0 ; i<list.size() ; i++ ) {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                publishProgress(list.get(i));
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            updateWaveView(values[0]);
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled() {

            super.onCancelled();
        }
    }
}
