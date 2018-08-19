package evanc.watchdatalogger;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements
        DataClient.OnDataChangedListener {

    private static final String LOG_TAG_EXTERNAL_STORAGE = "EXTERNAL_STORAGE";
    private static final int WRITE_PERMISSION = 1;

    String datapath = "/data_path";
    Button clearButton, sendButton;
    TextView mData;
    TextView count;
    String TAG = "Mobile MainActivity";
    int num = 0;
    ArrayList<String> sensorData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorData = new ArrayList<>();
        mData = findViewById(R.id.logger);
        count = findViewById(R.id.count);
        clearButton = findViewById(R.id.clearbtn);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Hello wearable " + num;
                //Requires a new thread to avoid blocking the UI
                //sendData(message);
                mData.setText("Output:");
                num = 0;
                count.setText("Count: " + num);
            }
        });

        sendButton = findViewById(R.id.send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (StorageUtil.isMounted()) {
                        // Check whether this app has write external storage permission or not.
                        int storagePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        // If do not grant write external storage permission.
                        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                            // Request user to grant write external storage permission.
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
                        } else {
                            String formattedPattern = "MM-dd-yyyy HH:mm:ss.SSS";
                            String unformattedPattern = "MMddyyyyHHmmssSSS";

                            SimpleDateFormat sdf = new SimpleDateFormat(unformattedPattern);
                            String date = sdf.format(new Date());

                            // Save email_public.txt file to /storage/emulated/0/DCIM folder
                            String dcimPath = StorageUtil.getDirectory(Environment.DIRECTORY_DOCUMENTS);

                            // Name text file
                            File newFile = new File(dcimPath, "accelerometer_" + date + ".csv");

                            FileWriter fw = new FileWriter(newFile);
                            sdf.applyPattern(formattedPattern);
                            //fw.write(edit.getText().toString());

                            //fw.write(date + "\n" + edit.getText().toString());
                            fw.append("Count: " + num + "\n");
                            fw.append("Timestamp,X,Y,Z\n");
                            for (int i = 0; i < sensorData.size(); i ++) {
                                String lines[] = sensorData.get(i).split("\\r?\\n");
                                fw.append(lines[0] + "," +
                                          lines[1].substring(3) + "," +
                                          lines[2].substring(3) + "," +
                                          lines[3].substring(3) + "\n");
                            }

                            fw.write("");

                            fw.flush();

                            fw.close();

                            Toast.makeText(getApplicationContext(),
                                    "Saved to Documents", Toast.LENGTH_LONG).show();
                        }
                    }

                } catch (Exception ex) {
                    Log.e(LOG_TAG_EXTERNAL_STORAGE, ex.getMessage(), ex);

                    Toast.makeText(getApplicationContext(),
                            "Save to public external storage failed. Error message is " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // add data listener
    @Override
    public void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
    }

    //remove data listener
    @Override
    public void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
    }

    /**
     * simple method to add the log TextView.
     */
    public void logthis(String newinfo) {
        //if (newinfo.compareTo("") != 0) {}
        mData.append(newinfo + "\n");
        sensorData.add(newinfo);
        num++;
        count.setText("Count: " + num);
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged: " + dataEventBuffer);
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (datapath.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    String message = dataMapItem.getDataMap().getString("message");
                    Log.v(TAG, "Wear activity received message: " + message);
                    // Display message in UI
                    logthis(message);
                } else {
                    Log.e(TAG, "Unrecognized path: " + path);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.v(TAG, "Data deleted : " + event.getDataItem().toString());
            } else {
                Log.e(TAG, "Unknown data event Type = " + event.getType());
            }
        }
    }

    // This method is invoked after user click buttons in permission grant popup dialog.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_PERMISSION) {
            int grantResultsLength = grantResults.length;
            if (grantResultsLength > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "You grant write external storage permission. Please click original button again to continue.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "You denied write external storage permission.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Sends the data.  Since it specify a client, everyone who is listening to the path, will
     * get the data.
     */
    /*private void sendData(String message) {
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
    }*/
}
