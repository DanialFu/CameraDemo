package com.danielfu.camerademo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView ivImage;
    private PopupWindow popWinChoose;

    private static final String TAG = "CameraDemo_MainActivity";

    private static final int REQUEST_OPEN_CAMERA = 0x011;
    private static final int REQUEST_OPEN_GALLERY = 0x022;
    private static final int REQUEST_CROP_PHOTO = 0x033;
    private static final int REQUEST_PERMISSIONS = 0x044;
    //原图像 路径
    private static String imgPathOri;
    //裁剪图像 路径
    private static String imgPathCrop;
    //原图像 URI
    private Uri imgUriOri;
    //裁剪图像 URI
    private Uri imgUriCrop;

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
                //注册相机权限
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
     * 7.0中如果需要调用系统(eg:裁剪)/其他应用，必须用FileProvider提供Content Uri，并且将Uri赋予读写的权限
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                imgUriOri = Uri.fromFile(oriPhotoFile);
            } else {
                imgUriOri = FileProvider.getUriForFile(this, getPackageName() + ".provider", oriPhotoFile);
            }
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUriOri);
            startActivityForResult(intent, REQUEST_OPEN_CAMERA);

            // 动态grant权限
            // 如果在xml中已经定义android:grantUriPermissions="true"
            // 则只需要intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);即可
            //            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            //            for (ResolveInfo resolveInfo : resInfoList) {
            //                String packageName = resolveInfo.activityInfo.packageName;
            //                grantUriPermission(packageName, imgUriOri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //            }
            Log.i(TAG, "openCamera_imgPathOri:" + imgPathOri);
            Log.i(TAG, "openCamera_imgUriOri:" + imgUriOri.toString());
        }
    }


    private void openGallery() {
        Intent intent = new Intent();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent.setAction(Intent.ACTION_GET_CONTENT);
//            intent.setAction(Intent.ACTION_PICK);
        }
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_OPEN_GALLERY);
    }


    /**
     * 裁剪图片
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
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                imgUriCrop = Uri.fromFile(cropPhotoFile);
//            }else{
//                imgUriCrop = FileProvider.getUriForFile(this, getPackageName() + ".provider", cropPhotoFile);
//            }

            //7.0 安全机制下不允许保存裁剪后的图片
            //所以仅仅将File Uri传入MediaStore.EXTRA_OUTPUT来保存裁剪后的图像
            imgUriCrop = Uri.fromFile(cropPhotoFile);

            intent.setDataAndType(uri, "image/*");
            intent.putExtra("crop", true);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("outputX", 300);
            intent.putExtra("outputY", 300);
            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUriCrop);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CROP_PHOTO);

            Log.i(TAG, "cropPhoto_imgPathCrop:" + imgPathCrop.toString());
            Log.i(TAG, "cropPhoto_imgUriCrop:" + imgUriCrop.toString());
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
                addPicToGallery(imgPathOri);
                cropPhoto(imgUriOri);
                Log.i(TAG, "openCameraResult_imgPathOri:" + imgPathOri);
                Log.i(TAG, "openCameraResult_imgUriOri:" + imgUriOri.toString());
                break;
            case REQUEST_OPEN_GALLERY:
                if (data != null) {
                    Uri imgUriSel = data.getData();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        //打开相册会返回一个经过图像选择器安全化的Uri，直接放入裁剪程序会不识别，抛出[暂不支持此类型：华为7.0]
                        //formatUri会返回根据Uri解析出的真实路径
                        String imgPathSel = UriUtils.formatUri(this, imgUriSel);
                        //根据真实路径转成File,然后通过应用程序重新安全化，再放入裁剪程序中才可以识别
                        cropPhoto(FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(imgPathSel)));
                        Log.i(TAG, "Kit_sel_path:" + imgPathSel);
                        Log.i(TAG, "Kit_sel_uri:" + Uri.fromFile(new File(imgPathSel)));
                    } else {
                        cropPhoto(imgUriSel);
                    }
                    Log.i(TAG, "openGalleryResult_imgUriSel:" + imgUriSel);
                }
                break;
            case REQUEST_CROP_PHOTO:
                addPicToGallery(imgPathCrop);
                ImageUtils.imageViewSetPic(ivImage, imgPathCrop);
                revokeUriPermission(imgUriCrop, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                Log.i(TAG, "cropPhotoResult_imgPathCrop:" + imgPathCrop);
                Log.i(TAG, "cropPhotoResult_imgUriCrop:" + imgUriCrop.toString());
                break;
        }
    }

    /**
     * 创建原图像保存的文件
     * @return
     * @throws IOException
     */
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

    /**
     * 创建裁剪图像保存的文件
     * @return
     * @throws IOException
     */
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
