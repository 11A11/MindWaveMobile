package com.neurosky.mindwavemobile;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Chirag on 07-04-2017.
 */
public class CompareAttention extends Activity implements View.OnClickListener {

    final int AT_FILE_1 = 0;
    final int AT_FILE_2 = 1;
    private static final int FILE_SELECT_CODE = 0;
    private static final String TAG = "MindWave Select File";

    boolean fileSelected = false;
    String fileContent = null, file2Content = null;
    int fileType;

    List<Integer> list, list2;

    Button atFile1, atFile2, comp;
    TextView fil1, fil2;
    BackgroundWork backgroundWork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attention_compare_view);
        initView();
//        setUpDrawWaveView();
    }

    // function to initialise view
    private void initView() {

        fil1 = (TextView) findViewById(R.id.tvFil1);
        fil2 = (TextView) findViewById(R.id.tvFil2);
        atFile1 = (Button) findViewById(R.id.btn_fil1);
        atFile2 = (Button) findViewById(R.id.btn_fil2);
        comp = (Button) findViewById(R.id.btn_cmp);
        atFile1.setOnClickListener(this);
        atFile2.setOnClickListener(this);
        comp.setOnClickListener(this);
    }

    // function to initialise graph
//    public void setUpDrawWaveView() {
//        waveView = new DrawWaveView(getApplicationContext());
//        wave_layout.addView(waveView, new ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        waveView.setValue(2048, 2048, -2048);
//    }
//
//    // function to draw graph as per new data
//    public void updateWaveView(int data) {
//        if (waveView != null) {
//            waveView.updateData(data);
//        }
//    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
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

                    try {
                        File myFile = new File(Environment.getExternalStorageDirectory().toString() + "/" + name);
                        Log.v(TAG, "FILE Name : " + myFile.getName() + " " + myFile.getAbsolutePath());

                        if (fileType == AT_FILE_1) {
                            fil1.setText(name);

                            FileInputStream fis = null;
                            fis = new FileInputStream(myFile);
                            InputStreamReader isr = new InputStreamReader(fis);
                            BufferedReader bufferedReader = new BufferedReader(isr);
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                sb.append(line);
                            }
                            Log.v(TAG, "FILE 1 Text Content : " + sb.toString());
                            fileContent = sb.toString();
                            Log.v(TAG, "fileContent : " + fileContent);
                            fileSelected = true;
                        } else if (fileType == AT_FILE_2) {
                            fil2.setText(name);

                            FileInputStream fis = null;
                            fis = new FileInputStream(myFile);
                            InputStreamReader isr = new InputStreamReader(fis);
                            BufferedReader bufferedReader = new BufferedReader(isr);
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                sb.append(line);
                            }
                            Log.v(TAG, "FILE 2 Text Content : " + sb.toString());
                            file2Content = sb.toString();
                            Log.v(TAG, "file2Content : " + file2Content);
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

    public List<Integer> getRawData(String str) {
        Scanner scanner = new Scanner(str);
        List<Integer> list = new ArrayList<Integer>();
        while (scanner.hasNextInt()) {
            list.add(scanner.nextInt());
        }
        return list;
    }

    @Override
    public void onClick(View v) {
        if (v.equals(atFile1)) {
            fileType = AT_FILE_1;
            showFileChooser();
        } else if (v.equals(atFile2)) {
            fileType = AT_FILE_2;
            showFileChooser();
            } else if (v.equals(comp)) {
            backgroundWork = new BackgroundWork();
            backgroundWork.execute(fileContent);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            backgroundWork = new BackgroundWork();
            backgroundWork.execute(file2Content);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            setUpDrawWaveView();
        }
    }

    public void setUpDrawWaveView() {
        GraphView graph = (GraphView) findViewById(R.id.graph);

        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);
        graph.getViewport().scrollToEnd();
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxY(100);
        graph.getViewport().setMaxX(20);


        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(generateData(fileContent));
        series.setColor(Color.RED);
        series.setThickness(3);
        graph.addSeries(series);
        LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>(generateData(file2Content));
        series2.setColor(Color.GREEN);
        series2.setThickness(3);
        graph.addSeries(series2);
    }

    public DataPoint[] generateData(String abc) {
        List<Integer> l1 = getRawData(abc);
        int sz = l1.size();
        DataPoint[] values = new DataPoint[sz];
        for (int i=0; i<sz; i++) {

            double x;

            if(i%2 == 0)
                x = i/2;
            else
                x = i/2 + 0.5;
            double y = l1.get(i);
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        Log.d("this is my array", "arr: " + Arrays.toString(values));
        return values;

    }

    public class BackgroundWork extends AsyncTask<String, Integer, Void> {

        @Override
        public Void doInBackground(String... params) {
            if (fileType == AT_FILE_1) {
                list = getRawData(fileContent);
                for (int i = 0; i < list.size(); i++) {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    publishProgress(list.get(i));
                }
            } else if (fileType == AT_FILE_2) {
                list2 = getRawData(file2Content);
                for (int i = 0; i < list2.size(); i++) {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    publishProgress(list2.get(i));
                }
            }
            return null;
        }
    }
}

