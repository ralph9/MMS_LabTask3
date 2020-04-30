package com.example.beware;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;

import java.util.List;

public class MainActivity extends Activity {

    //Request code for the image picker
    public static final int pickPhotoRequestCode = 200;

    private static final int CAMERA_REQUEST = 1888;

    //ImageView element in the UI
    private ImageView imageView;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
            processImageTagging(photo);
        }
        /*
        if (requestCode == pickPhotoRequestCode && resultCode == Activity.RESULT_OK)
        {
            if(data == null)
                throw new IllegalStateException("The Intent is null");
            Bitmap bitmap = null;
            try {
                bitmap = getImageFromData(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageView.setImageBitmap(bitmap);
        }
        */
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button photoButton = (Button) this.findViewById(R.id.buttonPickImage);
        photoButton.setOnClickListener(new View.OnClickListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v)
            {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        Button buttonTextRecognition = findViewById(R.id.buttonTextRecognition);
        buttonTextRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TextActivity.class);
                startActivity(intent);
            }
        });

        Button buttonObjectDetector = findViewById(R.id.buttonObjectDetector);
        buttonObjectDetector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ObjectDetectorActivity.class);
                startActivity(intent);
            }
        });

        imageView = findViewById(R.id.imagePhotoChosen);
    }

    /*
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, pickPhotoRequestCode);
    }

    private Bitmap getImageFromData(Intent data) throws IOException {
        Uri selectedImage = data.getData();
        return  MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
    }
    */


    private void processImageTagging(Bitmap bitmap) {
        FirebaseVisionImage visionImage = FirebaseVisionImage.fromBitmap(bitmap);
        Task<List<FirebaseVisionImageLabel>> labeler = FirebaseVision.getInstance()
                .getOnDeviceImageLabeler().processImage(visionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> firebaseVisionImageLabels) {
                            for(FirebaseVisionImageLabel label: firebaseVisionImageLabels){
                                float confidenceLevel = label.getConfidence();
                                TextView textForLabel = findViewById(R.id.textLabel);
                                textForLabel.setText("The image is labeled as " + label.getText() + " with a confidence of " +String.valueOf(confidenceLevel));
                            }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("terrible");
                    }
                });
    }
}
