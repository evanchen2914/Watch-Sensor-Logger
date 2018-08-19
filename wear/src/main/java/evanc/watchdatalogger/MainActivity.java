package evanc.watchdatalogger;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends WearableActivity implements
        DataClient.OnDataChangedListener {

    private final static String TAG = "Wear MainActivity";
    private TextView mTextView;
    Button mPrev, mNext, mSend, mReset;
    ToggleButton mToggle;
    int num = 0;
    String datapath = "/data_path";
    private DecimalFormat decimalFormat;
    boolean isRunning;
    private SensorManager sensorManager;
    long stopTime = 0;
    private List<Sensor> sensors;
    private Sensor sensor;
    private SensorEventListener listener;
    String dataMessage;
    LinkedHashMap dataMap;
    Chronometer mChron;
    MyService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataMap = new LinkedHashMap<String, String>();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //decimalFormat = new DecimalFormat("+@@;-@@"); // 2 significant figures
        decimalFormat = new DecimalFormat("+0.####;-0.####");

        //write out to the log all the sensors the device has.
        sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        mChron = findViewById(R.id.chronometer);
        mTextView = findViewById(R.id.text);
        mPrev = findViewById(R.id.prev);
        mNext = findViewById(R.id.next);
        mSend = findViewById(R.id.send);
        mReset = findViewById(R.id.reset);
        mToggle = findViewById(R.id.toggle);

        isRunning = false;
        mToggle.setChecked(isRunning);

        if (sensors.size() < 1) {
            makeToast("No sensors returned from sensor list");
            Log.wtf(TAG, "No sensors returned from getSensorList");
        }

        Sensor[] sensorArray = sensors.toArray(new Sensor[sensors.size()]);

        for (int i = 0; i < sensorArray.length; i++) {
            Log.wtf(TAG, "Found sensor " + i + " " + sensorArray[i].toString());
        }

        mPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeToast("Prev");
            }
        });

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeToast("Next");
            }
        });

        mToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    mChron.setBase(SystemClock.elapsedRealtime() + stopTime);
                    mChron.start();
                    isRunning = true;
                    startService();
                    onResume();
                    //Toast.makeText(getApplicationContext(), "On", Toast.LENGTH_SHORT).show();
                    //registerSensor();
                } else {
                    // The toggle is disabled
                    stopTime = mChron.getBase() - SystemClock.elapsedRealtime();
                    mChron.stop();
                    isRunning = false;
                    stopService();
                    onPause();
                    //Toast.makeText(getApplicationContext(), "Off", Toast.LENGTH_SHORT).show();
                    //unregisterSensor();
                }
            }
        });

        mReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataMap.clear();
                mChron.setBase(SystemClock.elapsedRealtime());
                stopTime = 0;
            }
        });

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendButton.setVisibility(View.GONE);
                //myButton.setEnabled(false);
                /*
                String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                String date = simpleDateFormat.format(new Date());
                String msg = dataMessage + "\n" + date;
                sendData(msg);
                */

                Set set = dataMap.entrySet();
                Iterator i = set.iterator();

                while (i.hasNext()) {
                    Map.Entry me = (Map.Entry) i.next();
                    sendData(me.getKey() + "\n" + me.getValue());
                }

                makeToast("Sent");

                /*String message = "Hello device " + num;
                sendData(message);
                num++;*/
            }
        });
        // Enables Always-on
        setAmbientEnabled();
    }

    public void startService() {
        startService(new Intent(this, MyService.class));
        //makeToast("Service Started");
    }

    public void stopService() {
        stopService(new Intent(this, MyService.class));
        //makeToast("Service Stopped");
    }

    //add listener.
    @Override
    public void onResume() {
        //Toast.makeText(getApplicationContext(), "Opened app", Toast.LENGTH_SHORT).show();
        super.onResume();
        registerSensor();
        Wearable.getDataClient(this).addListener(this);
    }

    //remove listener
    @Override
    public void onPause() {
        super.onPause();
        unregisterSensor();
        Wearable.getDataClient(this).removeListener(this);
        //Toast.makeText(getApplicationContext(), "Exited app", Toast.LENGTH_SHORT).show();
    }

    /**
     * Sends the data, note this is a broadcast, so we will get the message as well.
     */
    private void sendData(String message) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(datapath);
        dataMap.getDataMap().putString("message", message);
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        Task<DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);
        dataItemTask
                .addOnSuccessListener(new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        Log.d(TAG, "Sending message was successful: " + dataItem);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Sending message failed: " + e);
                    }
                })
        ;
    }

    void registerSensor() {
        //just in case
        if (sensorManager == null)
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //sensors = sensorManager.getSensorList(Sensor.TYPE_GAME_ROTATION_VECTOR);
        sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (sensors.size() > 0)
            sensor = sensors.get(0);

        listener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // I have no desire to deal with the accuracy events

            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                //just set the values to a textview so they can be displayed.
                if (isRunning) {
                    String msg = " x: " + getStrFromFloat(event.values[0]) +
                            "\n y: " + getStrFromFloat(event.values[1]) +
                            "\n z: " + getStrFromFloat(event.values[2]) + "\n"; //+
                    //"\n 3: " + String.valueOf(event.values[3]) +    //for the TYPE_ROTATION_VECTOR these 2 exist.
                    //"\n 4: " + String.valueOf(event.values[4]);
                    dataMessage = msg;
                    //String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
                    String pattern = "MM-dd-yyyy HH:mm:ss.SSS";
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                    String date = simpleDateFormat.format(new Date());

                    dataMap.put(date, dataMessage);
                    msg = date + "\n" + dataMessage;
                    mTextView.setText(msg);

                    //sendData(date + "\n" + dataMessage);
                }
            }
        };

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    void unregisterSensor() {
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener);
        }
        //clean up and release memory.
        sensorManager = null;
        listener = null;
    }

    public String getStrFromFloat(float in) {
        if ((in > -0.00001) && (in < 0.00001))
            in = 0;
        if (in == Math.rint(in))
            return Integer.toString((int) in);
        else
            return decimalFormat.format(in);
    }

    //receive data from the path.
    /* * * * * * * * * * * * * * * * NOT USED * * * * * * * * * * * * * * * */
    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        /*
        Log.d(TAG, "onDataChanged: " + dataEventBuffer);
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (datapath.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    String message = dataMapItem.getDataMap().getString("message");
                    Log.v(TAG, "Wear activity received message: " + message);
                    // Display message in UI
                    mTextView.setText(message);

                } else {
                    Log.e(TAG, "Unrecognized path: " + path);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.v(TAG, "Data deleted : " + event.getDataItem().toString());
            } else {
                Log.e(TAG, "Unknown data event Type = " + event.getType());
            }
        }*/
    }

    void makeToast(String str) {
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }
}
