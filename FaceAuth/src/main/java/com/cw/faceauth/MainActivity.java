package com.cw.faceauth;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.zkteco.android.biometric.liveface56.ZKLiveFaceService;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {
    private AsyncTask<Void, Void, String> mAsyncTask;

    private TextView textView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.txtViewResult);
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1000);
        }
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1000);
        }
    }

    @Override
    public void onDestroy() {
        cancelTask();
        super.onDestroy();
    }

    private void cancelTask() {
        if (mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mAsyncTask.cancel(true);
        }

        mAsyncTask = null;
    }

    private String getLastError() {
        byte[] error = new byte[256];
        int[] retLen = new int[1];
        retLen[0] = 256;
        ZKLiveFaceService.getLastError(0, error, retLen);
        String errMsg = new String(error, 0, retLen[0]);
        if (null == errMsg) {
            errMsg = "";
        }
        return errMsg;
    }

    public void OnBnGetDevFP(final View view) {
        if (mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }

        mAsyncTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                view.setEnabled(false);
            }

            @Override
            protected String doInBackground(Void... voids) {
                if (ZKLiveFaceService.isAuthorized() || ZKLiveFaceService.getChipAuthStatus()) {
                    return "设备已激活或者有加密芯片!";
                }

                String strHwid = "";
                String strDevFP = "";
                byte[] hwid = new byte[256];
                int retLen[] = new int[1];
                retLen[0] = 256;

                int retVal = ZKLiveFaceService.getHardwareId(hwid, retLen);
                if (0 != retVal) {
                    return "获取机器码失败!";
                }

                strHwid = new String(hwid, 0, retLen[0]);
                byte[] bufDevFp = new byte[32 * 1024];
                retLen[0] = 32 * 1024;
                retVal = ZKLiveFaceService.getDeviceFingerprint(bufDevFp, retLen);
                if (0 != retVal) {
                    return "获取设备指纹信息失败!";
                }
                strDevFP = new String(bufDevFp, 0, retLen[0]);
                String fileName = "/sdcard/" + strHwid + ".txt";
                saveFile(fileName, strDevFP);

                return "设备机器码：" + strHwid + "\r\n请从" + fileName + "下载设备指纹信息文件，并发送给商务申请授权文件！";
            }

            @Override
            protected void onPostExecute(String message) {
                textView.setText(message);
                view.setEnabled(true);
            }
        }.execute((Void[]) null);
    }

    public void OnBnTestAuth(final View view) {
        if (mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }

        mAsyncTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                view.setEnabled(false);
            }

            @Override
            protected String doInBackground(Void... voids) {
                int retVal = 0;
                if (!ZKLiveFaceService.isAuthorized() && !ZKLiveFaceService.getChipAuthStatus())    //设备未激活或无加密芯片
                {
                    String strHwid = "";
                    byte[] hwid = new byte[256];
                    int retLen[] = new int[1];
                    retLen[0] = 256;

                    retVal = ZKLiveFaceService.getHardwareId(hwid, retLen);
                    if (0 != retVal) {
                        return "获取机器码失败!";
                    }
                    strHwid = new String(hwid, 0, retLen[0]);
                    String fileName = "/sdcard/" + strHwid + ".lic";
                    retVal = ZKLiveFaceService.setParameter(0, 1011, fileName.getBytes(), fileName.length());
                    if (0 != retVal) {
                        return "设置许可文件失败!";
                    }
                }

                long[] context = new long[1];
                retVal = ZKLiveFaceService.init(context);
                if (0 != retVal) {
                    return "激活失败, 返回值：" + retVal + ",错误码：" + getLastError();
                }

                ZKLiveFaceService.terminate(context[0]);

                return "激活成功!";
            }

            @Override
            protected void onPostExecute(String message) {
                textView.setText(message);
                view.setEnabled(true);
            }
        }.execute((Void[]) null);
    }

    public void OnBnCheckLic(View view) {
        if (ZKLiveFaceService.isAuthorized()) {
            textView.setText("设备已激活");
        } else {
            textView.setText("设备未激活");
        }
    }

    public void OnBnCheckIC(View view) {
        if (ZKLiveFaceService.getChipAuthStatus()) {
            textView.setText("设备有加密芯片");
        } else {
            textView.setText("设备无加密芯片");
        }
    }

    private void saveFile(String filename, String data) {
        FileOutputStream outputStream;

        try {
            outputStream = new FileOutputStream(filename);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readFile(String filename) {
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(filename);
            byte temp[] = new byte[1024];
            StringBuilder sb = new StringBuilder("");
            int len = 0;
            while ((len = inputStream.read(temp)) > 0) {
                sb.append(new String(temp, 0, len));
            }
            Log.d("msg", "readLicenseFile: \n" + sb.toString());
            inputStream.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
