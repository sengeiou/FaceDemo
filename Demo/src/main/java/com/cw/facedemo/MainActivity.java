package com.cw.facedemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;

public class MainActivity extends Activity {
    private final String TAG = MainActivity.class.getSimpleName();

    private TextView mHardwareId;
    private TextView mDevFpTextView;
    private Button mInitButtn;
    private TextView mInitTextView;
    private Button mVerifyButton;
    private Button mIdentifyButton;


    private Button mConfirmButton;

    private String FILE_PATH;
    private final int INIT_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        PermissionsUtil.verifyStoragePermissions(this);
        initView();
    }

    private void initView() {
        mInitButtn = (Button) findViewById(R.id.initButton);
        mHardwareId = (TextView) findViewById(R.id.hardwardId);
        mInitButtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ZKLiveFaceManager.getInstance().isAuthorized()) {
                    if (ZKLiveFaceManager.getInstance().setParameterAndInit("")) {
                        mInitTextView.setText(getString(R.string.init_algorithm_success));
                    } else {
                        mInitTextView.setText(getString(R.string.init_algorithm_fail));
                    }
                } else {
//                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                    intent.setType("*/*");
//                    intent.addCategory(Intent.CATEGORY_OPENABLE);
//                    startActivityForResult(intent, INIT_CODE);
                    ExFilePicker exFilePicker = new ExFilePicker();
                    exFilePicker.setQuitButtonEnabled(true);
                    exFilePicker.setStartDirectory(Environment.getExternalStorageDirectory().getPath());
                    exFilePicker.setChoiceType(ExFilePicker.ChoiceType.FILES);
                    exFilePicker.start(MainActivity.this, INIT_CODE);
                }
            }
        });


        mInitTextView = (TextView) findViewById(R.id.initTextView);

        mConfirmButton = (Button) findViewById(R.id.confirmButton);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ZKLiveFaceManager.getInstance().isAuthorized()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.unAuthorized), Toast.LENGTH_SHORT).show();
                    mHardwareId.setText("" + getString(R.string.hardware_id) + ZKLiveFaceManager.getInstance().getHardwareId());
                    String defaultDevFpFilePath = "/sdcard/zklivefacedevfp.txt";
                    FileUtils.writeFile(defaultDevFpFilePath, ZKLiveFaceManager.getInstance().getDeviceFingerprint());
                    mDevFpTextView = (TextView) findViewById(R.id.devFpTextView);
                    mDevFpTextView.setText("" + getString(R.string.dev_fp) + defaultDevFpFilePath);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.isAuthorized), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mVerifyButton = (Button) findViewById(R.id.verifyButton);
        mIdentifyButton = (Button) findViewById(R.id.identifyButton);
        mVerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ZKLiveFaceManager.getInstance().isInit()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.pls_init_algorithm), Toast.LENGTH_SHORT).show();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(getString(R.string.choose_method));
                builder.setPositiveButton(getString(R.string.from_local_file), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(MainActivity.this, VerifyFromFile.class));
                    }
                });
                builder.setNegativeButton(getString(R.string.from_camera), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(MainActivity.this, VerifyFromCamera.class));
                    }
                });
                builder.setNeutralButton(getString(R.string.ignore), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
            }
        });
        mIdentifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ZKLiveFaceManager.getInstance().isInit()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.pls_init_algorithm), Toast.LENGTH_SHORT).show();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(getString(R.string.choose_method));
                builder.setPositiveButton(getString(R.string.from_local_file), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(MainActivity.this, IdentifyFromFile.class));
                    }
                });
                builder.setNegativeButton(getString(R.string.from_camera), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(MainActivity.this, IdentifyFromCamera.class));
                    }
                });

                builder.setNeutralButton(getString(R.string.ignore), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (INIT_CODE == requestCode) {
                ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
                if (result == null || result.getCount() <= 0) {
                    mInitTextView.setText(getString(R.string.choose_lic_file_failed));
                    return;
                }

                String path = result.getPath();
                List<String> names = result.getNames();

                for (int i = 0; i < names.size(); i++) {
                    File f = new File(path, names.get(i));
                    FILE_PATH = f.getAbsolutePath();
                    break;
                }

                if (!TextUtils.isEmpty(FILE_PATH)) {
                    if (ZKLiveFaceManager.getInstance().setParameterAndInit(FILE_PATH)) {
                        mInitTextView.setText(getString(R.string.init_algorithm_success));
                    } else {
                        mInitTextView.setText(getString(R.string.init_algorithm_fail));
                    }
                } else {
                    mInitTextView.setText(getString(R.string.choose_lic_file_failed));
                }
                //System.out.println(ZKLiveFaceManager.getInstance().getDeviceFingerprint());
            }
        }
    }

}
