package com.example.beware;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;

public class TextActivity extends Activity {


    //Request code for the image picker
    public static final int pickPhotoTextRequestCode = 200;

    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;

    //ImageView element in the UI
    ImageView imageText;
    TextView textResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        Button button = findViewById(R.id.buttonPickImageText);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });
        imageText = findViewById(R.id.imagePhotoTextChosen);

        Button photoButton = (Button) this.findViewById(R.id.buttonTakePhoto);
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
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, pickPhotoTextRequestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == pickPhotoTextRequestCode && resultCode == Activity.RESULT_OK)
        {
            if(data == null)
                throw new IllegalStateException("The Intent is null");
            Bitmap bitmap = null;
            try {
                bitmap = getImageFromData(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageText.setImageBitmap(bitmap);
            processImageTagging(bitmap);
        }

            if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageText.setImageBitmap(photo);
                processImageTagging(photo);
            }


        super.onActivityResult(requestCode, resultCode, data);
    }

    private Bitmap getImageFromData(Intent data) throws IOException {
        Uri selectedImage = data.getData();
        return  MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
    }

    private void processImageTagging(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer textRecognizer =
                FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        textRecognizer.processImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
               textResult = findViewById(R.id.textRecognized);
               if(firebaseVisionText.getText().length() == 0){
                   textResult.setText("No text could be recognized");
               }else {
                   textResult.setText("Recognized text: \n  \n" + firebaseVisionText.getText());
               }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("Recognition problem");
                Context context = getApplicationContext();
                CharSequence text = "Hello toast!";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }





}
