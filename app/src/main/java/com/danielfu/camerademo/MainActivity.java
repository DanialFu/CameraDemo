package com.danielfu.camerademo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ImageView ivImage;
    private PopupWindow popWinChoose;

    private static final int REQUEST_OPEN_CAMERA = 0x011;
    private static final int REQUEST_OPEN_GALLERY = 0x022;
    private static final int REQUEST_CROP_PHOTO = 0x033;

    //图像保存的 文件夹路径 .../CameraDemo/PictureDir
    private static String pictureDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CameraDemo/PictureDir";

    //拍照保存的原 图像路径 .../CameraDemo/PictureDir/Original/{time}.jpg
    private static String ImgPathOri;

    //裁剪保存的裁剪图 图像路径 .../CameraDemo/PictureDir/Crop/{time}.jpg
    private static String ImgPathCrop;

    private String ImgPathSel; //从相册选择的图片路径


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initDir();
        initChoosePop();
        ivImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popWinChoose.showAtLocation(MainActivity.this.findViewById(R.id.activity_main), Gravity.BOTTOM, 0, 0); // 在底部显示
                setWindowAlpha(0.5f); //Window设置为全透明
            }
        });
    }

    private void initDir() {
        File originalDirFile = new File(pictureDir+"/Original");
        File cropDirFile = new File(pictureDir+"/Crop");
        if(!originalDirFile.exists()){
            originalDirFile.mkdirs();
        }
        if(!cropDirFile.exists()){
            cropDirFile.mkdirs();
        }
    }


    private void initViews() {
        ivImage = (ImageView) findViewById(R.id.iv_image);
    }

    private void initChoosePop() {
        View view = LayoutInflater.from(this).inflate(R.layout.popwin_sel, null);
        Button btnFirst = (Button) view.findViewById(R.id.btn_first);
        Button btnSecond = (Button) view.findViewById(R.id.btn_second);
        Button btnThird = (Button) view.findViewById(R.id.btn_third);

        btnFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        btnSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        btnThird.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popWinChoose.dismiss();
            }
        });

        popWinChoose = new PopupWindow(view,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

        popWinChoose.setFocusable(true); // 设置popWindow弹出窗体可点击
        popWinChoose.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
        popWinChoose.setAnimationStyle(R.style.storeImageChooseStyle);

        popWinChoose.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setWindowAlpha(1); //Window设置为全透明
            }
        });
    }

    /**
     * 打开相机
     * 将相机获得的照片保存在SdCard中 路径为ImgPathOri
     * 注意：如果指定了MediaStore.EXTRA_OUTPUT，相机返回的data为空
     * <p>
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
        ImgPathOri = pictureDir + "/Original/" + new SimpleDateFormat("yyyyMMdd_hhmmss").format(new Date()) + ".jpg";

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 打开相机
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(ImgPathOri))); //指定拍照后保存图片的URI
        startActivityForResult(intent, REQUEST_OPEN_CAMERA);
    }

    /**
     * 打开相册
     * ACTION_PICK 从数据中选择一个项目，返回所选的内容。
     * ACTION_GET_CONTENT 允许用户选择特定类型的数据并返回
     * <p>
     * 两者相同点都是打开资源管理器，并通过setType()请求启动的内容选择器
     * 但不同的是ACTION_PICK返回的就是特性类型的内容，ACTION_GET_CONTENT返回的是泛指的
     * 比如：图片选择 ：ACTION_PICK会返回所有image ，ACTION_GET_CONTENT会返回所有类型为image的文件（其中包括url，或者其他类型）
     * 两者的差别在于返回的Uri的类型，ACTION_PICK返回的是路径类型的. ACTION_GET_CONTENT则是内容类型。(可使用ContentResolver.openInputStream(Uri)打开)
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
        ImgPathCrop = pictureDir + "/Crop/" + new SimpleDateFormat("yyyyMMdd_hhmmss").format(new Date()) + ".jpg";

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("return-data", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(ImgPathCrop)));
        startActivityForResult(intent, REQUEST_CROP_PHOTO);
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
                cropPhoto(Uri.fromFile(new File(ImgPathOri)));
                break;
            case REQUEST_OPEN_GALLERY:
                if (data == null) {
                    return;
                } else {
                    //获取到用户所选图片的Uri
                    Uri uriSelPhoto = data.getData();
                    cropPhoto(uriSelPhoto);
                }
                break;
            case REQUEST_CROP_PHOTO:
                Bitmap bitmap = BitmapFactory.decodeFile(ImgPathCrop);
                ivImage.setImageBitmap(bitmap);
                break;
        }
    }

    private void setWindowAlpha(float alpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = alpha;
        getWindow().setAttributes(lp);
    }


}
