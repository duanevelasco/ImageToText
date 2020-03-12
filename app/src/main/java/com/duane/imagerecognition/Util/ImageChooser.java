package com.duane.imagerecognition.Util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.duane.imagerecognition.R;
import com.github.crazyorr.zoomcropimage.CropImageLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ASUS on 4/24/2018.
 */

public class ImageChooser {

    public static final int INTENT_CODE_IMAGE_CHOOSER = 1;
    public static final int CAMERA_REQUEST  = 2;
    public static final int CROP_IMAGE  = 3;

    @SuppressLint("SdCardPath")
    public static final String fileName = "upload.jpg";
    private static final String uploadFile = "file:///sdcard/" + fileName;

    private Uri mPictureUri;

    private Activity mActivity;

    public ImageChooser(Activity activity) {
        mActivity = activity;
    }

    /**
     * ギャラリーを起動するIntentを返します
     * @return
     */
    public Intent getGalleryChooserIntent() {
        // ギャラリーから選択
        return new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI
        );
    }

    /**
     * カメラを起動するIntentを返す
     * @return
     */
    public Intent getCameraChooserIntent(){
        // カメラで撮影

        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intentCamera.resolveActivity(mActivity.getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    mPictureUri = Uri.fromFile(photoFile);
                    intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                }
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
        }
        return  intentCamera;
    }

    public Uri getUriFromIntent(Intent data) {
        Bundle extras = data.getExtras();
        Bitmap imageBitmap = (Bitmap) extras.get("data");
        return getImageUri(imageBitmap);
    }

    /**
     * カメラを起動するIntentを返す
     * @return
     */
    public Intent getCameraCaptureIntent(){
        // カメラで撮影
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uri  = Uri.parse(uploadFile);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
        return intent;
    }

    public Uri getCapturePictureUri() {
        File file = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
        return Uri.fromFile(file);
    }

    public Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(mActivity.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = mActivity.getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    /**
     * 画像保存先のファイルパスを作成します
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp;
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        File image = new File(storageDir, imageFileName + ".jpg");
        return image;
    }

    /**
     * 呼び出し元のonActivityResultメソッド中で、取得した画像のUriを返す処理です
     * @param requestCode
     * @param resultCode
     * @param data
     * @return 画像の取得に失敗した場合はnullを返します
     */
    public Uri getUriOnActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    mPictureUri = null;
                }
                return null;
            }

            // 画像を取得
            Uri imageUri = (data == null || data.getData() == null) ? mPictureUri : data.getData();

            if(mPictureUri != null){
                MediaScannerConnection.scanFile(
                        activity,
                        new String[]{mPictureUri.getPath()},
                        new String[]{"image/jpeg"},
                        null);
            }
            mPictureUri = null;

            return imageUri;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isFileValid(Uri uri) {
        try {
            CropImageLayout.rotaingBitmap(0, CropImageLayout.sampleBitmap(mActivity, uri, 1, 1));
        } catch (Exception e) {
            String warning = mActivity.getString(R.string.error_photo_not_found);
            Toast.makeText(mActivity, warning, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

}
