package com.danielfu.camerademo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView ivImage;
    private PopupWindow popWinChoose;

    private static final String TAG = "CameraDemo_MainActivity";

    private static final int REQUEST_OPEN_CAMERA = 0x011;
    private static final int REQUEST_OPEN_GALLERY = 0x022;
    private static final int REQUEST_CROP_PHOTO = 0x033;
    //原图像 路径
    private static String imgPathOri;
    //裁剪图像 路径
    private static String imgPathCrop;
    //原图像 路径
    private Uri imgUriOri;
    private Uri imgUriCrop;

    private static final int REQUEST_PERMISSIONS = 0x044;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();
        initViews();
        initChoosePop();
        ivImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popWinChoose.showAtLocation(MainActivity.this.findViewById(R.id.activity_main), Gravity.BOTTOM, 0, 0); // 在底部显示
                setWindowAlpha(0.5f); //Window设置为全透明
            }
        });
    }

    /**
     * 初始化相机相关权限
     * 适配6.0+手机的运行时权限
     */
    private void initPermission() {
        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        //检查权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //判断权限是否被拒绝过
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "用户曾拒绝打开相机权限", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        REQUEST_PERMISSIONS);

            }
        }


    }

    private void initViews() {
        ivImage = (ImageView) findViewById(R.id.iv_image);
    }

    private void initChoosePop() {
        View view = LayoutInflater.from(this).inflate(R.layout.popwin_sel, null);
        popWinChoose = new PopupWindow(view, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        popWinChoose.setFocusable(true); // 设置popWindow弹出窗体可点击
        popWinChoose.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
        popWinChoose.setAnimationStyle(R.style.storeImageChooseStyle);
        popWinChoose.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setWindowAlpha(1); //Window设置为全透明
            }
        });
        Button btnFirst = (Button) view.findViewById(R.id.btn_first);
        Button btnSecond = (Button) view.findViewById(R.id.btn_second);
        Button btnThird = (Button) view.findViewById(R.id.btn_third);
        btnFirst.setOnClickListener(this);
        btnSecond.setOnClickListener(this);
        btnThird.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_first:
                openCamera();
                break;
            case R.id.btn_second:
                openGallery();
                break;
            case R.id.btn_third:
                popWinChoose.dismiss();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //成功
                    Toast.makeText(this, "用户授权打开相机权限", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "用户拒绝打开相机权限", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    /**
     * 打开相机
     * 将相机获得的照片保存在SdCard中 路径为ImgPathOri
     * 注意：如果指定了MediaStore.EXTRA_OUTPUT，相机返回的data为空
     * <p>
     * 伪源码：
     * if (mSaveUri != null) { //存在mSaveUri，即指定了目标uri
     * outputStream.write(ContentResolver.openOutputStream(mSaveUri))
     * setResult(RESULT_OK);
     * }else{
     * Bitmap bitmap = createCaptureBitmap(data);
     * setResult(RESULT_OK, new Intent("inline-data").putExtra("data", bitmap));
     * }
     */
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 打开相机
        File oriPhotoFile = null;
        try {
            oriPhotoFile = createOriImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (oriPhotoFile != null) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                imgUriOri = Uri.fromFile(oriPhotoFile);
                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, imgUriOri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            } else {
                imgUriOri = FileProvider.getUriForFile(this, getPackageName() + ".provider", oriPhotoFile);
            }
            Log.i(TAG, "openCamera_imgPathOri:" + imgPathOri);
            Log.i(TAG, "openCamera_imgUriOri:" + imgUriOri.toString());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUriOri); //指定拍照后保存图片的URI
            startActivityForResult(intent, REQUEST_OPEN_CAMERA);
        }
    }

    /**
     * 打开相册
     * ACTION_GET_CONTENT 允许用户选择特定类型的数据并返回
     * Android 7.0 (API level 24) 或者更高的版本返回的Uri格式file:// URI，不同于之前的content:// URI
     * So，
     * 在获取Uri的时候需要用FileProvider来设置共享的目录和类型
     * <p>
     * http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2014/1026/1845.html
     */
    private void openGallery() {
        //        Intent intent = new Intent(ACTION_PICK, null);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_OPEN_GALLERY);
    }

    /**
     * 裁剪图片
     *
     * @param uri 需要 裁剪图像的Uri
     */
    public void cropPhoto(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        File cropPhotoFile = null;
        try {
            cropPhotoFile = createCropImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (cropPhotoFile != null) {
            //            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            //                imgUriCrop = Uri.fromFile(cropPhotoFile);
            //                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            //                for (ResolveInfo resolveInfo : resInfoList) {
            //                    String packageName = resolveInfo.activityInfo.packageName;
            //                    grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //                }
            //            }else{
            //                imgUriCrop = FileProvider.getUriForFile(this, getPackageName() + ".provider", cropPhotoFile);
            //            }
            imgUriCrop = Uri.fromFile(cropPhotoFile);

            Log.i(TAG, "CropPhoto_imgPathCrop:" + imgPathCrop.toString());
            Log.i(TAG, "CropPhoto_imgUriCrop:" + imgUriCrop.toString());
            intent.setDataAndType(uri, "image/*");
            intent.putExtra("crop", true);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("outputX", 300);
            intent.putExtra("outputY", 300);
            //            intent.putExtra("return-data", true);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUriCrop);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CROP_PHOTO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        //data的返回值根据
        switch (requestCode) {
            case REQUEST_OPEN_CAMERA:
                //                测试putExtra(MediaStore.EXTRA_OUTPUT)是否返回data
                //                if (data == null) {
                //                    Log.i("TAG_CAMERA", "Take pictures after the return data is empty");
                //                } else {
                //                    Log.i("TAG_CAMERA", data.getData().toString());
                //                }
                addPicToGallery(imgPathOri);
                cropPhoto(imgUriOri);
                Log.i(TAG, "openCameraResult_imgPathOri:" + imgPathOri);
                Log.i(TAG, "openCameraResult_imgUriOri:" + imgUriOri.toString());

                break;
            case REQUEST_OPEN_GALLERY:
                if (data != null) {
                    //获取到用户所选图片的Uri
                    Uri uriSelPhoto = data.getData();
                    cropPhoto(uriSelPhoto);
                }
                break;
            case REQUEST_CROP_PHOTO:
                addPicToGallery(imgPathCrop);
                imageViewSetPic(ivImage, imgPathCrop);
                //                revokeUriPermission(imgUriCrop, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Log.i(TAG, "openCameraResult_imgPathCrop:" + imgPathCrop);
                Log.i(TAG, "CropPhotoResult_imgUriCrop:" + imgUriCrop.toString());
                break;
        }
    }

    private File createOriImageFile() throws IOException {
        String imgNameOri = "HomePic_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File pictureDirOri = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/OriPicture");
        if (!pictureDirOri.exists()) {
            pictureDirOri.mkdirs();
        }
        File image = File.createTempFile(
                imgNameOri,         /* prefix */
                ".jpg",             /* suffix */
                pictureDirOri       /* directory */
        );
        imgPathOri = image.getAbsolutePath();
        return image;

    }

    private File createCropImageFile() throws IOException {
        String imgNameCrop = "HomePic_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File pictureDirCrop = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/CropPicture");
        if (!pictureDirCrop.exists()) {
            pictureDirCrop.mkdirs();
        }
        File image = File.createTempFile(
                imgNameCrop,         /* prefix */
                ".jpg",             /* suffix */
                pictureDirCrop      /* directory */
        );
        imgPathCrop = image.getAbsolutePath();
        return image;
    }

    /**
     * ImageView设置优化内存使用后的Bitmap
     * 返回一个等同于ImageView宽高的bitmap
     *
     * @param view    ImageView
     * @param imgPath 图像路径
     */
    private void imageViewSetPic(ImageView view, String imgPath) {
        // Get the dimensions of the View
        int targetW = view.getWidth();
        int targetH = view.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(imgPath, bmOptions);
        view.setImageBitmap(bitmap);
    }

    /**
     * 把图像添加进系统相册
     *
     * @param imgPath 图像路径
     */
    private void addPicToGallery(String imgPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imgPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    private void setWindowAlpha(float alpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = alpha;
        getWindow().setAttributes(lp);
    }


}
