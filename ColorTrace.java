package com.example.healthdemo.activity;

import android.Manifest;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.healthdemo.R;
import com.example.healthdemo.common.Data;
import com.yalantis.contextmenu.lib.ContextMenuDialogFragment;
import com.yalantis.contextmenu.lib.MenuParams;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.List;

/**
 * Created by Feemic on 2016/8/8.
 */
public class ColorTrace extends AppCompatActivity implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2
{
    private FragmentManager fragmentManager;
    private ContextMenuDialogFragment mMenuDialogFragment;

    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat mRgba;
    private Mat                 mRgbaF;
    private Mat                  mRgbaT;

    private Scalar mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    public int sport_count;
    public  int flag,ok_flag;
    private ImageButton back,buttonok;
    private Context mContext;


    private CameraBridgeViewBase mOpenCvCameraView;
    //the camara prio
    private static final int TAKE_PHOTO_REQUEST_CODE = 1;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorTrace.this);
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public ColorTrace()
    {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "called onCreate");
        mContext = this;
        sport_count=0;
        flag=0;
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.camera_layout);
        fragmentManager = getFragmentManager();
        initToolbar();
        initMenuFragment();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.lotine_background));

        if(Build.VERSION.SDK_INT>=23){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    TAKE_PHOTO_REQUEST_CODE);
        }
        }

        if (Build.VERSION.SDK_INT >= 21)
        {
            getWindow().setStatusBarColor(getResources().getColor(R.color.lotine_background));
        }
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        // mOpenCvCameraView.setCameraIndex(this);
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
        if (!OpenCVLoader.initDebug())
        {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        }
        else
        {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy()
    {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height)
    {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        this.mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        this.mRgbaT = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped()
    {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event)
    {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        // int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        //int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)(event.getX()/mOpenCvCameraView.getWidth()*cols);
        int y = (int)(event.getY()/mOpenCvCameraView.getHeight()*rows);

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount

        //
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.rgb((int) this.mBlobColorRgba.val[0], (int) this.mBlobColorRgba.val[1], (int) this.mBlobColorRgba.val[2]));
        if (Build.VERSION.SDK_INT >= 21)
        {
            getWindow().setStatusBarColor(Color.rgb((int)this.mBlobColorRgba.val[0], (int)this.mBlobColorRgba.val[1], (int) this.mBlobColorRgba.val[2]));
        }


        Log.e(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        mRgba = inputFrame.rgba();
        Object localObject;

        Core.transpose(this.mRgba, this.mRgbaT);
        Imgproc.resize(this.mRgbaT, this.mRgbaF, this.mRgbaF.size(), 0.0D, 0.0D, 0);
        Core.flip(this.mRgbaF, this.mRgba, 1);  

        if (mIsColorSelected)
        {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.v(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            localObject = new Message();
            Bundle localBundle = new Bundle();

            if (contours.size() != 0)
            {
                Log.e("coutonrs.size!=0","!=1");
                Moments localMoments = Imgproc.moments((Mat)contours.get(0), false);
                int i = (int)(localMoments.get_m10() / localMoments.get_m00());
                int j = (int)(localMoments.get_m01() / localMoments.get_m00());
                int k = (int)((MatOfPoint)contours.get(0)).size().area();
                localBundle.putInt("x", j);
                localBundle.putInt("y", i);
                localBundle.putInt("size", k);
                Log.e("x:",i+"y:"+j+"size:"+k);
                ((Message)localObject).what = 0;
                ((Message)localObject).setData(localBundle)

			/*Mat colorLabel = mRgba.submat(0, 38, 0, 800); //间距为0，写38行，800列
			 colorLabel.setTo(mBlobColorRgba);*/

            //Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            // mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor)
    {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    private void initMenuFragment()
    {
        MenuParams menuParams = new MenuParams();
        menuParams.setClosableOutside(false);
    }


    private void initToolbar()
    {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView mToolBarTextView = (TextView) findViewById(R.id.text_view_toolbar_title);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        mToolbar.setNavigationIcon(R.drawable.btn_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mToolBarTextView.setGravity(Gravity.CENTER);
        mToolBarTextView.setText("点击颜色进行跟踪");

    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.context_menu:
                if(mIsColorSelected==true) {
                    Toast.makeText(this, "已选中要跟踪的颜色 ", Toast.LENGTH_SHORT).show();
                    ok_flag=1;
                    sport_count = 0;
                }
                else      Toast.makeText(this, "请先选择要跟踪的颜色 ", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        if (mMenuDialogFragment != null && mMenuDialogFragment.isAdded())
        {
            mMenuDialogFragment.dismiss();
        }
        else
        {
            finish();
        }
    }
}

