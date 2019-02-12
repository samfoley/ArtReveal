package com.samfoley.artreveal;

import android.Manifest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";

    private CameraBridgeViewBase cameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        cameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        cameraView.setMaxFrameSize(500,500);
        cameraView.setRotation(180.0f);

        cameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        cameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener()
        {

            @Override
            public void onCameraViewStarted(int width, int height) {
                Log.i(TAG, "Camera started");
            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(Mat inputFrame) {
                Log.i(TAG, "inputFrame "+inputFrame);
                if(inputFrame.rows()>500 || inputFrame.cols()>500)
                {
                    Mat frame = new Mat(500,500, inputFrame.type());
                    Imgproc.resize(inputFrame, frame, frame.size());
                    Mat stretch = Transformations.decorrelationStretch(frame);
                    //stretch.copyTo(inputFrame.submat(0,500,0,500));
                    Imgproc.resize(stretch, inputFrame, inputFrame.size());
                    return inputFrame;
                }
                return Transformations.decorrelationStretch(inputFrame);
            }
        });

        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);

        cameraView.enableView();


    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
