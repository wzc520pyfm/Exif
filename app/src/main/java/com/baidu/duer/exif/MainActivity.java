package com.baidu.duer.exif;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.baidu.duer.exif.bean.Gps;
import com.baidu.duer.exif.utils.PositionUtil;
import com.bumptech.glide.Glide;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.model.TImage;
import com.jph.takephoto.model.TResult;
import com.jph.takephoto.model.TakePhotoOptions;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUIRadiusImageView2;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends TakePhotoActivity {

    @BindView(R.id.photoImageView)
    QMUIRadiusImageView2 photoImageView;
    @BindView(R.id.textView)
    TextView mText;

    private TakePhoto takePhoto;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View contentView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(contentView);
        ButterKnife.bind(this);
        takePhoto = getTakePhoto();
        configTakePhotoOption(takePhoto);
    }

    @OnClick({R.id.takePhotoBtn, R.id.selectPhotoBtn})
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.takePhotoBtn:
                Toast.makeText(this, "拍照", Toast.LENGTH_SHORT).show();
                imageUri = getImageCropUri();
                // 拍照不裁剪
                takePhoto.onPickFromCapture(imageUri);
                break;
            case R.id.selectPhotoBtn:
                Toast.makeText(this, "选照片", Toast.LENGTH_SHORT).show();
                imageUri = getImageCropUri();
                // 从相册选取不裁剪
                takePhoto.onPickFromGallery();
                break;
        }
    }

    @Override
    public void takeCancel() {
        super.takeCancel();
    }

    @Override
    public void takeFail(TResult result, String msg) {
        super.takeFail(result, msg);
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);
        showImg(result.getImage());
    }

    private void showImg(TImage image) {
        getInfo(image.getOriginalPath());
        photoImageView.setImageURI(Uri.fromFile(new File(image.getOriginalPath())));
        photoImageView.setBorderWidth(QMUIDisplayHelper.dp2px(this, 2));
        photoImageView.setCornerRadius(QMUIDisplayHelper.dp2px(this, 10));
        photoImageView.setSelectedBorderWidth(QMUIDisplayHelper.dp2px(this, 3));
        photoImageView.setTouchSelectModeEnabled(true);
        photoImageView.setCircle(false);
    }

    // 获取照片的输出报存Uri
    private Uri getImageCropUri() {
        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return Uri.fromFile(file);
    }

    private void configTakePhotoOption(TakePhoto takePhoto) {
        TakePhotoOptions.Builder builder = new TakePhotoOptions.Builder();
        takePhoto.setTakePhotoOptions(builder.create());
    }

    /**
     * @param path 图片路径
     */
    private void getInfo(String path) {
        try {

            ExifInterface exifInterface = new ExifInterface(path);

            String guangquan = exifInterface.getAttribute(ExifInterface.TAG_APERTURE);
            String shijain = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
            String baoguangshijian = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
            String jiaoju = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
            String chang = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            String kuan = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            String moshi = exifInterface.getAttribute(ExifInterface.TAG_MODEL);
            String zhizaoshang = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
            String iso = exifInterface.getAttribute(ExifInterface.TAG_ISO);
            String jiaodu = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            String baiph = exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
            String altitude_ref = exifInterface.getAttribute(ExifInterface
                    .TAG_GPS_ALTITUDE_REF);
            String altitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
            String latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String latitude_ref = exifInterface.getAttribute(ExifInterface
                    .TAG_GPS_LATITUDE_REF);
            String longitude_ref = exifInterface.getAttribute(ExifInterface
                    .TAG_GPS_LONGITUDE_REF);
            String longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String timestamp = exifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
            String processing_method = exifInterface.getAttribute(ExifInterface
                    .TAG_GPS_PROCESSING_METHOD);

            //转换经纬度格式
            double lat = score2dimensionality(latitude);
            double lon = score2dimensionality(longitude);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("光圈 = " + guangquan+"\n")
                    .append("时间 = " + shijain+"\n")
                    .append("曝光时长 = " + baoguangshijian+"\n")
                    .append("焦距 = " + jiaoju+"\n")
                    .append("长 = " + chang+"\n")
                    .append("宽 = " + kuan+"\n")
                    .append("型号 = " + moshi+"\n")
                    .append("制造商 = " + zhizaoshang+"\n")
                    .append("ISO = " + iso+"\n")
                    .append("角度 = " + jiaodu+"\n")
                    .append("白平衡 = " + baiph+"\n")
                    .append("海拔高度 = " + altitude_ref+"\n")
                    .append("GPS参考高度 = " + altitude+"\n")
                    .append("GPS时间戳 = " + timestamp+"\n")
                    .append("GPS定位类型 = " + processing_method+"\n")
                    .append("GPS参考经度 = " + latitude_ref+"\n")
                    .append("GPS参考纬度 = " + longitude_ref+"\n")
                    .append("GPS经度 = " + lon+"\n")
                    .append("GPS经度 = " + lat+"\n");

            //将获取的到的信息设置到TextView上
            mText.setText(stringBuilder.toString());

            /**
             * 将wgs坐标转换成火星坐标
             */
            Gps wgs2bd = PositionUtil.gps84_To_Gcj02(lat, lon);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 将 112/1,58/1,390971/10000 格式的经纬度转换成 112.99434397362694格式
     * @param string 度分秒
     * @return 度
     */
    private double score2dimensionality(String string) {
        double dimensionality = 0.0;
        if (null==string){
            return dimensionality;
        }

        //用 ，将数值分成3份
        String[] split = string.split(",");
        for (int i = 0; i < split.length; i++) {

            String[] s = split[i].split("/");
            //用112/1得到度分秒数值
            double v = Double.parseDouble(s[0]) / Double.parseDouble(s[1]);
            //将分秒分别除以60和3600得到度，并将度分秒相加
            dimensionality=dimensionality+v/Math.pow(60,i);
        }
        return dimensionality;
    }
}