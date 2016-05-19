package com.omeragca.artest.VuforiaSamples.app.ImageTargets;

import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ObjectTracker;
import com.vuforia.State;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;
import com.omeragca.artest.SampleApplication.SampleApplicationControl;
import com.omeragca.artest.SampleApplication.SampleApplicationException;
import com.omeragca.artest.SampleApplication.SampleApplicationSession;
import com.omeragca.artest.SampleApplication.utils.LoadingDialogHandler;
import com.omeragca.artest.SampleApplication.utils.SampleApplicationGLView;
import com.omeragca.artest.VuforiaSamples.R;

/**
 * Created by omer on 19.03.2016.
 */
public class ImageTargetActivity extends Activity implements SampleApplicationControl {
    //private static final String LOGTAG = "ImageTargets";

    // Vuforia sitesine bağlantı, oturum-lisans kontrolü ve barkod taraması için.
    SampleApplicationSession vuforiaAppSession;

    // Barkod taramasından alınan veriler için.
    private DataSet mCurrentDataset;

    // Çoklu tracker (izlenen nesne) için ilkini seçtik.
    private int mCurrentDatasetSelectionIndex = 0;

    // İzlenen nesnelerin (tracker) alınması ve tutulması.
    private ArrayList<String> mDatasetStrings = new ArrayList<>();

    // OpenGL view - Düzenlenebilir örnek layout:
    private SampleApplicationGLView mGlView;

    // Renderer; çizilmiş ham modeli işleyip resme çevirir
    private ImageTargetRenderer mRenderer;

    // Kullanılacak dokular (textures) (Alınan resimler burada tutulacak)
    private Vector<Texture> mTextures;

    private boolean mSwitchDatasetAsap = false; // Çoklu izlenen nesneler için, önemli değil

    // Buton vs. için Genel layout
    private RelativeLayout mUILayout;

    // Loading diyalog:
    LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    private AlertDialog mErrorDialog;

    boolean mIsDroidDevice = false;

    // Dokunma kontrolleri için
    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        // Tam ekran görüntülenmesi için
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Vuforia oturum icin
        vuforiaAppSession = new SampleApplicationSession(this);

        // Layoutun bağlanması:
        startLoadingAnimationAndBindLayout();

        // İzlenecek nesnenin asset klasöründen alınması ve değişkene atılması:
        mDatasetStrings.add("barcode.xml");

        // Ekranı yatay ayarladık.
        vuforiaAppSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Dokunma kontrolleri için
        mGestureDetector = new GestureDetector(this, new GestureListener());


        // İzlenen nesnenin üzerine döşenecek dokular-resimler(textures) yüklenmesi:
        mTextures = new Vector<>();
        loadTextures();

        // Android cihaz mı?
        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().
                startsWith(
                        "droid");
    }


    // Düzenleme: 15.4.2016
    private void loadTextures() {
        for (Bitmap bitmap : Texture.getBitmapsFromAssets(this)) {
            mTextures.add(Texture.loadTextureFromBitmap(bitmap));
        }
    }

    // Durdurulan activity yeniden çalıştığında
    @Override
    protected void onResume() {
        //Log.d(LOGTAG, "onResume");
        super.onResume();

        if (mIsDroidDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        try {
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e) {
            //Log.e(LOGTAG, e.getString());
        }

        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }

    }


    // Yapılandırma ayarları değiştiğinde vuforia oturumuna bildirmek için:
    @Override
    public void onConfigurationChanged(Configuration config) {
        //Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }


    // Uygulama durdurulduğunda:
    @Override
    protected void onPause() {
        //Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        try {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e) {
            //Log.e(LOGTAG, e.getString());
        }
    }



    // Uygulama yok edildiğinde:
    @Override
    protected void onDestroy() {
        //Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e) {
            //Log.e(LOGTAG, e.getString());
        }

        mTextures.clear();
        mTextures = null;

        System.gc();
    }


    // AR uygulama bileşenlerinin hazırlanması:
    private void initApplicationAR() {

        // OpenGL view yaratılması:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new ImageTargetRenderer(this, vuforiaAppSession);

        // Textures (dokuların) render'a yüklenmesi:
        mRenderer.setTextures(mTextures);

        // Render edilen görüntünün SurfaceView layouta yerleştirilmesi:
        mGlView.setRenderer(mRenderer);

    }

    RadioGroup radioGroup;

    // Layoutun bağlanması (parkelerin (radiobutton) görüntülenmesi vs.):
    private void startLoadingAnimationAndBindLayout() {
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay,
                null);

        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.pbLoading);

        loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);


        // Düzenleme: 19.4.2016
        // radioButton'ların yüklenmesi
        radioGroup = (RadioGroup) mUILayout.findViewById(R.id.rGroup);
        RadioButton radioButton;
        int index = 0;
        for (Bitmap bitmap : Texture.getBitmapsFromAssets(this)) {
            radioButton = new RadioButton(this);

            float scale = getResources().getDisplayMetrics().density;
            // her telefonda ayn gözükmesi için yoğunluk ayarı.
            radioButton.setWidth((int) (80 * scale));
            radioButton.setHeight((int) (80 * scale));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                radioButton.setBackground(new BitmapDrawable(getResources(), bitmap));
            } else {
                radioButton.setBackgroundDrawable(new BitmapDrawable(getResources(), bitmap));
            }

            if (index == 0) {
                radioButton.setChecked(true);
            }

            radioButton.setId(index);
            radioGroup.addView(radioButton);
            index++;
        }


        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {

                ImageTargetRenderer.textureIndex = checkedId;
                // Burada seçilen resmin indexi ImageTargetRenderer textureIndex değişkenine gonderilecek
                // ImageTargetRenderer textureIndex değerine göre resmi şeklin üzerine döşeyecek

                //Log.e(LOGTAG, "Texture değişti...");

                initApplicationAR();
                // Renderer ve diğer değişkenleri baştan yükle
            }
        });

    }


    // İzlenen datanın yüklenmesi ve kaldırılması için
    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet();

        if (mCurrentDataset == null)
            return false;

        if (!mCurrentDataset.load(
                mDatasetStrings.get(mCurrentDatasetSelectionIndex),
                STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false;

        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false;

        int numTrackables = mCurrentDataset.getNumTrackables();
        for (int count = 0; count < numTrackables; count++) {
            Trackable trackable = mCurrentDataset.getTrackable(count);

            trackable.startExtendedTracking();

            //String name = "Current Dataset : " + trackable.getName();
            //trackable.setUserData(name);
            //Log.d(LOGTAG, "UserData:Set the following user data "
            //       + (String) trackable.getUserData());
        }

        return true;
    }


    // İzleyici doğru yüklenmediğinde:
    @Override
    public boolean doUnloadTrackersData() {
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset != null && mCurrentDataset.isActive()) {
            if (objectTracker.getActiveDataSet().equals(mCurrentDataset)
                    && !objectTracker.deactivateDataSet(mCurrentDataset)) {
                result = false;
            } else if (!objectTracker.destroyDataSet(mCurrentDataset)) {
                result = false;
            }

            mCurrentDataset = null;
        }

        return result;
    }


    // AugmentedReality için ilk kullanıma hazırlamak; kamera vs. gerekli fonksiyonlar başlatıldı:
    @Override
    public void onInitARDone(SampleApplicationException exception) {

        if (exception == null) {
            initApplicationAR();

            initViewElements();

            mRenderer.mIsActive = true;

            // Kamera görüntüsünün aktarılacağı SurfaceView layoutunu ekledik:
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            // Arka kameradan alınanı görüntüyü layoutta gösterir.
            mUILayout.bringToFront();

            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            try {
                vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (SampleApplicationException e) {
                //Log.e(LOGTAG, e.getString());
            }

            //boolean result = CameraDevice.getInstance().setFocusMode(
            // CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

        } else {
            //Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }

    // Hata mesajlarının telefonda diyalog şeklinde gösterilmesi için.
    public void showInitializationErrorMessage(String message) {
        final String errorMessage = message;
        runOnUiThread(new Runnable() {
            public void run() {
                if (mErrorDialog != null) {
                    mErrorDialog.dismiss();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(
                        ImageTargetActivity.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton(getString(R.string.button_OK),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }


    // Vuforia güncellendiğinde:
    // İzlenen resim-data değiştiğinde çağırılacak:
    @Override
    public void onVuforiaUpdate(State state) {
        if (mSwitchDatasetAsap) {
            mSwitchDatasetAsap = false;
            TrackerManager tm = TrackerManager.getInstance();
            ObjectTracker ot = (ObjectTracker) tm.getTracker(ObjectTracker
                    .getClassType());
            if (ot == null || mCurrentDataset == null
                    || ot.getActiveDataSet() == null) {
                //Log.d(LOGTAG, "Failed to swap datasets");
                return;
            }

            // İzlenen hedefi kaldır ve tekrar yükle.
            doUnloadTrackersData();
            doLoadTrackersData();
        }
    }


    // İzleyiciyi ilk kullanıma hazırladık:
    @Override
    public boolean doInitTrackers() {
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        // Trying to initialize the image tracker
        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) {
            //Log.e(LOGTAG,
            //       "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else {
            //Log.i(LOGTAG, "Tracker successfully initialized");
        }
        return result;
    }


    // İzleyici başlatıldığında:
    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();

        return result;
    }


    // İzleyiciyi durdurduk:
    @Override
    public boolean doStopTrackers() {
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();

        return result;
    }


    // İzleyiciyi kaldırdık.
    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }


    // Düzenleme: 29.4.2016
    // Kullanım talimatları vs. gosterilecek:
    private void initViewElements() {
        Toast.makeText(this, "Ayarları açmak için bir kere dokunun...", Toast.LENGTH_LONG).show();
    }

    // Düzenleme: 29.04.2016
    // Dokunma:
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    // Tek dokunma kontrolü:
    // Butonları görünür yapacak-gizleyecek
    // Focus-Odaklanma için.
    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener {
        private final Handler autofocusHandler = new Handler();
        boolean isVisible = false;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }


        @Override
        public boolean onSingleTapUp(MotionEvent e) {

            if (!isVisible) {
                mUILayout.findViewById(R.id.lnX1).setVisibility(View.VISIBLE);
                mUILayout.findViewById(R.id.lnY1).setVisibility(View.VISIBLE);
                mUILayout.findViewById(R.id.lnX2).setVisibility(View.VISIBLE);
                mUILayout.findViewById(R.id.lnY2).setVisibility(View.VISIBLE);
                mUILayout.findViewById(R.id.my_layout).setVisibility(View.VISIBLE);
                isVisible = !isVisible;
            } else {
                mUILayout.findViewById(R.id.lnX1).setVisibility(View.INVISIBLE);
                mUILayout.findViewById(R.id.lnY1).setVisibility(View.INVISIBLE);
                mUILayout.findViewById(R.id.lnX2).setVisibility(View.INVISIBLE);
                mUILayout.findViewById(R.id.lnY2).setVisibility(View.INVISIBLE);
                mUILayout.findViewById(R.id.my_layout).setVisibility(View.INVISIBLE);
                isVisible = !isVisible;
            }


            autofocusHandler.postDelayed(new Runnable() {
                public void run() {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);

                    if (!result)
                        Log.e("SingleTapUp", "Unable to trigger focus");
                }
            }, 1000L);

            return true;
        }
    }


    // Düzenleme: 26-28.04.2016
    // Butonlara bağlı olarak Texture'u büyütecek küçültecek.
    public void bX1PlusClicked(View view) {
        ImageTargetRenderer.Vx1 += 10;
        initApplicationAR();
    }

    public void bX1NegClicked(View view) {
        ImageTargetRenderer.Vx1 -= 10;
        initApplicationAR();
    }

    public void bX2PlusClicked(View view) {
        ImageTargetRenderer.Vx2 += 10;
        initApplicationAR();
    }

    public void bY1PlusClicked(View view) {
        ImageTargetRenderer.Vy1 += 10;
        initApplicationAR();
    }

    public void bY2PlusClicked(View view) {
        ImageTargetRenderer.Vy2 += 10;
        initApplicationAR();
    }



    public void bX2NegClicked(View view) {
        ImageTargetRenderer.Vx2 -= 10;
        initApplicationAR();
    }

    public void bY1NegClicked(View view) {
        ImageTargetRenderer.Vy1 -= 10;
        initApplicationAR();
    }

    public void bY2NegClicked(View view) {
        ImageTargetRenderer.Vy2 -= 10;
        initApplicationAR();
    }


}
