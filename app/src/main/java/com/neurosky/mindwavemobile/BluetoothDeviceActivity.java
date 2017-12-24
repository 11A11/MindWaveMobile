package com.neurosky.mindwavemobile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.DataType.MindDataType.FilterType;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Created by Mukesh on 11/3/2016.
 */
public class BluetoothDeviceActivity extends Activity {
	private static final String TAG = BluetoothDeviceActivity.class.getSimpleName();
	private TgStreamReader tgStreamReader;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private String address = null;

	private TextView tv_ps = null;
	private TextView tv_attention = null;
	private TextView tv_meditation = null;

	private Button btn_start = null;
	private Button btn_stop = null;
	private Button btn_selectdevice = null;
	private LinearLayout wave_layout;
	ArrayList<Integer> rawData = new ArrayList<>();
    ArrayList<Integer> atVal = new ArrayList<>();

    private int currentState = 0;
    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            // TODO Auto-generated method stub
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            currentState  = connectionStates;
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTED:
                    //sensor.start();
                    showToast("Connected", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_WORKING:
                    //byte[] cmd = new byte[1];
                    //cmd[0] = 's';
                    //tgStreamReader.sendCommandtoDevice(cmd);
                    LinkDetectedHandler.sendEmptyMessageDelayed(1234, 5000);
                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    //get data time out
                    break;
                case ConnectionStates.STATE_COMPLETE:
                    //read file complete
                    break;
                case ConnectionStates.STATE_STOPPED:
                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    break;
                case ConnectionStates.STATE_ERROR:
                    Log.d(TAG,"Connect error, Please try again!");
                    break;
                case ConnectionStates.STATE_FAILED:
                    Log.d(TAG,"Connect failed, Please try again!");
                    break;
            }

            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_STATE;
            msg.arg1 = connectionStates;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onRecordFail(int a) {
            // TODO Auto-generated method stub
            Log.e(TAG,"onRecordFail: " +a);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // TODO Auto-generated method stub

            badPacketCount ++;
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_BAD_PACKET;
            msg.arg1 = badPacketCount;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // TODO Auto-generated method stub
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = datatype;
            msg.arg1 = data;
            msg.obj = obj;
            LinkDetectedHandler.sendMessage(msg);
            //Log.i(TAG,"onDataReceived");
        }

    };

    DrawWaveView waveView = null;
    private boolean isPressing = false;
    private static final int MSG_UPDATE_BAD_PACKET = 1001;
    private static final int MSG_UPDATE_STATE = 1002;
    private static final int MSG_CONNECT = 1003;
    private boolean isReadFilter = false;

    int raw;

	private int badPacketCount = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.bluetoothdevice_view);

		initView();
		setUpDrawWaveView();

		try {
			// TODO	
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
				Toast.makeText(
						this,
						"Please enable your Bluetooth and again click this button!",
						Toast.LENGTH_LONG).show();
				finish();
//				return;
			}  
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "error:" + e.getMessage());
			return;
		}
	}


    // function to initialise view
	private void initView() {
		tv_ps = (TextView) findViewById(R.id.tv_ps);
		tv_attention = (TextView) findViewById(R.id.tv_attention);
		tv_meditation = (TextView) findViewById(R.id.tv_meditation);

		btn_start = (Button) findViewById(R.id.btn_start);
		btn_stop = (Button) findViewById(R.id.btn_stop);
		wave_layout = (LinearLayout) findViewById(R.id.wave_layout);
		
		btn_start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				badPacketCount = 0;
				showToast("connecting ...",Toast.LENGTH_SHORT);
				start();
			}
		});

		btn_stop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(tgStreamReader != null){
					tgStreamReader.stop();

                    try {
						SimpleDateFormat sDate = new SimpleDateFormat("ddMMyyyyhhmmss");
						String date = sDate.format(new Date());

                        File txtFile = new File("/sdcard/EEG Data " + date + ".txt" );
                        txtFile.createNewFile();
                        FileOutputStream fOut = new FileOutputStream(txtFile);
                        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                        for(int i=0; i<rawData.size(); i++){
                            myOutWriter.append(Integer.toString(rawData.get(i)) + " ");
                        }
                        myOutWriter.close();
                        fOut.close();

                        File compressedFile = new File("/sdcard/C_" + date );
                        compressedFile.createNewFile();
                        HuffmanCompression huffmanCompression = new HuffmanCompression();
                        huffmanCompression.encode(txtFile, compressedFile);

                        File atValFile = new File("/sdcard/AtVal " + date + ".txt" );
                        atValFile.createNewFile();
                        FileOutputStream fOut2 = new FileOutputStream(atValFile);
                        OutputStreamWriter myOutWriter2 = new OutputStreamWriter(fOut2);
                        for(int i=0; i<atVal.size(); i++){
                            myOutWriter.append(Integer.toString(atVal.get(i)) + " ");
                        }
                        myOutWriter2.close();
                        fOut2.close();


                        Toast.makeText(getBaseContext(), "Done Writing To File", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
						Log.v("Write Byte " , e.getMessage());
                        Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
			}

		});
		
		btn_selectdevice =  (Button) findViewById(R.id.btn_selectdevice);
		
		btn_selectdevice.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                scanDevice();
            }

        });
	}
	
	private void start(){
		if(address != null){
			BluetoothDevice bd = mBluetoothAdapter.getRemoteDevice(address);
			createStreamReader(bd);

			tgStreamReader.connectAndStart();
		}else{
			showToast("Please select device first!", Toast.LENGTH_SHORT);
		}
	}

	public void stop() { //update file here

		if(tgStreamReader != null){

			tgStreamReader.stop();
			tgStreamReader.close();//if there is not stop cmd, please call close() or the data will accumulate 
			tgStreamReader = null;

		}
	}

    // function to initialise graph
	public void setUpDrawWaveView() {
		
		waveView = new DrawWaveView(getApplicationContext());
		wave_layout.addView(waveView, new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		waveView.setValue(2048, 2048, -2048);
	}

    // function to draw graph as per new data
    public void updateWaveView(int data) {
		if (waveView != null) {
			waveView.updateData(data);
		}
	}

    // this function is used to process data read from the device
	private Handler LinkDetectedHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1234:
                    tgStreamReader.MWM15_getFilterType();
                    isReadFilter = true;
                    Log.d(TAG,"MWM15_getFilterType ");

                    break;
                case 1235:
                    tgStreamReader.MWM15_setFilterType(FilterType.FILTER_60HZ);
                    Log.d(TAG,"MWM15_setFilter  60HZ");
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;
                case 1236:
                    tgStreamReader.MWM15_setFilterType(FilterType.FILTER_50HZ);
                    Log.d(TAG,"MWM15_SetFilter 50HZ ");
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;

                case 1237:
                    tgStreamReader.MWM15_getFilterType();
                    Log.d(TAG,"MWM15_getFilterType ");
                    break;

                case MindDataType.CODE_FILTER_TYPE:
                    Log.d(TAG," CODE_FILTER_TYPE : " + msg.arg1 + " isReadFilter: " + isReadFilter);
                    if(isReadFilter){
                        isReadFilter = false;
                        if(msg.arg1 == FilterType.FILTER_50HZ.getValue()){
                            LinkDetectedHandler.sendEmptyMessageDelayed(1235, 1000);
                        }else if(msg.arg1 == FilterType.FILTER_60HZ.getValue()){
                            LinkDetectedHandler.sendEmptyMessageDelayed(1236, 1000);
                        }else{
                            Log.e(TAG,"Error filter type");
                        }
                    }
                    break;

                case MindDataType.CODE_RAW:
                    updateWaveView(msg.arg1);
                    rawData.add(msg.arg1);
//                        myOutWriter.append(Integer.toString(msg.arg1)+"\n");
                    break;
                case MindDataType.CODE_MEDITATION:
                    Log.d(TAG, "HeadDataType.CODE_MEDITATION " + msg.arg1);
                    tv_meditation.setText("" +msg.arg1 );
                    break;
                case MindDataType.CODE_ATTENTION:
                    Log.d(TAG, "CODE_ATTENTION " + msg.arg1);
                    tv_attention.setText("" +msg.arg1 );
                    atVal.add(msg.arg1);
                    break;
                case MindDataType.CODE_EEGPOWER:
                    EEGPower power = (EEGPower)msg.obj;
                    break;
                case MindDataType.CODE_POOR_SIGNAL://
                    int poorSignal = msg.arg1;
                    Log.d(TAG, "poorSignal:" + poorSignal);
                    tv_ps.setText(""+msg.arg1);
                    break;
                case MSG_UPDATE_BAD_PACKET:
                    break;
                default:
                    break;
            }
			super.handleMessage(msg);
		}
	};
	
	
	public void showToast(final String msg,final int timeStyle){
		BluetoothDeviceActivity.this.runOnUiThread(new Runnable()
        {    
            public void run()    
            {    
            	Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }    
    
        });  
	}
	
	//show device list while scanning
	private ListView list_select;
	private BTDeviceListAdapter deviceListApapter = null;
	private Dialog selectDialog;
	
	// Getting Bluetooth device dynamically
    public void scanDevice(){

    	if(mBluetoothAdapter.isDiscovering()){
    		mBluetoothAdapter.cancelDiscovery();
    	}
    	
    	setUpDeviceListView();
    	//register the receiver for scanning
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);
    	
    	mBluetoothAdapter.startDiscovery();
    }

    private void setUpDeviceListView(){
    	
    	LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_select_device, null);
		list_select = (ListView) view.findViewById(R.id.list_select);
		selectDialog = new Dialog(this, R.style.dialog1);
		selectDialog.setContentView(view);
    	//List device dialog

    	deviceListApapter = new BTDeviceListAdapter(this);
    	list_select.setAdapter(deviceListApapter);
    	list_select.setOnItemClickListener(selectDeviceItemClickListener);
    	
    	selectDialog.setOnCancelListener(new OnCancelListener(){

			@Override
			public void onCancel(DialogInterface arg0) {
				// TODO Auto-generated method stub
				Log.e(TAG,"onCancel called!");
				BluetoothDeviceActivity.this.unregisterReceiver(mReceiver);
			}

    	});
    	
    	selectDialog.show();
    	
    	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    	for(BluetoothDevice device: pairedDevices){
    		deviceListApapter.addDevice(device);
    	}
		deviceListApapter.notifyDataSetChanged();
    }
 
    //Select device operation
    private OnItemClickListener selectDeviceItemClickListener = new OnItemClickListener(){
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1,int arg2, long arg3) {
        Log.d(TAG, "Rico ####  list_select onItemClick     ");
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }
        //unregister receiver
        BluetoothDeviceActivity.this.unregisterReceiver(mReceiver);

        mBluetoothDevice =deviceListApapter.getDevice(arg2);
        selectDialog.dismiss();
        selectDialog = null;

        Log.d(TAG,"onItemClick name: "+mBluetoothDevice.getName() + " , address: " + mBluetoothDevice.getAddress() );
        address = mBluetoothDevice.getAddress().toString();

        //ger remote device
        BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mBluetoothDevice.getAddress().toString());

        tgStreamReader = createStreamReader(remoteDevice);
        tgStreamReader.connectAndStart();
		
		}
	
    };
 
     // If the TgStreamReader is created, just change the bluetooth else create TgStreamReader, set data receiver, TgStreamHandler and parser and return TgStreamReader
	public TgStreamReader createStreamReader(BluetoothDevice bd){

		if(tgStreamReader == null){
			// Example of constructor public TgStreamReader(BluetoothDevice mBluetoothDevice,TgStreamHandler tgStreamHandler)
			tgStreamReader = new TgStreamReader(bd,callback);
			tgStreamReader.startLog();
		}else{
			//If bluetooth deivce changed changeBluetoothDevice
			tgStreamReader.changeBluetoothDevice(bd);
			tgStreamReader.setTgStreamHandler(callback);
		}
		return tgStreamReader;
	}

    // this function bind device to app
    public void bindToDevice(BluetoothDevice bd){
 	    int ispaired = 0;
		if(bd.getBondState() != BluetoothDevice.BOND_BONDED){
			//ispaired = remoteDevice.createBond();
			try {
				//Set pin
				if(Utils.autoBond(bd.getClass(), bd, "0000")){
					ispaired += 1;
				}
				//bind to device
				if(Utils.createBond(bd.getClass(), bd)){
					ispaired += 2;
				}
				Method createCancelMethod=BluetoothDevice.class.getMethod("cancelBondProcess");
                boolean bool=(Boolean)createCancelMethod.invoke(bd);
                Log.d(TAG,"bool="+bool);
					
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.d(TAG, " paire device Exception:    " + e.toString());	
			}
		}
		Log.d(TAG, " ispaired:    " + ispaired);
    }
 
    //The BroadcastReceiver that listens for discovered devices
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
            Log.d(TAG, "mReceiver()");

        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG,"mReceiver found device: " + device.getName());

            // update to UI
            deviceListApapter.addDevice(device);
            deviceListApapter.notifyDataSetChanged();

        }
		}
	};

    @Override
    protected void onDestroy() {
        if(tgStreamReader != null){
            tgStreamReader.close();
            tgStreamReader = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }
}
