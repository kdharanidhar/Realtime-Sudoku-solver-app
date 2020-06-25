package com.example.sud;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import androidx.annotation.NonNull;

class SudokuExtractor {
    private Mat puzzle;
    private Bitmap puzzle_bm;
    private Context context;
    SudokuExtractor(Bitmap bm, Context context){
        puzzle=new Mat();
        puzzle_bm=bm;
        Bitmap bmp32 = bm.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, puzzle);
        this.context = context;
    }
    private String processTextRecognitionResult(FirebaseVisionText text){
        List<FirebaseVisionText.TextBlock> blocks = text.getTextBlocks();
        if(blocks.size()!=0)
            Log.d("Block size",blocks.size()+"");
        if (blocks.size() == 0) {
            return ".";
        }
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    if(blocks.size()!=0) {
                        Log.d("Rec text", elements.get(k).getBoundingBox().flattenToString());
                        Log.d("Detec text", elements.get(k).getText());
                    }
                    return elements.get(k).getText();
                }
            }
        }
        return ".";
    }
    private String no;
    private void getNum(Mat image){
        Point[] h_p=new Point[10];
        Point[] v_p=new Point[10];
        double PADDING = image.width()/19;
        h_p[0]= new Point(0,0);
        v_p[0]=new Point(0,0);

        int SUDOKU_SIZE = 9;
        int IMAGE_WIDTH = image.width();
        int IMAGE_HEIGHT = image.height();
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
                Mat digit_cropped = new Mat(image, R);
                Imgproc.GaussianBlur(digit_cropped,digit_cropped,new Size(5,5),0);
                Imgproc.rectangle(image, p1, p2, new Scalar(0, 255, 0,255),3);
                Bitmap digit_bitmap = Bitmap.createBitmap(digit_cropped.cols(), digit_cropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(digit_cropped, digit_bitmap);




                FirebaseVisionImage vimage = FirebaseVisionImage.fromBitmap(digit_bitmap);
                FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                        .getOnDeviceTextRecognizer();
                Task<FirebaseVisionText> result =
                        detector.processImage(vimage)
                                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText text) {
                                String temp = processTextRecognitionResult(text);
                                if((temp.toCharArray()[0]>='1'&& temp.toCharArray()[0]<='9'))
                                    no+=temp;
                                else
                                    no+='.';
                                if(no.length()==81){
                                    Intent intent = new Intent(context,PuzzleActivity.class);
                                    intent.putExtra("no",no);
                                    context.startActivity(intent);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("Error in detection   ",e.getMessage());
                            }
                        });
            }
        }
    }
    public void getNum(){
        if(!puzzle.empty()){
            no="";
            getNum(puzzle);
        }
    }
}

