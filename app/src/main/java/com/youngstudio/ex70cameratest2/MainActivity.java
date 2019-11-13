package com.youngstudio.ex70cameratest2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ImageView iv;
    Button btn;

    //캡쳐한 이미지를 저장할 파일의 경로 Uri
    Uri imgUri=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv= findViewById(R.id.iv);
        btn= findViewById(R.id.btn);

        //카메라 앱에게 캡쳐한 사진을 저장하게
        //하려면 외부저장소의 읽고쓰기 권한을
        //부여해야함.
        //AndroidManifest.xml에 퍼미션 추가
        //마시멜로우 버전부터는 동적퍼미션을 요구
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            int permissionResult= checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if(permissionResult== PackageManager.PERMISSION_DENIED){
                //사용자에게 퍼미션을 요청하는 다이얼로그 보이기
                String[] permissions=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions,10);
            }
        }

    }

    //requestPermissions()메소드를 호출하여
    //사용자가 퍼미션허용 여부를 선택하면
    //자동으로 실행되는 콜백메소드
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case 10:
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){//허가 했다면?
                    Toast.makeText(this, "카메라 기능 사용 가능", Toast.LENGTH_SHORT).show();
                    btn.setEnabled(true);
                }else{//거부했다면
                    Toast.makeText(this, "카메라 기능 제한", Toast.LENGTH_SHORT).show();
                    btn.setEnabled(false);
                }

                break;
        }
    }

    public void clickPhoto(View view) {

        //카메라 앱 실행을 위한 Intent객체 생성
        Intent intent= new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //켭쳐한 이미지를 저장할 파일의 경로(File객체말고 Uri로)를
        //intent에게 추가데이터(Extra)로 보내줘야함.
        //imgUri에 경로를 설정하는 메소드
        setImageUri();

        if(imgUri!=null) intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);

        startActivityForResult(intent, 100);

    }

    //startActivityForResult()실행 후
    //결과를 받을 때 자동 실행되는 메소드
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 100:
                if(resultCode==RESULT_OK){


                    if(data!=null){
                        Uri uri=data.getData();
                        if(uri!=null) Glide.with(this).load(uri).into(iv);
                    }else{
                        //Bitmap으로 데이터 전달되었다면..
                        //우리가 지정한 imgUri의 경로에 사진이
                        //저장되어 있으니..
                        //Bitmap이미지 말고 imgUri의 사진을
                        //보여주도록.
                        if(imgUri!=null) Glide.with(this).load(imgUri).into(iv);

                        //이미지가 보인다면 파일로 저장이 잘 된 것임.
                        //근데 갤러리앱에서 목록으로 나오지 않음.
                        //그래서 갤러리앱에게 새로 추가된 사진을
                        //스캔하도록...방송하기!!!
                        Intent intent= new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(imgUri);
                        sendBroadcast(intent);


                    }

                }
                break;
        }
    }

    //imgUri객체를 만들어주는 메소드
    void setImageUri(){

        //외부 저장소에 저장할 것을 권장함.

        //이때 외부저장소의 2가지 영역 중 하나를 선택
        //1. 외부저장소의 앱 전용 영역 - 앱을 지우면 사진도 같이 지워짐
        File path= getExternalFilesDir("photo");//외부메모리의 본인 패키지명으로된 폴더가 생기고 그 안에 files폴더 안에 photo라는 이름으로 된 폴더경로를 지칭함
        if(!path.exists()) path.mkdirs();

        //2. 외부저장소의 공용영역 - 앱을 지워도 사진이 지워지지 않음.
        path= Environment.getExternalStorageDirectory();//외부 메모리의 최상위 폴더경로 [ storage/emulated/0/ 인 경우가 많음]
        path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        //경로를 결정했다면 저장할 파일명.jpg 지정

        //1)날짜를 이용해서 파일명 지정
        SimpleDateFormat sdf= new SimpleDateFormat("yyyyMMddhhmmss");
        String fileName= "IMG_"+sdf.format(new Date())+".jpg";
        File file= new File(path, fileName);

        //2)자동으로 임시파일명을 만들어주는 메소드
        try {
            file= File.createTempFile("IMG_", ".jpg", path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //일단 여기까지의 경로(file)가 잘되었는지 확인
        //new AlertDialog.Builder(this).setMessage(file.toString()).show();

        //위 File객체까지 잘되었다면...
        //File객체를 콘텐츠의 경로를 지칭하는 Uri객체로 변경해야 카메라 앱이 인식함.

        //File -> Uri
        //Nougat(누가버전)부터 경로이미지 노출이 위험하다고 판단되어 File->Uri로 변환할때 FileProvider가 필요함.
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N){
            imgUri= Uri.fromFile(file);
        }else{
            //누가버전 이후부터 Uri.formFile()은 에러남
            //다른 앱에게 파일의 접근을 허용해주도록 하는
            //Provider를 이용해야함. 그중에 FileProvider라는 것을 이용하도록...

            imgUri= FileProvider.getUriForFile(this, "com.mrhi.ex70cameratest2", file);
        }

        //잘 되었는지 확인
        //new AlertDialog.Builder(this).setMessage(imgUri.toString()).show();

    }

}



















