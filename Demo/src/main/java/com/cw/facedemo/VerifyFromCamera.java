package com.cw.facedemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zkteco.android.graphics.ImageConverter;

import java.io.IOException;

/**
 * @author gy.lin
 * @create 2018/8/13
 * @Describe
 */

public class VerifyFromCamera extends Activity implements SurfaceHolder.Callback {

    private static final String TAG ="VerifyFromCamera";

    private SurfaceView mSurfaceView;
    private Button mTemplate1Button;
    private Button mTemplate2Button;
    private Button mVerifyButton;
    private TextView mStatusTextView;

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private final int CAMERA_WIDTH = 640;
    private final int CAMERA_HEIGH = 480;
    private final int cameraId = 0;

    private boolean FIRST_OPTION = false;
    private boolean SECOND_OPTION = false;

    private byte[] mTemplate1 = null;
    private byte[] mTemplate2 = null;

    private Button camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_from_camera);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initView();
    }

    private void initView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.verifySurfaceView);
        mTemplate1Button = (Button) findViewById(R.id.template1ButtonCamera);
        mTemplate2Button = (Button) findViewById(R.id.template2ButtonCamera);
        mVerifyButton = (Button) findViewById(R.id.verifyButtonCamera);
        mStatusTextView = (TextView) findViewById(R.id.statusTextCamera);
        mVerifyButton.setEnabled(false);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        OpenCameraAndSetSurfaceviewSize(Camera.CameraInfo.CAMERA_FACING_BACK);

        mTemplate1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FIRST_OPTION = true;
                mTemplate1Button.setEnabled(false);
                mTemplate2Button.setEnabled(false);
            }
        });
        mTemplate2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SECOND_OPTION = true;
                mTemplate1Button.setEnabled(false);
                mTemplate2Button.setEnabled(false);
            }
        });
        mVerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTemplate1 == null) {
                    mStatusTextView.setText(getString(R.string.pls_extract_template1));
                    return;
                }
                if (mTemplate2 == null) {
                    mStatusTextView.setText(getString(R.string.pls_extract_template2));
                    return;
                }
                int score = ZKLiveFaceManager.getInstance().verify(mTemplate1, mTemplate2);
                if (score >= ZKLiveFaceManager.getInstance().DEFAULT_VERIFY_SCORE) {
                    mStatusTextView.setText(getString(R.string.verify_success));
                } else {
                    mStatusTextView.setText(getString(R.string.verify_fail));
                }
                mTemplate1Button.setEnabled(true);
                mTemplate2Button.setEnabled(true);
            }
        });


        camera = findViewById(R.id.camera);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //默认后摄
                if (camera.getText().equals("前摄")) {
                    camera.setText("后摄");

                }else {
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
        mCamera.setPreviewCallback(new VerifyPreview());
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

    private Void SetAndStartPreview(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            mCamera.setPreviewCallback(new VerifyPreview());
            mCamera.startPreview();
            //mCamera.cancelAutoFocus();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private Void kill_camera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        return null;
    }

    class VerifyPreview implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            byte[] data2 = new byte[data.length];

            //ImageConverter.rotateNV21Degree90(data, data2, CAMERA_HEIGH, CAMERA_WIDTH);


            if (FIRST_OPTION) {
                mTemplate1 = ZKLiveFaceManager.getInstance().getTemplateFromNV21(data, CAMERA_WIDTH, CAMERA_HEIGH);
                if (mTemplate1 == null) {
                    mStatusTextView.setText(getString(R.string.extract_template1_fail));
                } else {
                    mStatusTextView.setText(getString(R.string.extract_template1_success));
                    FIRST_OPTION = false;
                    mTemplate1Button.setEnabled(false);
                    mTemplate2Button.setEnabled(true);

                }
            }

            if (SECOND_OPTION) {
                mTemplate2 = ZKLiveFaceManager.getInstance().getTemplateFromNV21(data, CAMERA_WIDTH, CAMERA_HEIGH);
                if (mTemplate2 == null) {
                    mStatusTextView.setText(getString(R.string.extract_template2_fail));
                } else {
                    mStatusTextView.setText(getString(R.string.extract_template2_success));
                    SECOND_OPTION = false;
                    mTemplate1Button.setEnabled(false);
                    mTemplate2Button.setEnabled(false);
                    mVerifyButton.setEnabled(true);
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
