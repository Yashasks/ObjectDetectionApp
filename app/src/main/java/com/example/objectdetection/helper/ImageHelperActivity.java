package com.example.objectdetection.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.objectdetection.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

public class ImageHelperActivity extends AppCompatActivity {
    private ImageView inputImageView;
    private TextView outputTextView;
    private int REQUEST_PICK_IMAGE = 1000;
    private int REQUEST_CAPTURE_IMAGE = 1001;
    private ImageLabeler imageLabeler;
    private File photoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_helper);
        inputImageView = findViewById(R.id.imageViewInput);
        outputTextView = findViewById(R.id.textViewOutput);
        // initialize imageLabeler
        imageLabeler = ImageLabeling.getClient(new ImageLabelerOptions.Builder()
                                                .setConfidenceThreshold(0.7f)
                                                .build()
        );
        // check for storage permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(ImageHelperActivity.class.getSimpleName(), "grant result for"+permissions[0]+"is"+grantResults[0]);
    }

    public void onPickImage(View view){
        Intent intent = new Intent((Intent.ACTION_GET_CONTENT));
        intent.setType("image/*");

        startActivityForResult(intent, REQUEST_PICK_IMAGE);

    }

    public void onStartCamera(View view) {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create a File reference for future access
        photoFile = getPhotoFileUri(new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg");

        // wrap File object into a content provider
        // required for API >= 24
        Uri fileProvider = FileProvider.getUriForFile(this, "com.example.objectdetection.fileprovider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);

        if (intent.resolveActivity(getPackageManager()) != null) {
            // Start the image capture intent to take photo
            startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
        }
    }

    @SuppressLint("RestrictedApi")
    public File getPhotoFileUri(String fileName) {
        // Get safe storage directory for photos
        File mediaStorageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), LOG_TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.d(LOG_TAG, "failed to create directory");
        }

        // Return the file target for the photo based on filename
        File file = new File(mediaStorageDir.getPath() + File.separator + fileName);

        return file;
    }
    /**
     * getCapturedImage():
     *     Decodes and crops the captured image from camera.
     */
    private Bitmap getCapturedImage() {
        // Get the dimensions of the View
        int targetW = inputImageView.getWidth();
        int targetH = inputImageView.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.max(1, Math.min(photoW / targetW, photoH /targetH));

        bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inMutable = true;
        return BitmapFactory.decodeFile(photoFile.getAbsolutePath(), bmOptions);
    }

    private void rotateIfRequired(Bitmap bitmap) {
        try {
            ExifInterface exifInterface = new ExifInterface(photoFile.getAbsolutePath());
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
            );

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotateImage(bitmap, 90f);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotateImage(bitmap, 180f);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotateImage(bitmap, 270f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(
                source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true
        );
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK ){
            if(requestCode==REQUEST_PICK_IMAGE){
                Uri uri = data.getData();
                Bitmap bitmap = loadFromUri(uri);
                inputImageView.setImageBitmap(bitmap);
                runClassification(bitmap);

            }
            else if(requestCode==REQUEST_CAPTURE_IMAGE){
                Log.d("Ml", "recieved callback from camera");
                Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                inputImageView.setImageBitmap(bitmap);
                runClassification(bitmap);

            }
        }
    }
    // to convert Uri to Bitmap
    private Bitmap loadFromUri(Uri uri){
        Bitmap bitmap=null;
        try{
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1){
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            }else{
                // if version is < 27
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;

    }

   protected void runClassification(Bitmap bitmap){
        InputImage inputimage = InputImage.fromBitmap(bitmap, 0);
        // pass input image to ImageLabeler
        imageLabeler.process(inputimage).addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
            @Override
            public void onSuccess(@NonNull  List<ImageLabel> imageLabels) {
                if(imageLabels.size()>0){
                    StringBuilder builder= new StringBuilder();
                    for(ImageLabel label:imageLabels){
                        builder.append(label.getText())
                                .append(": ")
                                .append(label.getConfidence())
                                .append("\n");
                    }
                    outputTextView.setText(builder.toString());

                }else{
                    outputTextView.setText("Could not classify");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull  Exception e) {
                e.printStackTrace();
            }
        });
    }

    protected TextView getOutputTextView() {
        return outputTextView;
    }

    protected ImageView getInputImageView() {
        return inputImageView;
    }

}