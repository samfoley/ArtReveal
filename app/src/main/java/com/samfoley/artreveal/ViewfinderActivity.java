package com.samfoley.artreveal;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.samfoley.artreveal.R;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ViewfinderActivity extends AppCompatActivity {
    private static final String TAG = "ViewfinderActivity";
    private static final int MESSAGE_IMAGE_READY = 1;


    private ImageView mPreviewView;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Camera mCamera;
    private ImageReader mPreviewReader;
    private Bitmap mPreviewBitmap;
    private ColorMatrixColorFilter mFilter;
    private ColorMatrix mMatrix;
    private Handler mUiHandler;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_viewfinder);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton captureFab = findViewById(R.id.capture);
        captureFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    if(mPreviewBitmap != null)
                    {
                        File captureDir = new File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ArtReveal");

                        ActivityCompat.requestPermissions(ViewfinderActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                1);

                        try {
                            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                                captureDir.mkdirs();
                                File captureFile = new File(captureDir, "test.jpg");
                                captureFile.createNewFile();
                                FileOutputStream outputStream = new FileOutputStream(captureFile);
                                Bitmap bitmap = Bitmap.createBitmap(mPreviewBitmap.getWidth(), mPreviewBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                                Canvas canvas = new Canvas(bitmap);
                                Paint paint = new Paint();
                                paint.setColorFilter(mFilter);
                                canvas.drawBitmap(mPreviewBitmap,0,0,paint);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                                outputStream.close();
                                Snackbar.make(view, "Captured photo", Snackbar.LENGTH_LONG)
                                        .setAction("View", null).show();
                            } else {
                                Snackbar.make(view, "Storage not available", Snackbar.LENGTH_LONG)
                                        .setAction("", null).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }


            }
        });

        if(OpenCVLoader.initDebug()){
            Log.v("TAG", "openCv loaded");
        }else{
            Toast.makeText(this, "openCv cannot be loaded", Toast.LENGTH_SHORT).show();
        }
        mPreviewView = findViewById(R.id.previewView);
        mUiHandler = new Handler(Looper.getMainLooper());
        mCamera = new Camera("0");
        startBackgroundThread();
        setupCamera(640, 480);
    }

    public void onResume()
    {
        super.onResume();
        if(mBackgroundHandler == null)
        {
            startBackgroundThread();
            setupCamera(640, 480);
        }
    }

    public void onPause()
    {

        //mCamera.close();

        //stopBackgroundThread();
        Log.v("TAG", "onPause");
        super.onPause();
    }

    public void onStop()
    {
        mCamera.close();
        stopBackgroundThread();
        Log.v("TAG", "onStop");
        super.onStop();
    }



    private void startBackgroundThread() {

        mBackgroundThread = new HandlerThread("ViewfinderBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void setupCamera(final int width, final int height)
    {
        // Get camera permissions
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.CAMERA  },1);
        }
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);


        mCamera.openPreview(this, mBackgroundHandler, new PreviewListener() {
            @Override
            public void onPreview(Bitmap preview) {
                Log.v(TAG,"Got image");
                mPreviewBitmap = preview;

                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFilter = new ColorMatrixColorFilter(
                                Transformations.getDecorrelationMatrix(mPreviewBitmap));
                    }
                });

                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Matrix matrix = new Matrix();
                        matrix.setRotate(90.0f);
                        mPreviewView.setImageBitmap(Bitmap.createBitmap(
                                mPreviewBitmap, 0, 0,
                                mPreviewBitmap.getWidth(), mPreviewBitmap.getHeight(),
                                matrix, true));
                        if(mFilter !=null) mPreviewView.setColorFilter(mFilter);
                    }
                });
            }
        });

    }

}
