/**
 * Create By Shawn.xiao at 2023/05/01
 */
package com.android.mycamera;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;

import android.util.Size;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import java.util.List;

public class CameraUtils {
    private static CameraUtils sInstance;

    private final String TAG = "CameraUtils";

    private final String BACK_CAMERA = "0";
    private final String FRONT_CAMERA = "1";

    public static CameraUtils getInstance() {
        if (sInstance == null) {
            synchronized (CameraUtils.class) {
                if (sInstance == null) {
                    sInstance = new CameraUtils();
                }
            }
        }
        return sInstance;
    }

    /**
     * 获取相机id
     */
    public String getCameraId() {
        //先写死返回后摄
        return BACK_CAMERA;
    }

    /**
     * 根据输出类获取指定相机的输出尺寸列表，降序排序
     *
     * @param cameraId 相机id
     * @param clz      输出类
     * @return
     */
    public List<Size> getCameraOutputSizes(CameraCharacteristics characteristics, String cameraId, Class clz) {
        StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        List<Size> sizes = Arrays.asList(configs.getOutputSizes(clz));
        Collections.sort(sizes, new Comparator<Size>() {
            @Override
            public int compare(Size s1, Size s2) {
                return s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight();
            }
        });
        Collections.reverse(sizes);

        return sizes;
    }
}
