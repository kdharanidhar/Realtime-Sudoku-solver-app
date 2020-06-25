package com.example.sud;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean sudokuReg;
    private Bitmap puzzle;
    Button RL,RR,scan,recapture;
    ProgressBar progressBar;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;
    Mat mIntermediateMat;
    ImageView imageView;
    SudokuExtractor extractor;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    imageView.setVisibility(View.INVISIBLE);
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
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        FirebaseOptions options = new FirebaseOptions.Builder().setApplicationId("sudoku-93f70").build();
        FirebaseApp.initializeApp(this, options,"sudoko");


        mOpenCvCameraView.setCvCameraViewListener(this);
        imageView=findViewById(R.id.image);
        RR = findViewById(R.id.rotate_right);
        RL = findViewById(R.id.rotate_left);
        sudokuReg=false;
        RR.setVisibility(View.INVISIBLE);
        RL.setVisibility(View.INVISIBLE);
        progressBar= findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        scan=findViewById(R.id.button);
        recapture=findViewById(R.id.button2);
        recapture.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
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
        changeUI();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // TODO Auto-generated method stub
        mRgba = inputFrame.rgba();
        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0, 0, 0);
        Core.flip(mRgbaF, mRgba, 1);
        PuzzleFinder finder = new PuzzleFinder(mRgba);
        return finder.getPuzzle(false);
    }


    public void scan(View view) {
        RR.setVisibility(View.VISIBLE);
        RL.setVisibility(View.VISIBLE);
        recapture.setVisibility(View.VISIBLE);
        if(!sudokuReg) {
            Mat source = mRgba;
            PuzzleFinder finder = new PuzzleFinder(source);
            Mat m = finder.getPuzzle(true);
            Mat temp = m.clone();
            Mat lines = new Mat();
            double PADDING =temp.width()/19;

            int SUDOKU_SIZE = 9;
            int IMAGE_WIDTH = temp.width();
            int IMAGE_HEIGHT = temp.height();
            int HSIZE = IMAGE_HEIGHT/SUDOKU_SIZE;
            int WSIZE = IMAGE_WIDTH/SUDOKU_SIZE;


            int[][] sudos = new int[SUDOKU_SIZE][SUDOKU_SIZE];

            // Divide the image to 81 small grid and do the digit recognition
            for (int y = 0, iy = 0; y < IMAGE_HEIGHT - HSIZE ; y+= HSIZE,iy++) {
                for (int x = 0, ix = 0; x < IMAGE_WIDTH - WSIZE; x += WSIZE, ix++) {
                    sudos[iy][ix] = 0;
                    int cx = (x + WSIZE / 2);
                    int cy = (y + HSIZE / 2);
                    org.opencv.core.Point p1 = new org.opencv.core.Point(cx - PADDING, cy - PADDING);
                    org.opencv.core.Point p2 = new org.opencv.core.Point(cx + PADDING, cy + PADDING);
                    Rect R = new Rect(p1, p2);
                    Mat digit_cropped = new Mat(temp, R);
                    Imgproc.GaussianBlur(digit_cropped, digit_cropped, new Size(5, 5), 0);
                    Imgproc.rectangle(temp, p1, p2, new Scalar(0, 255, 0, 255), 3);
                    Bitmap digit_bitmap = Bitmap.createBitmap(digit_cropped.cols(), digit_cropped.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(digit_cropped, digit_bitmap);
                }
            }

            Bitmap bm = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(temp, bm);
            //Utils.matToBitmap(m,bm);
            sudokuReg=true;

            puzzle=bm;
            mOpenCvCameraView.disableView();
            mOpenCvCameraView.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(bm);
            scan.setText("Extract");
        }
        else{
            progressBar.setVisibility(View.VISIBLE);
            extractor = new SudokuExtractor(puzzle,this);
            extractor.getNum();
        }
    }
    private void changeUI(){
        mOpenCvCameraView.setVisibility(View.VISIBLE);
        mOpenCvCameraView.enableView();
        imageView.setVisibility(View.INVISIBLE);
        sudokuReg=false;
        RR.setVisibility(View.INVISIBLE);
        RL.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        recapture.setVisibility(View.INVISIBLE);
        scan.setText("Scan");
    }
    public void recapture(View view) {
        changeUI();
    }

    public void rotateLeft(View view) {
        Matrix mat = new Matrix();
        mat.postRotate(90);
        puzzle= Bitmap.createBitmap(puzzle,0,0,puzzle.getWidth(),puzzle.getHeight(),mat,true);
        imageView.setImageBitmap(puzzle);
    }

    public void rotateRight(View view) {
        Matrix mat = new Matrix();
        mat.postRotate(-90);
        puzzle= Bitmap.createBitmap(puzzle,0,0,puzzle.getWidth(),puzzle.getHeight(),mat,true);
        imageView.setImageBitmap(puzzle);
    }
}
