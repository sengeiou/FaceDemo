package com.cw.facedemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author gy.lin
 * @create 2018/8/13
 * @Describe
 */

public class IdentifyFromFile extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_from_file);
        initView();
    }
    private ImageView mFirstImage;
    private Button mFirstButton;
    private TextView mFirstTextView;
    private byte[] FirstTemplate;
    private Bitmap mFirstBitmap;

    private ImageView mSecondImage;
    private Button mSecondButton;
    private TextView mSecondTextView;
    private byte[] SecondTemplate;
    private Bitmap mSecondBitmap;

    private Button mIdentifyButton;
    private TextView mIdentifyTextView;

    private final int FIRST_CODE = 1;
    private final int SECOND_CODE = 2;
    private void initView() {
        mFirstImage = (ImageView)findViewById(R.id.identifyFirstImage);
        mFirstButton = (Button)findViewById(R.id.identifyFirstButton);
        mFirstButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");//无类型限制
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, FIRST_CODE);
            }
        });
        mFirstTextView = (TextView)findViewById(R.id.identifyFirstTextView);

        mSecondImage = (ImageView)findViewById(R.id.identifySecondImage);
        mSecondButton = (Button)findViewById(R.id.identifySecondButton);
        mSecondButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");//无类型限制
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, SECOND_CODE);
            }
        });
        mSecondTextView = (TextView)findViewById(R.id.identifySecondTextView);

        mIdentifyButton = (Button)findViewById(R.id.identifyButton);
        mIdentifyTextView = (TextView)findViewById(R.id.identifyTextView);
        mIdentifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( SecondTemplate == null){
                    mIdentifyTextView.setText(getString(R.string.pls_choose_identify_file));
                    return;
                }
                String id = ZKLiveFaceManager.getInstance().identify(SecondTemplate);
                if(TextUtils.isEmpty(id)){
                    mIdentifyTextView.setText(getString(R.string.identify_fail));
                }else{
                    mIdentifyTextView.setText(""+getString(R.string.identify_success)+",id="+id);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = FileUtils.getPath(this,uri);
            if(FIRST_CODE == requestCode){
                if(!TextUtils.isEmpty(path)){
                    mFirstBitmap = BitmapFactory.decodeFile(path);
                    if(mFirstBitmap == null){
                        return;
                    }
                    mFirstImage.setImageBitmap(mFirstBitmap);
                    FirstTemplate = null;
                    FirstTemplate = ZKLiveFaceManager.getInstance().getTemplateFromBitmap(mFirstBitmap);
                    if(FirstTemplate == null){
                        mFirstTextView.setText(getString(R.string.extract_template_fail));
                        return;
                    }
                    if(ZKLiveFaceManager.getInstance().dbAdd(FileUtils.getFileName(path),FirstTemplate)){
                        mFirstTextView.setText(""+getString(R.string.dbadd_template_success)+",id="+FileUtils.getFileName(path));
                    }else{
                        mFirstTextView.setText(getString(R.string.dbadd_template_fail));
                    }
                }
            }else if(SECOND_CODE == requestCode){
                if(!TextUtils.isEmpty(path)){
                    mSecondBitmap = BitmapFactory.decodeFile(path);
                    if(mSecondBitmap == null){
                        return;
                    }
                    mSecondImage.setImageBitmap(mSecondBitmap);
                    SecondTemplate = null;
                    SecondTemplate = ZKLiveFaceManager.getInstance().getTemplateFromBitmap(mSecondBitmap);
                    if(SecondTemplate == null){
                        mSecondTextView.setText(getString(R.string.extract_template_fail));
                    }else{
                        mSecondTextView.setText(getString(R.string.extract_template_success));
                    }
                }
            }
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        FirstTemplate = null;
        SecondTemplate = null;
        if(mFirstBitmap != null){
            mFirstBitmap.recycle();
            mFirstBitmap = null;
        }
        if(mSecondBitmap != null){
            mSecondBitmap.recycle();
            mSecondBitmap = null;
        }
    }
}
