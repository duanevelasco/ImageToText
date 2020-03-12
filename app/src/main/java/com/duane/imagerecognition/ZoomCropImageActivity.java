package com.duane.imagerecognition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.crazyorr.zoomcropimage.CropImageLayout;
import com.github.crazyorr.zoomcropimage.CropShape;

import java.io.File;
import java.io.FileOutputStream;

import static com.github.crazyorr.zoomcropimage.FileUtils.createFile;
import static com.github.crazyorr.zoomcropimage.FileUtils.isSdCardMounted;

public class ZoomCropImageActivity extends AppCompatActivity implements View.OnClickListener {
    /**
     * intent extra name : uri
     */
    public static final String INTENT_EXTRA_URI = "INTENT_EXTRA_URI";

    /**
     * intent extra name : outputWidth
     */
    public static final String INTENT_EXTRA_OUTPUT_WIDTH = "INTENT_EXTRA_OUTPUT_WIDTH";
    /**
     * intent extra name : outputHeight
     */
    public static final String INTENT_EXTRA_OUTPUT_HEIGHT = "INTENT_EXTRA_OUTPUT_HEIGHT";
    /**
     * intent extra name : cropShape
     */
    public static final String INTENT_EXTRA_CROP_SHAPE = "INTENT_EXTRA_CROP_SHAPE";
    /**
     * intent extra name : mDir
     */
    public static final String INTENT_EXTRA_SAVE_DIR = "INTENT_EXTRA_SAVE_DIR";
    /**
     * intent extra name : mFileName
     */
    public static final String INTENT_EXTRA_FILE_NAME = "INTENT_EXTRA_FILE_NAME";
    //crop status
    public static final int CROP_SUCCEEDED = 3;
    public static final int CROP_CANCELLED = 4;
    public static final int CROP_FAILED = 5;
    /**
     * default cropped image name
     */
    private static final String DEFAULT_CROPPED_IMAGE_NAME = "cropped_picture.png";
    /**
     * default cropped image width
     */
    private static final int DEFAULT_OUTPUT_WIDTH = 75;
    /**
     * default cropped image height
     */
    private static final int DEFAULT_OUTPUT_HEIGHT = 100;
    private CropImageLayout mCropImageLayout;

    private String mDir;
    private String mFileName;

    /**
     * get default directory to save cropped image
     *
     * @return
     */
    private static String getDefaultSaveDir() {
    String path;
    if (isSdCardMounted()) {
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator + BuildConfig.APPLICATION_ID;
    } else {
        throw new RuntimeException("No SD card is mounted.");
    }
    return path;
}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_zoom_crop_image);

    //hide actionbar
    if(getSupportActionBar() != null) {
        getSupportActionBar().hide();
    }

    mCropImageLayout = (CropImageLayout) findViewById(R.id.crop_overlay_layout);

    try {
        // Image Uri
        Uri uri = getIntent().getParcelableExtra(INTENT_EXTRA_URI);
        if (uri == null) {
            throw new NullPointerException("uri == null");
        } else {
            mCropImageLayout.setImageURI(uri);
        }

        // width
        int outputWidth = getIntent().getIntExtra(INTENT_EXTRA_OUTPUT_WIDTH,
                DEFAULT_OUTPUT_WIDTH);
        // height
        int outputHeight = getIntent().getIntExtra(INTENT_EXTRA_OUTPUT_HEIGHT,
                DEFAULT_OUTPUT_HEIGHT);
        mCropImageLayout.setOutputSize(outputWidth, outputHeight);

        // shape
        int notSpecified = -1;
        int cropShape = getIntent().getIntExtra(INTENT_EXTRA_CROP_SHAPE, notSpecified);
        if (cropShape != notSpecified) {
            mCropImageLayout.setCropShape(cropShape);
        }

        // directory
        mDir = getIntent().getStringExtra(INTENT_EXTRA_SAVE_DIR);
        if (mDir == null) {
            mDir = getDefaultSaveDir();
        }

        // file name
        mFileName = getIntent().getStringExtra(INTENT_EXTRA_FILE_NAME);
        if (mFileName == null) {
            mFileName = DEFAULT_CROPPED_IMAGE_NAME;
        }

        TextView btnCancel = (TextView) findViewById(R.id.tv_cancel);
        TextView btnUsePhoto = (TextView) findViewById(R.id.tv_use_photo);

        btnCancel.setOnClickListener(this);
        btnUsePhoto.setOnClickListener(this);

    } catch (Exception e) {
        Toast.makeText(this, R.string.error_photo_not_found, Toast.LENGTH_LONG).show();
        e.printStackTrace();
        setResult(CROP_FAILED);
        finish();
    }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            setResult(CROP_CANCELLED);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        Log.d("DUANE", "ONCLICK!!!");
        int i = v.getId();
        if (i == R.id.tv_cancel) {
            setResult(CROP_CANCELLED);
            finish();

        } else if (i == R.id.tv_use_photo) {
            mCropImageLayout.setCropShape(CropShape.SHAPE_RECTANGLE);
            Bitmap bitmap = mCropImageLayout.crop();
            try {
                File file = createFile(mDir, mFileName);
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();

                Uri uri = Uri.fromFile(file);
                Intent intent = new Intent();
                intent.putExtra(INTENT_EXTRA_URI, uri);
                setResult(CROP_SUCCEEDED, intent);

                Log.d("AndYou", "Result Code : " + CROP_SUCCEEDED);

            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
                setResult(CROP_FAILED);
            }
            finish();

        }
    }
}
