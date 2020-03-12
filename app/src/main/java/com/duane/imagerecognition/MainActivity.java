package com.duane.imagerecognition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.duane.imagerecognition.Util.ImageChooser;
import com.github.crazyorr.zoomcropimage.CropShape;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Detector.Processor {

    /*Views*/
    private TextView tvLoadImage, tvText, tvCaptureImage;
    private ImageView ivImage;
    private SurfaceView surfaceView;
    private TextView tvCamera;

    /*Classes*/
    private TextRecognizer textRecognizer;
    private CameraSource mCameraSource;
    private ImageChooser mImageChooser;

    /*Variables*/
    private int clicked;
    private static final String TAG = "DEBUG TAG";
    private static final int RequestCodeWriteExternal = 100;
    private static final int  RequestCodeReadExternal = 101;
    private static final int RequestCodeCamera        = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvText = findViewById(R.id.tvText);
        tvLoadImage = findViewById(R.id.tvLoadImage);
        tvCamera = findViewById(R.id.tvCamera);
        ivImage = findViewById(R.id.ivImage);
        surfaceView = findViewById(R.id.surface_view);
        tvCaptureImage = findViewById(R.id.tvCapture);

        textRecognizer = new TextRecognizer.Builder(this).build();
        mImageChooser = new ImageChooser(this);
        tvText.setMovementMethod(new ScrollingMovementMethod());

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");
        }

        tvLoadImage.setOnClickListener(v -> {
            getRequest(0);
            clicked = 0;
        });

        tvCamera.setOnClickListener(v -> useCameraSource());

        tvCaptureImage.setOnClickListener(v -> {
            getRequest(1);
            clicked = 1;
        });
    }

    private void translateText(Frame imgFrame) {
        final SparseArray<TextBlock> items = textRecognizer.detect(imgFrame);
        if (items.size() != 0 ){

            tvText.post(() -> {
                StringBuilder stringBuilder = new StringBuilder();
                for(int i=0;i<items.size();i++){
                    TextBlock item = items.valueAt(i);
                    stringBuilder.append(item.getValue());
                    stringBuilder.append("\n");
                }
                tvText.setText(stringBuilder.toString());
            });
        }
    }

    private void getRequest(int clicked) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //request permission for writing on external storage
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodeWriteExternal);
        } else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //request permission for reading on external storage
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RequestCodeReadExternal);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, RequestCodeCamera);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)  {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, RequestCodeCamera);
        } else {
            if (clicked == 0) {
                startActivityForResult(mImageChooser.getGalleryChooserIntent(), ImageChooser.INTENT_CODE_IMAGE_CHOOSER);
            } else {
                if (mImageChooser == null) {
                    mImageChooser = new ImageChooser(this);
                }
                startActivityForResult(mImageChooser.getCameraCaptureIntent(), ImageChooser.CAMERA_REQUEST);
            }
        }
    }

    private void displayText(Bitmap bitmap) {
        Frame frame = new Frame.Builder()
                .setBitmap(bitmap)
                .build();

        translateText(frame);
    }

    private void cropImage(Uri result) {
        if (!mImageChooser.isFileValid(result)) {
            return;
        }

        // call up ZoomCropImageActivity
        Intent intent = new Intent(this, ZoomCropImageActivity.class);
        intent.putExtra(ZoomCropImageActivity.INTENT_EXTRA_URI, result);
        intent.putExtra(ZoomCropImageActivity.INTENT_EXTRA_OUTPUT_WIDTH, 1280);
        intent.putExtra(ZoomCropImageActivity.INTENT_EXTRA_OUTPUT_HEIGHT, 1024);
        intent.putExtra(ZoomCropImageActivity.INTENT_EXTRA_CROP_SHAPE, CropShape.SHAPE_RECTANGLE);   //optional
        intent.putExtra(ZoomCropImageActivity.INTENT_EXTRA_SAVE_DIR,
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + getPackageName());   //optional
        intent.putExtra(ZoomCropImageActivity.INTENT_EXTRA_FILE_NAME, "cropped.png");   //optional
        startActivityForResult(intent, ImageChooser.CROP_IMAGE);
    }

    private void useCameraSource() {
        mCameraSource = new CameraSource.Builder(this, textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setAutoFocusEnabled(true)
                .setRequestedFps(2.0f)
                .build();
        surfaceView.getHolder().addCallback(this);
        textRecognizer.setProcessor(this);
        surfaceView.setVisibility(View.VISIBLE);
        ivImage.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case RequestCodeWriteExternal:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    //request permission for writing on external storage
                    return;
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getRequest(clicked);
                }
                break;
            case RequestCodeReadExternal:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    //request permission for writing on external storage
                    return;
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getRequest(clicked);
                }
                break;
            case RequestCodeCamera:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    //request permission for writing on external storage
                    return;
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getRequest(clicked);
                }
                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK && resultCode != ImageChooser.CROP_IMAGE) {
            return;
        }

        switch (requestCode) {
            case ImageChooser.CAMERA_REQUEST:
                Uri imageUri = (data != null && data.getData() != null) ? data.getData() : mImageChooser.getCapturePictureUri();
                cropImage(imageUri);
                break;
            case ImageChooser.INTENT_CODE_IMAGE_CHOOSER:
                cropImage(data.getData());
                break;
            case ImageChooser.CROP_IMAGE:
                surfaceView.setVisibility(View.GONE);
                ivImage.setVisibility(View.VISIBLE);
                Uri croppedPictureUri = data.getParcelableExtra(ZoomCropImageActivity.INTENT_EXTRA_URI);
                try {
                    //Getting the Bitmap from Gallery
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), croppedPictureUri);
                    ivImage.setImageBitmap(bitmap);
                    displayText(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},1);
                return;
            }
            mCameraSource.start(surfaceView.getHolder());
            surfaceView.setVisibility(View.VISIBLE);
            ivImage.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCameraSource.stop();
    }

    @Override
    public void release() {

    }

    @Override
    public void receiveDetections(Detector.Detections detections) {
        SparseArray<TextBlock> items = detections.getDetectedItems();
        if (items.size() != 0 ){
            tvText.post(() -> {
                StringBuilder stringBuilder = new StringBuilder();
                for(int i=0;i<items.size();i++){
                    TextBlock item = items.valueAt(i);
                    stringBuilder.append(item.getValue());
                    stringBuilder.append("\n");
                }
                tvText.setText(stringBuilder.toString());
            });
        }
    }
}
