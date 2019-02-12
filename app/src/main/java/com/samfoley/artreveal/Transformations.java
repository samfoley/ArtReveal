package com.samfoley.artreveal;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Transformations {
    private static final String TAG = "Transformations";

    public static Mat decorrelationStretch(Mat src) {
        Mat A = new Mat();


        src.convertTo(A, CvType.CV_64FC4);

        int width = A.rows();
        A = A.reshape(0, A.cols() * A.rows());
        A = A.reshape(1).submat(0, A.rows(), 0, 3);
        int channels = A.cols();

        double[] value = A.get(0, 0);
        Log.v(TAG, "A " + A + "A[0].length " + value.length);
        Mat cov = Mat.zeros(channels, channels, CvType.CV_64F);
        Mat mean = Mat.zeros(1, channels, CvType.CV_64F);
        Core.calcCovarMatrix(A, cov, mean, Core.COVAR_NORMAL | Core.COVAR_ROWS, CvType.CV_64F);

        Log.v(TAG, "Matrix cov: " + cov + " mean: " + mean + " channels: " + channels);


        Mat sigma = Mat.zeros(channels, channels, CvType.CV_64F);
        Mat stretch = Mat.zeros(channels, channels, CvType.CV_64F);
        Mat eigenvalues = new Mat();
        Mat eigenvectors = new Mat();
        Core.eigen(cov, eigenvalues, eigenvectors);

        for (int i = 0; i < channels; i++) {
            double[] tmp = cov.get(i, i);
            sigma.put(i, i, Math.sqrt(tmp[0]));
            stretch.put(i, i, 1.0 / Math.sqrt(eigenvalues.get(i, 0)[0]));
        }

        Log.v(TAG, "signa " + sigma);
        logMat(sigma);


        for (int i = 0; i < channels; i++) {
            Mat channel = A.submat(0, A.rows(), i, i + 1);
            Core.subtract(channel, new Scalar(mean.get(0, i)[0]), channel);
        }
        Mat transform = Mat.zeros(channels, channels, CvType.CV_64F);
        Mat empty = Mat.zeros(channels, channels, CvType.CV_64F);

        Core.gemm(sigma, eigenvectors, 1.0, empty, 0.0, transform, Core.GEMM_2_T);
        Core.gemm(transform.clone(), stretch, 1.0, empty, 0.0, transform);
        Core.gemm(transform.clone(), eigenvectors, 1.0, empty, 0.0, transform);

        Mat offset = Mat.zeros(1, channels, CvType.CV_64F);
        Core.gemm(mean, transform, -1.0, mean, 1.0, offset);

        Mat A_transformed = new Mat(A.size(), CvType.CV_64F);
        Core.gemm(A, transform, 1.0, empty, 0.0, A_transformed);

        Mat transform_offset = new Mat(1, channels, CvType.CV_64F);
        Core.add(mean, offset, transform_offset);
        Mat transform_matrix = Mat.zeros(channels, channels+1, CvType.CV_64F);

        for (int i = 0; i < channels; i++) {
            Mat channel = A_transformed.submat(0, A.rows(), i, i + 1);
            Core.add(channel, new Scalar(mean.get(0, i)[0] + offset.get(0, i)[0]), channel);
            Core.MinMaxLocResult result = Core.minMaxLoc(channel);
            double min = result.minVal;
            double max = result.maxVal;
            Log.v(TAG, "minmax: " + max + " min " + min);
            Core.subtract(channel, new Scalar(min), channel);
            Core.multiply(channel, new Scalar(255.0 / (max - min)), channel);
            // channel[i] = 255 * (channel[i]-min)/(max-min)

            // Vb = 255/(maxB-minB)*T
            // V[:,b] = Vb[:,b]
            for(int j = 0; j < channels; j++)
                transform_matrix.put(i,j, 255.0/(max-min)*transform.get(i,j)[0]);

            // d[b] = 255*(d[b]-minB) / (maxB-minB)
            transform_matrix.put(i, channels, 255.0*(transform_offset.get(0,i)[0]-min)/(max-min));

        }


        //Core.gemm(A, transform_matrix, 1.0, transform_matrix, 0.0, A_transformed);
        Mat A3 = A.reshape(3);
        Mat A3t = new Mat(A3.size(), A3.type());
        Core.transform(A3, A3t, transform_matrix );
        /*for(int i = 0; i<channels; i++)
        {
            Mat channel = A_transformed.submat(0, A_transformed.rows(), i,i+1);
            Core.add(channel, new Scalar(transform_offset.get(0,i)), channel);
        }*/
       // Mat temp = A_transformed.submat(0,A_transformed.rows(), 0, channels).clone();
        A_transformed = A3t.reshape(3, width);
        Mat b = new Mat();
        A_transformed.convertTo(b, CvType.CV_8UC3);

        return b;
    }

    public static void logMat(Mat mat)
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
