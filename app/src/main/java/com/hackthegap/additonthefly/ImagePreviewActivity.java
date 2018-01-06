package com.hackthegap.additonthefly;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class ImagePreviewActivity extends AppCompatActivity {
    static final String TAG = "imagePreviewActicity";
    private ImageView mImageView;
    private Uri mImagePath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        mImageView = (ImageView) findViewById(R.id.previewImageView);
        mImagePath = (Uri) getIntent().getParcelableExtra("com.hackthegap.additonthefly.previewImage");

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
                    detectedText += textBlock.getValue();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        textRecognizer.release();


//        setPic();
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = 400;//mImageView.getWidth();
        int targetH = 200;//mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mImagePath.toString(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mImagePath.toString(), bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

}
