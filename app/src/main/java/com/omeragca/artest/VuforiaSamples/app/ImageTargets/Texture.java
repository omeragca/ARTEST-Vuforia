package com.omeragca.artest.VuforiaSamples.app.ImageTargets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Created by omer on 19.03.2016.
 * Düzenleme: 15.04.2016
 */

public class Texture {
    //private static final String LOGTAG = "Vuforia_Texture";

    public int mWidth;          // Texture genişliği
    public int mHeight;         // Texture yüksekliği
    public int mChannels;
    public ByteBuffer mData;    // Tampon
    public int[] mTextureID = new int[1];
    public boolean mSuccess = false;


    public static Texture loadTextureFromBitmap(Bitmap bitMap) {

        int[] data = new int[bitMap.getWidth() * bitMap.getHeight()];
        bitMap.getPixels(data, 0, bitMap.getWidth(), 0, 0, bitMap.getWidth(), bitMap.getHeight());
        // Dönüşüm:
        byte[] dataBytes = new byte[bitMap.getWidth() * bitMap.getHeight() * 4];

        for (int p = 0; p < bitMap.getWidth() * bitMap.getHeight(); ++p) {
            int colour = data[p];
            dataBytes[p * 4] = (byte) (colour >>> 16); // R
            dataBytes[p * 4 + 1] = (byte) (colour >>> 8); // G
            dataBytes[p * 4 + 2] = (byte) colour; // B
            dataBytes[p * 4 + 3] = (byte) (colour >>> 24); // A
        }

        Texture texture = new Texture();
        texture.mWidth = bitMap.getWidth();
        texture.mHeight = bitMap.getHeight();
        texture.mChannels = 4;

        texture.mData = ByteBuffer.allocateDirect(dataBytes.length).order(
                ByteOrder.nativeOrder());
        int rowSize = texture.mWidth * texture.mChannels;
        for (int r = 0; r < texture.mHeight; r++)
            texture.mData.put(dataBytes, rowSize * (texture.mHeight - 1 - r),
                    rowSize);

        texture.mData.rewind(); // Tamponu temizle
        dataBytes = null;
        data = null;

        texture.mSuccess = true;
        return texture;
    }

    // Düzenleme: 15.4.2016
    // Resimlerin asset/Textures klasöründen çekilip diziye atılması
    // Bu dizi render edilecek dokulara (textures) dönüştürülecek (ImageTargets-loadTextures içinde):
    public static List<Bitmap> getBitmapsFromAssets(Context context) {
        String[] files = null;
        AssetManager assetManager = context.getAssets();
        InputStream iStream;
        Bitmap bitmap = null;

        String foldername = "Textures";

        List<Bitmap> bitmapList = new ArrayList<>();

        try {
            files = assetManager.list(foldername);
            for (String filename : files) {

                Log.e("filename", filename);

                iStream = assetManager.open(foldername + "/" + filename);
                bitmap = BitmapFactory.decodeStream(iStream);
                bitmapList.add(bitmap);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmapList;
    }

}