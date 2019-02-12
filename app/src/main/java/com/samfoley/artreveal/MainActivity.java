package com.samfoley.artreveal;



import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PHOTO_FOR_AVATAR = 0;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImage();
            }
        });


        if(OpenCVLoader.initDebug()){
            Toast.makeText(this, "openCv successfully loaded", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "openCv cannot be loaded", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_camera) {
            Intent cameraActivity = new Intent(this, CameraActivity.class);
            startActivity(cameraActivity);
            return true;
        }
        if (id == R.id.action_viewfinder) {
            Intent viewfinderActivity = new Intent(this, ViewfinderActivity.class);
            startActivity(viewfinderActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PHOTO_FOR_AVATAR);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO_FOR_AVATAR && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }
            try {
                ImageView imageView = findViewById(R.id.imageView);

                RequestListener listener = new RequestListener<BitmapDrawable>()
                {

                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<BitmapDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(BitmapDrawable resource, Object model, Target<BitmapDrawable> target, DataSource dataSource, boolean isFirstResource) {
                        Toast.makeText(MainActivity.super.getBaseContext(), "image loaded", Toast.LENGTH_SHORT).show();
                        Bitmap photo = resource.getBitmap();
                        decorrstretch(photo);
                        ImageView imageView = findViewById(R.id.imageView);
                        imageView.setImageBitmap(photo);
                        return false;
                    }
                };

                Glide.with(this).load(data.getData()).addListener(listener).into(imageView);
            } catch (Exception e)
            {
                Log.v(TAG, e.getMessage());
            }

            //Now you can do whatever you want with your inpustream, save it as file, upload to a server, decode a bitmap...
        }
    }

    private void decorrstretch(Bitmap photo)
    {
        Mat src = new Mat();
        Utils.bitmapToMat(photo, src);
        Mat A = new Mat();

        if(photo.getWidth() > 1000)
        {
            int width = 1000;
            int height = (int) (1000.0*photo.getHeight()/photo.getWidth());
            Mat resized = new Mat(height, width, src.type());
            Imgproc.resize(src, resized, resized.size(), 0, 0, Imgproc.INTER_LINEAR);
            src = resized;
            photo.reconfigure(width, height, Bitmap.Config.ARGB_8888);
        }

        Mat b = Transformations.decorrelationStretch(src);

        Utils.matToBitmap(b, photo);
    }

    private void logMat(Mat mat)
    {
        for(int i = 0; i<mat.rows(); i++)
        {
            StringBuffer buffer = new StringBuffer("[");
            for(int j = 0; j<mat.cols(); j++)
            {
                double[] elements = mat.get(i,j);
                buffer.append("[");
                for(int k = 0; k<elements.length; k++)
                {
                    buffer.append(""+elements[k]+" ,");
                }
                buffer.append("], ");
            }
            buffer.append("], ");
            Log.v(TAG, buffer.toString());
        }
    }
}
