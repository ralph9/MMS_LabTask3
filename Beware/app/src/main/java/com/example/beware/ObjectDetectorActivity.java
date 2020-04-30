package com.example.beware;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;

import java.io.IOException;
import java.util.List;

public class ObjectDetectorActivity extends Activity {

    //Request code for the image picker
    public static final int pickPhotoTextRequestCode = 200;

    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;

    //ImageView element in the UI
    ImageView imageToAnalyze;
    TextView textResult;


    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_detector);

        textResult = findViewById(R.id.textObjectsDetected);

        Button button = findViewById(R.id.buttonPickPhotoObject);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });
        imageToAnalyze = findViewById(R.id.imageObjectToDetect);

        Button photoButton = (Button) this.findViewById(R.id.buttonTakePhotoObject);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                } else {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == pickPhotoTextRequestCode && resultCode == Activity.RESULT_OK) {
            if (data == null)
                throw new IllegalStateException("The Intent is null");
            Bitmap bitmap = null;
            try {
                bitmap = getImageFromData(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageToAnalyze.setImageBitmap(bitmap);
            processImageTagging(bitmap);
        }

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageToAnalyze.setImageBitmap(photo);
            processImageTagging(photo);
        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    private Bitmap getImageFromData(Intent data) throws IOException {
        Uri selectedImage = data.getData();
        return MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
    }


    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, pickPhotoTextRequestCode);
    }

    private void processImageTagging(final Bitmap bitmap) {
        System.out.println("Starting Image processing");
        // Setting up the options for the object detector: with single image mode and classification
        // Multiple object detection in static images
        FirebaseVisionObjectDetectorOptions options =
                new FirebaseVisionObjectDetectorOptions.Builder()
                        .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // Optional
                        .build();


        //Generate image from the bitmap provided
        FirebaseVisionImage imageForDetector = FirebaseVisionImage.fromBitmap(bitmap);

        //Get instance of the object detector
        FirebaseVisionObjectDetector objectDetector =
                FirebaseVision.getInstance().getOnDeviceObjectDetector(options);

        //Call to the detector to process the image
        objectDetector.processImage(imageForDetector)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionObject>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionObject> detectedObjects) {
                                // Task completed successfully, objects have been recognized
                                System.out.println("some objects were detected: " + detectedObjects.size());
                                if (detectedObjects.size() == 0) {
                                    textResult.setText("No objects were detected");
                                } else {
                                    StringBuilder objectsDetected = new StringBuilder();
                                    for (FirebaseVisionObject obj : detectedObjects) {
                                        Integer id = obj.getTrackingId();
                                        Rect bounds = obj.getBoundingBox();
                                        // If classification was enabled:
                                        int categoryOfObject = obj.getClassificationCategory();
                                        Float confidenceOfObject = obj.getClassificationConfidence();
                                        objectsDetected.append("Object detected: " + " Cat: " + categoryOfObject + " Confidence: " + confidenceOfObject + "\n");
                                        System.out.println(obj.getBoundingBox().toString());


                                        Bitmap myBitmap = bitmap;
                                        Paint myPaint = new Paint();

                                        //We set the painting style in terms of color and width
                                        myPaint.setStyle(Paint.Style.STROKE);
                                        myPaint.setStrokeWidth(20);
                                        myPaint.setColor(Color.GREEN);

                                        //Coordinates of the rect, taken from the current object from
                                        //detected objects
                                        int x1 = obj.getBoundingBox().top;
                                        int y1 = obj.getBoundingBox().right;
                                        int x2 = obj.getBoundingBox().bottom;
                                        int y2 = obj.getBoundingBox().left;

                                        //Create a new image bitmap and attach a brand new canvas to it
                                        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
                                        Canvas tempCanvas = new Canvas(tempBitmap);

                                        //Draw the image bitmap into the canvas
                                        tempCanvas.drawBitmap(myBitmap, 0, 0, myPaint);

                                        //Draw everything else you want into the canvas, in this example a rectangle with rounded edges
                                        tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 10, 10, myPaint);

                                        //Attach the canvas to the ImageView
                                        imageToAnalyze.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));

                                    }
                                    System.out.println(objectsDetected.toString());

                                    //We display the object data retrieved
                                    textResult.setText(objectsDetected.toString());
                                    System.out.println("drawing done");

                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception, not objects were recognized
                                System.out.println("An exception was thrown: " + e.getMessage());
                            }
                        });

    }
}

