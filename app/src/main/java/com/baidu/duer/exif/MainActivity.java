package com.baidu.duer.exif;

import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.ServiceSettings;
import com.baidu.duer.exif.bean.Gps;
import com.baidu.duer.exif.utils.PositionUtil;
import com.bumptech.glide.Glide;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.model.TImage;
import com.jph.takephoto.model.TResult;
import com.jph.takephoto.model.TakePhotoOptions;
import com.qmuiteam.qmui.skin.QMUISkinHelper;
import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.skin.QMUISkinValueBuilder;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.util.QMUIResHelper;
import com.qmuiteam.qmui.widget.QMUIRadiusImageView2;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.qmuiteam.qmui.widget.popup.QMUIPopup;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends TakePhotoActivity {

    @BindView(R.id.map)
    MapView mapView;
    @BindView(R.id.photoImageView)
    QMUIRadiusImageView2 photoImageView;
    @BindView(R.id.textView)
    TextView mText;

    private QMUIPopup mNormalPopup;
    private TakePhoto takePhoto;
    private Uri imageUri;
    private ExifInterface exifInterface = null;
    private AMap aMap;
    private Marker marker;
    public Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View contentView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(contentView);
        ButterKnife.bind(this);
        mContext = this;
        takePhoto = getTakePhoto();
        configTakePhotoOption(takePhoto);

        // ?????????????????? END ??????5
        //??????????????????????????????(??????)
        ServiceSettings.updatePrivacyShow(this,true,true);
        ServiceSettings.updatePrivacyAgree(this,true);
        // ????????????
        mapView.onCreate(savedInstanceState);// ?????????????????????(????????????)
        aMap = mapView.getMap();
    }

    @OnClick({R.id.takePhotoBtn, R.id.selectPhotoBtn, R.id.tips, R.id.erasePhotoBtn})
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.takePhotoBtn:
                reset();
                Toast.makeText(this, "??????", Toast.LENGTH_SHORT).show();
                imageUri = getImageCropUri();
                // ???????????????
                takePhoto.onPickFromCapture(imageUri);
                break;
            case R.id.selectPhotoBtn:
                reset();
                Toast.makeText(this, "?????????", Toast.LENGTH_SHORT).show();
                imageUri = getImageCropUri();
                // ????????????????????????
                takePhoto.onPickFromGallery();
                break;
            case R.id.tips:
                TextView textView = new TextView(mContext);
                textView.setLineSpacing(QMUIDisplayHelper.dp2px(mContext, 4), 1.0f);
                int padding = QMUIDisplayHelper.dp2px(mContext, 20);
                textView.setPadding(padding, padding, padding, padding);
                textView.setText("?????????????????????????????????????????????!");
                textView.setTextColor(
                        QMUIResHelper.getAttrColor(mContext, R.attr.app_skin_common_title_text_color));
                QMUISkinValueBuilder builder = QMUISkinValueBuilder.acquire();
                builder.textColor(R.attr.app_skin_common_title_text_color);
                QMUISkinHelper.setSkinValue(textView, builder);
                builder.release();
                mNormalPopup = QMUIPopups.popup(mContext, QMUIDisplayHelper.dp2px(mContext, 250))
                        .preferredDirection(QMUIPopup.DIRECTION_BOTTOM)
                        .view(textView)
                        .skinManager(QMUISkinManager.defaultInstance(mContext))
                        .edgeProtection(QMUIDisplayHelper.dp2px(mContext, 20))
                        .dimAmount(0.6f)
                        .offsetX(QMUIDisplayHelper.dp2px(mContext, 20))
                        .offsetYIfBottom(QMUIDisplayHelper.dp2px(mContext, 5))
                        .shadow(true)
                        .arrow(true)
                        .animStyle(QMUIPopup.ANIM_GROW_FROM_CENTER)
                        .onDismiss(new PopupWindow.OnDismissListener() {
                            @Override
                            public void onDismiss() {
                                Toast.makeText(mContext, "onDismiss", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show(v);
                break;
            case R.id.erasePhotoBtn:
                if(exifInterface != null) {
                    eraseInfo(exifInterface);
                }
                break;
        }
    }

    private void reset() {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
        if(exifInterface != null) {
            exifInterface = null;
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

    /**
     * ????????????
     * @param image TImage??????,?????????????????????????????????path
     */
    private void showImg(TImage image) {
        getInfo(image.getOriginalPath());
        photoImageView.setImageURI(Uri.fromFile(new File(image.getOriginalPath())));
        photoImageView.setBorderWidth(QMUIDisplayHelper.dp2px(this, 2));
        photoImageView.setCornerRadius(QMUIDisplayHelper.dp2px(this, 10));
        photoImageView.setSelectedBorderWidth(QMUIDisplayHelper.dp2px(this, 3));
        photoImageView.setTouchSelectModeEnabled(true);
        photoImageView.setCircle(false);
    }

    /**
     * ???????????????????????????Uri
     * @return ????????????Uri
     */
    private Uri getImageCropUri() {
        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return Uri.fromFile(file);
    }

    /**
     * takePhoto??????
     * @param takePhoto takePhoto??????
     */
    private void configTakePhotoOption(TakePhoto takePhoto) {
        TakePhotoOptions.Builder builder = new TakePhotoOptions.Builder();
        takePhoto.setTakePhotoOptions(builder.create());
    }

    /**
     * @param path ????????????
     */
    private void getInfo(String path) {
        try {

            exifInterface = new ExifInterface(path);

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

            //?????????????????????
            double lat = score2dimensionality(latitude);
            double lon = score2dimensionality(longitude);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  " + "?????? = " + guangquan+"\n  ")
                    .append("?????? = " + shijain+"\n  ")
                    .append("???????????? = " + baoguangshijian+"\n  ")
                    .append("?????? = " + jiaoju+"\n  ")
                    .append("??? = " + chang+"\n  ")
                    .append("??? = " + kuan+"\n  ")
                    .append("?????? = " + moshi+"\n  ")
                    .append("????????? = " + zhizaoshang+"\n  ")
                    .append("ISO = " + iso+"\n  ")
                    .append("?????? = " + jiaodu+"\n  ")
                    .append("????????? = " + baiph+"\n  ")
                    .append("???????????? = " + altitude_ref+"\n  ")
                    .append("GPS???????????? = " + altitude+"\n  ")
                    .append("GPS????????? = " + timestamp+"\n  ")
                    .append("GPS???????????? = " + processing_method+"\n  ")
                    .append("GPS???????????? = " + latitude_ref+"\n  ")
                    .append("GPS???????????? = " + longitude_ref+"\n  ")
                    .append("GPS?????? = " + lon+"\n  ")
                    .append("GPS?????? = " + lat+"\n  ");

            //?????????????????????????????????TextView???
            mText.setText(stringBuilder.toString());

            if(longitude != null && latitude != null) {
                /**
                 * ???wgs???????????????????????????
                 */
                Gps gpsGc = PositionUtil.gps84_To_Gcj02(lat, lon);
                /**
                 * ??????????????????????????????????????????
                 */
                Gps gpsBd = PositionUtil.gcj02_To_Bd09(gpsGc.getWgLat(), gpsGc.getWgLon());
                /**
                 * ???????????????????????????????????????
                 */
                CoordinateConverter converter  = new CoordinateConverter(mContext);
                // CoordType.GPS ?????????????????????
                converter.from(CoordinateConverter.CoordType.BAIDU);
                // sourceLatLng?????????????????? LatLng??????
                converter.coord(new LatLng(gpsBd.getWgLat(), gpsBd.getWgLon()));
                // ??????????????????
                LatLng desLatLng = converter.convert();

                //??????????????????????????????
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(desLatLng));
                aMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                // ??????marker
                Marker marker = aMap.addMarker(new MarkerOptions().position(desLatLng));
            } else {
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(39.904989,116.405285)));
                aMap.moveCamera(CameraUpdateFactory.zoomTo(10));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * ??????????????????
     */
    private void eraseInfo(ExifInterface exifInterface) {
        exifInterface.setAttribute(ExifInterface.TAG_APERTURE, null);
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME, null);
        exifInterface.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, null);
        exifInterface.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, null);
        exifInterface.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, null);
        exifInterface.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, null);
        exifInterface.setAttribute(ExifInterface.TAG_MODEL, null);
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, null);
        exifInterface.setAttribute(ExifInterface.TAG_ISO, null);
        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, null);
        exifInterface.setAttribute(ExifInterface.TAG_WHITE_BALANCE, null);
        exifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null);
        exifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null);
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null);
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null);
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null);
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null);
        exifInterface.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null);
        exifInterface.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, null);
        try {
            exifInterface.saveAttributes();
            final QMUITipDialog tipDialog = new QMUITipDialog.Builder(mContext)
                    .setIconType(QMUITipDialog.Builder.ICON_TYPE_SUCCESS)
                    .setTipWord("????????????")
                    .create();
            tipDialog.show();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    tipDialog.dismiss();
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 1500);
        } catch (IOException e) {
            Log.e("Mine","cannot save exif",e);
        }

    }

    /**
     * ??? 112/1,58/1,390971/10000 ??????????????????????????? 112.99434397362694??????
     * @param string ?????????
     * @return ???
     */
    private double score2dimensionality(String string) {
        double dimensionality = 0.0;
        if (null==string){
            return dimensionality;
        }

        //??? ??????????????????3???
        String[] split = string.split(",");
        for (int i = 0; i < split.length; i++) {

            String[] s = split[i].split("/");
            //???112/1?????????????????????
            double v = Double.parseDouble(s[0]) / Double.parseDouble(s[1]);
            //?????????????????????60???3600?????????????????????????????????
            dimensionality=dimensionality+v/Math.pow(60,i);
        }
        return dimensionality;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //???activity??????onDestroy?????????mMapView.onDestroy()???????????????
        mapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //???activity??????onResume?????????mMapView.onResume ()???????????????????????????
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //???activity??????onPause?????????mMapView.onPause ()????????????????????????
        mapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //???activity??????onSaveInstanceState?????????mMapView.onSaveInstanceState (outState)??????????????????????????????
        mapView.onSaveInstanceState(outState);
    }
}