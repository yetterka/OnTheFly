package com.hackthegap.additonthefly;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ImagePreviewActivity extends AppCompatActivity {
    static final String TAG = "imagePreviewActivity";
    private ImageView mImageView;
    private EditText mDateField;
    private EditText mStartTime;
    private Uri mImagePath;
    private EditText mNameField;
    private Button mAddToCalendarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        mAddToCalendarButton = (Button) findViewById(R.id.addToCalendarButton);
        mImageView = (ImageView) findViewById(R.id.previewImageView);
        mDateField = (EditText) findViewById(R.id.dateEditText);
        mStartTime = (EditText) findViewById(R.id.timeEditText);
        mImagePath = (Uri) getIntent().getParcelableExtra("com.hackthegap.additonthefly.previewImage");
        mNameField = (EditText) findViewById(R.id.nameEditText);

        mAddToCalendarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, startTimeFixer() + " \n" + dateFixer());

                sendToServer(startTimeFixer(), endTimeFixer(), dateFixer(), mNameField.getText().toString());
            }
        });

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, "low_storage_error", Toast.LENGTH_LONG).show();
                Log.w(TAG, "low_storage_error");
            }
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImagePath);
            mImageView.setImageBitmap(bitmap);
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> text = textRecognizer.detect(frame);
            String detectedText = "";
            for (int i = 0; i < text.size(); i++) {
                TextBlock textBlock = text.valueAt(i);
                if (textBlock != null && textBlock.getValue() != null) {
                    Log.d("Processor", "Text detected! " + textBlock.getValue());
                    detectedText += textBlock.getValue() + " \n";
                }
            }

            Log.d("Processor", "Final String: " + detectedText);
            parseFlyer(detectedText);
        } catch (IOException e) {
            e.printStackTrace();
        }
        textRecognizer.release();
    }

    private void parseFlyer(String detectedText){
        int timeIndex = detectedText.indexOf(':');
        int dateIndex = detectedText.indexOf('/');
        int firstNew = detectedText.indexOf('\n');
        int nameIndex = detectedText.indexOf('\n', firstNew + 1);

        String time = "";
        String date = "";
        String name = "";
        if(timeIndex != -1)
            time = detectedText.substring(timeIndex - 2, timeIndex + 3).trim();
        if(dateIndex != -1)
            date = detectedText.substring(dateIndex - 2, dateIndex + 7).trim();
        if(nameIndex != -1)
            name = detectedText.substring(0, nameIndex).trim().replaceFirst("(\r\n|\r|\n)", " ");
        mDateField.setText(date);
        mStartTime.setText(time);
        mNameField.setText(name);
    }

    private void sendToServer(String startTime, String endTime, String date, String eventName) {
        String endpoint = "https://enigmatic-stream-81819.herokuapp.com/";
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject payload = new JSONObject();
        try {
            payload.put("eventName", eventName);
            payload.put("date", date); // Has to be in MM/dd/yyyy format
            payload.put("startTime", startTime); // Has to be in HH:MM
            payload.put("startap", "pm"); // Hardcoded by now, need to check for real
            payload.put("endTime", endTime); // Has to be in HH:MM
            payload.put("endap", "pm"); // Hardcoded by now, need to check for real
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, endpoint, payload, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "Got response " + response.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(ImagePreviewActivity.this)
                                .setTitle("Success!")
                                .setMessage("The event was successfully added to your calendar.")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        onBackPressed();
                                        Toast.makeText(ImagePreviewActivity.this, "Successfully sent to calendar", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .show();
                    }
                });
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error == null) return;
                if (error.networkResponse == null) return;

                Log.e(TAG, "error: " + error.toString());
                error.printStackTrace();
            }
        });

        queue.add(request);
    }

    private String dateFixer(){
        String dateA = mDateField.getText().toString();
        int iOfSlash = dateA.indexOf('/');
        int iOfSecSlash = dateA.indexOf('/', iOfSlash + 1);
        String day = dateA.substring(0, dateA.indexOf("/"));
        String dayDone;
        if (day.length() == 1) {
            dayDone = "0" + day;
        } else {
            dayDone = day;
        }
        String month = dateA.substring(iOfSlash + 1, dateA.indexOf("/", iOfSlash + 1));
        String monthDone;
        if (month.length() == 1) {
            monthDone = "0" + month;
        } else {
            monthDone = month;
        }
        String year = dateA.substring(dateA.length()-4);
        return year + "-" + dayDone + "-" + monthDone;
    }

    private String startTimeFixer(){
        String time = mStartTime.getText().toString();
        String hour = time.substring(0, time.indexOf(":"));
        String min = time.substring(time.indexOf(":") + 1);
        int start = Integer.parseInt(hour) + 12;
        return (Integer.toString(start) + ":" + min);
    }

    private String endTimeFixer(){
        String time = mStartTime.getText().toString();
        String hour = time.substring(0, time.indexOf(":"));
        String min = time.substring(time.indexOf(":") + 1);
        int start = Integer.parseInt(hour) + 13;
        return (Integer.toString(start) + ":" + min);
    }
}
