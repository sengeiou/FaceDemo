package com.cw.facedemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

/**
 * @author gy.lin
 * @create 2018/8/13
 * @Describe
 */

public class IdentifyFromCamera extends Activity implements SurfaceHolder.Callback {


    private static final String TAG = "IdentifyFromCamera";

    private SurfaceView mSurfaceView;
    private Button mTemplateButton;
    private Button mIdentifyButton;
    private TextView mStatusTextView;

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private final int CAMERA_WIDTH = 640;
    private final int CAMERA_HEIGH = 480;
    private final int cameraId = 0;

    private boolean ADD_OPTION = false;
    private boolean IDENTIFY_OPTION = false;

    private byte[] mTemplate1 = null;
    private byte[] mTemplate2 = null;

    private int FACEID = 0;

    private Button camera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_from_camera);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initView();
    }


    private void initView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.identifySurfaceView);
        mTemplateButton = (Button) findViewById(R.id.AddtemplateButtonCamera);
        mIdentifyButton = (Button) findViewById(R.id.identifyButtonCamera);
        mStatusTextView = (TextView) findViewById(R.id.statusTextViewCamera);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        OpenCameraAndSetSurfaceviewSize(Camera.CameraInfo.CAMERA_FACING_BACK);

        mTemplateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ADD_OPTION = true;
                mTemplateButton.setEnabled(false);
                                mIdentifyButton.setEnabled(false);


            }
        });

        mIdentifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDENTIFY_OPTION = true;
                mIdentifyButton.setEnabled(false);
                                mTemplateButton.setEnabled(false);


            }
        });

        camera = findViewById(R.id.camera);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //默认后摄
                if (camera.getText().equals("前摄")) {
                    camera.setText("后摄");

                } else {
                    camera.setText("前摄");
                }
                try {
                    changeCamera();
                } catch (IOException e) {


                }
            }
        });

    }

    private Void OpenCameraAndSetSurfaceviewSize(int cameraId) {

        if (mCamera == null) {
            mCamera = openCamera(cameraId);
        }
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(CAMERA_WIDTH, CAMERA_HEIGH);
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        mCamera.setParameters(parameters);

        return null;
    }

    private Void SetAndStartPreview(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            mCamera.setPreviewCallback(new IdentifyPreview());
            mCamera.startPreview();
            //mCamera.cancelAutoFocus();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    private int currentCameraType = -1;//当前打开的摄像头标记
    private static final int BACK = 0;//前置摄像头标记
    private static final int FRONT = 1;//后置摄像头标记

    @SuppressLint("NewApi")
    private Camera openCamera(int type) {
        int frontIndex = -1;
        int backIndex = -1;
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int cameraIndex = 0; cameraIndex < cameraCount; cameraIndex++) {
            Camera.getCameraInfo(cameraIndex, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontIndex = cameraIndex;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backIndex = cameraIndex;
            }
        }

        currentCameraType = type;
        if (type == BACK && frontIndex != -1) {
            return Camera.open(backIndex);
        } else if (type == FRONT && backIndex != -1) {
            return Camera.open(frontIndex);
        }
        return null;
    }


    private void changeCamera() throws IOException {

        int cameraCount = Camera.getNumberOfCameras();
        if (cameraCount == 1) {
            Toast.makeText(this, "该设备只有一个摄像头!", Toast.LENGTH_SHORT).show();
            return;
        }
        releaseCamera();
        if (currentCameraType == FRONT) {
            mCamera = openCamera(BACK);
        } else if (currentCameraType == BACK) {
            mCamera = openCamera(FRONT);
        }
        cameraConfig(180);
    }

    private void cameraConfig(int de) {
        Log.i(TAG, "----degree----" + de);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(CAMERA_WIDTH, CAMERA_HEIGH);
        parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setPreviewCallback(new IdentifyPreview());
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(de);
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    /**
     * 释放mCamera
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
            mCamera = null;
        }
    }


    class IdentifyPreview implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            if (ADD_OPTION) {
                mTemplate1 = ZKLiveFaceManager.getInstance().getTemplateFromNV21(data, CAMERA_WIDTH, CAMERA_HEIGH);
                if (mTemplate1 == null) {
                    mStatusTextView.setText(getString(R.string.extract_template_fail));
                    return;
                }
                if (ZKLiveFaceManager.getInstance().dbAdd("faceID_" + FACEID, mTemplate1)) {
                    mStatusTextView.setText("" + getString(R.string.dbadd_template_success) + ",id=" + "faceID_" + FACEID);
                    FACEID++;
                    ADD_OPTION = false;

                    mTemplateButton.setEnabled(true);
                    mIdentifyButton.setEnabled(true);


                } else {
                    mStatusTextView.setText(getString(R.string.dbadd_template_fail));
                    mTemplateButton.setEnabled(false);
                    mIdentifyButton.setEnabled(false);

                }
            }
            if (IDENTIFY_OPTION) {
                mTemplate2 = ZKLiveFaceManager.getInstance().getTemplateFromNV21(data, CAMERA_WIDTH, CAMERA_HEIGH);
                if (mTemplate2 == null) {
                    mStatusTextView.setText(getString(R.string.extract_template_fail));
                    return;
                }
                String id = ZKLiveFaceManager.getInstance().identify(mTemplate2);
                if (TextUtils.isEmpty(id)) {
                    mStatusTextView.setText(getString(R.string.identify_fail));
                    mIdentifyButton.setEnabled(false);
                    mTemplateButton.setEnabled(false);


                } else {
                    mStatusTextView.setText("" + getString(R.string.identify_success) + ",id=" + id);
                    IDENTIFY_OPTION = false;
                    mIdentifyButton.setEnabled(true);
                    mTemplateButton.setEnabled(true);


                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            SetAndStartPreview(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTemplate1 = null;
        mTemplate2 = null;
    }
}
