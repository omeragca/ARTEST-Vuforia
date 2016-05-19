package com.omeragca.artest.VuforiaSamples.app.ImageTargets;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.Vuforia;
import com.omeragca.artest.SampleApplication.SampleApplicationSession;
import com.omeragca.artest.SampleApplication.utils.CubeShaders;
import com.omeragca.artest.SampleApplication.utils.LoadingDialogHandler;
import com.omeragca.artest.SampleApplication.utils.MeshObject;
import com.omeragca.artest.SampleApplication.utils.SampleUtils;

/**
 * Created by omer on 19.03.2016.
 */
public class ImageTargetRenderer implements GLSurfaceView.Renderer {

    //private static final String LOGTAG = "ImageTargetRenderer";

    private SampleApplicationSession vuforiaAppSession;
    // Vuforia sitesine bağlantı, oturum ve barkod taraması için.

    private ImageTargetActivity mActivity;

    private Vector<Texture> mTextures;
    // Textures: barkodun üzerine döşenecek resimler-dokular.

    private int shaderProgramID;
    // Texture'un koordinatlara döşenmesi için.

    private int vertexHandle;
    // Vertex noktalarının tutulması için; yani texturenin çizileceği alanın 4 köşe koordinatları
    // Aşağıda şu şekilde tanımladık : Vx1 = -100f, Vx2 = 100f, Vy1 = -100f, Vy2 = 100f

    private int normalHandle;//?
    // TODO: 27.4.2016


    private int textureCoordHandle;
    // Texture içindeki tekrarlı fayans vs. için
    // Aşağıda şu şekilde tanımladık : x1 = 0, x2 = 4, y1 = 0, y2 = 4 (BOYUT değişkeni : 4)
    // Bu 4x4 tekrarlı resim çizecek.

    private int mvpMatrixHandle;
    // Shader'da (GPU) çizilecek model için tutulan matrix koordinatlar. (Model view projection matrix)

    private int texSampler2DHandle;
    // TODO: 27.4.2016

    public MeshObject mModel;
    // Barkodun üzerine döşenecek model şekil için.

    private Renderer mRenderer;
    // Şekli çizdirmek-render etmek için.

    boolean mIsActive = false;
    // Renderer aktif mi?

    protected static float OBJECT_SCALE_FLOAT = 3.0f;
    // Çizilecek objenin büyüklüğü (Matrix.scaleM'de çağırdık)


    public static int BOYUT = 4; //4x4
    // Texture icindeki tekrar icin

    public static float Vx1 = -100f, Vx2 = 100f, Vy1 = -100f, Vy2 = 100f; //Ekledim
    // Vertex noktaları: Texture alanını değiştirmek için

    protected static int textureIndex = 0;
    // Asset klasöründen sıralı çekilen resimlerin indexi


    public ImageTargetRenderer(ImageTargetActivity activity,
                               SampleApplicationSession session) {
        mActivity = activity;
        // Üzerinde bulunduğumuz class Renderer classı, yani GL grafik çizimleri için,
        // ProgressBar gizlemek için Activity çağırmamız gerekli.

        vuforiaAppSession = session;
        // Vuforia kütüphanesi oturumu için.
    }


    // Anlık frame-çerçeve çizimleri için
    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mIsActive)
            return;

        // Renderer aktifse bu fonksiyonunu çağırdık:
        renderFrame();
        // Burada barkod(tracker) aradık ve bulunca grafik çizilecek alanı ayırdık.
    }


    // Surface layoutu oluşturduk.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        initRendering();
        // Ayırdığımız grafik alanına texture'i (resmi) çizdirdik.


        vuforiaAppSession.onSurfaceCreated();
        // Vuforia kütüphanesi içinde surface oluşturduk.
        // LICENCE_KEY vs kontrolü için.
    }


    // Surface: çizim alanı, değiştiğinde çağırılacak.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);
    }


    // Ayırdığımız grafik alanına texture'i (resmi) çizdirdik.
    protected void initRendering() {
        //Log.e(LOGTAG, "initRendering...");

        // Çizilecek texture modelini aldık.
        mModel = new Plane();

        // Örnek renderer classını aldık.
        mRenderer = Renderer.getInstance();

        // GL tamponu temizledik.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        // Texture'ların GL üzerinde oluşturulması ve filtre, tekrar vs. düzenlemeler
        for (Texture t : mTextures) {

            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);

            // Resmin bulanıklığı gibi filtreler için
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // Resmin fayans gibi tekrarı için
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT); // Ekledim
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);


            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);

            //Log.e(LOGTAG, "glTexImage2D running...");

        }

        // shaderProgramID: çizilecek modelin hangi piksele-kaç tekrarlı vs. çeşitli özellliklerini sıralı taşıyan int değer
        // bu değeri aşağıda vertexHandle, normalHandle ... ayırdık ve bunları int değere attık.
        // bu ayrırdığımız int değişkenleri gl (gl Attribute Pointer vs) fonksiyonları içinde kullanacağız.
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        normalHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexNormal");

        Log.d("normalHandle", normalHandle + "");

        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

        // Loading dialogunu gizledik.
        mActivity.loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

    }


    // Burada barkod(tracker) aradık ve bulunca grafik çizilecek alanı ayırdık.
    private void renderFrame() {

        // Ekranı temizle; renk ve derinlik tamponlarını temizle.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Renderer durumunu tuttuk
        // TODO: 28.4.2016
        State state = mRenderer.begin();
        mRenderer.drawVideoBackground();

        // GL_DEPTH_TEST: Derinlik test özelliğini aktif ettik.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // ViewPortu GL'ye yerleştirdik.
        // TODO: 28.4.2016
        int[] viewport = vuforiaAppSession.getViewport();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        // CullFace Performans iyileştirmeleri için
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW); // Ön kamera
        else
            GLES20.glFrontFace(GLES20.GL_CCW); // Arka kamera

        // Barcode bulundu mu?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            TrackableResult result = state.getTrackableResult(tIdx);
            //Trackable trackable = result.getTrackable();
            //printUserData(trackable);

            // Bulunan barkod alanının matrix'e dönüştürülmesi, değişkenlerinin tutulması
            Matrix44F modelViewMatrix_Vuforia = Tool
                    .convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();


            // Modelview matris dönüşümleri
            float[] modelViewProjection = new float[16];

            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                    OBJECT_SCALE_FLOAT);
            Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
                    OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                    .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);

            // Shader'ı (Gpu çizici) etkinleştirmek ve vertex/normal/tex koordinatlarını bağlamak için
            GLES20.glUseProgram(shaderProgramID);


            // Düzenleme: 26.04.2016
            // Texture alanı
            // Buradaki değerler artırılıp azaltılarak parke alanının genişlemesi-daralması sağlanacak.
            double planeVertices[] = {
                    Vx1, Vy1, 0.0f,
                    Vx2, Vy1, 0.0f,
                    Vx2, Vy2, 0.0f,
                    Vx1, Vy2, 0.0f
            };
            Buffer mVertBuff = fillBuffer(planeVertices); // Tampona atılması
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                    false, 0, mVertBuff);


            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                    false, 0, mModel.getNormals());


            // Düzenleme: 15.04.2016
            // Texture alanı içinde tekrar için
            double planeTexcoords[] =
                    {
                            0, 0,
                            BOYUT, 0,
                            BOYUT, BOYUT,
                            0, BOYUT
                    };
            Buffer mTexCoordBuff = fillBuffer(planeTexcoords);
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, mTexCoordBuff);


            // Yukarıda ayarladığımız Gl özellik değişkenlerini aktif ettik.
            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);

            // texture 0'ı aktif etmek, bağlamak, ve shader'a (Gpu çiziciye) aktardık.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                    mTextures.get(textureIndex).mTextureID[0]);
            GLES20.glUniform1i(texSampler2DHandle, 0);

            // Model matrixini shadere geçirir.
            // Shader: şekli GPU'da çizdirir.
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                    modelViewProjection, 0);

            // Son olarak şekli-modeli çizdirir.
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                    mModel.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                    mModel.getIndices());

            // Etkinleştirilen gl özellik değişkenlerini devre dışı bırakır.
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);


            // GL hatalarını yazdırmak için.
            SampleUtils.checkGLError("Render Frame");

        }

        // Etkinleştirilen derinlik değişkenini devre dışı bırakır.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Renderer'ı sona erdir.
        // Renderer, çizilmiş veya düzenlenmiş olan ham modeli işleyip resme çevirir.
        mRenderer.end();

    }

    // Alınan dizileri tampon belleğe atar.
    protected Buffer fillBuffer(double[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind(); // geri ver.

        return bb;

    }


    private void printUserData(Trackable trackable) {
        String userData = (String) trackable.getUserData();
        //Log.d(LOGTAG, "UserData:Retreived User Data	\"" + userData + "\"");
    }


    // Texture'ları değişkene attık.
    public void setTextures(Vector<Texture> textures) {
        mTextures = textures;
    }


}
