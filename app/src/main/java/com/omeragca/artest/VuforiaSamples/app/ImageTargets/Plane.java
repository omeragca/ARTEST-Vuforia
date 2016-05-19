package com.omeragca.artest.VuforiaSamples.app.ImageTargets;

import com.omeragca.artest.SampleApplication.utils.MeshObject;

import java.nio.Buffer;


/**
 * Created by omer on 19.03.2016.
 * Düzenleme: 26.04.2016
 */

public class Plane extends MeshObject {


    // planeTexcoords tekrar edilecek-döşenecek texture sayısı:
    // bir dikdörtgen gibi düşünürsek, koordinatlar
    // şu şekilde x1, y1, x2, y1, x2, y2, x1, y2:
    private final static double planeTexcoords[] =
            {
                    0, 0,
                    4, 0,
                    4, 4,
                    0, 4
            };

    // Genel texture'un yerleştirileceği piksel cinsinden alan:
    // bir dikdörtgen gibi düşünürsek, koordinatlar
    // şu şekilde x1, y1, x2, y1, x2, y2, x1, y2:
    private final static double planeVertices[] =
            {
                    -100f, -100f, 0.0f,
                    100f, -100f, 0.0f,
                    100f, 100f, 0.0f,
                    -100f, 100f, 0.0f
            };

    private final static double planeNormals[] =
            {
                    0, 0, 1,
                    0, 0, 1,
                    0, 0, 1,
                    0, 0, 1
            };

    private final static short planeIndices[] =
            {
                    0, 1, 2, 0, 2, 3
            };


    private Buffer mVertBuff;
    private Buffer mTexCoordBuff;
    private Buffer mNormBuff;
    private Buffer mIndBuff;

    public Plane() {

        mVertBuff = fillBuffer(planeVertices);
        mTexCoordBuff = fillBuffer(planeTexcoords);
        mNormBuff = fillBuffer(planeNormals);
        mIndBuff = fillBuffer(planeIndices);
    }

    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType) {
        Buffer result = null;
        switch (bufferType) {
            case BUFFER_TYPE_VERTEX:
                result = mVertBuff;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                result = mTexCoordBuff;
                break;
            case BUFFER_TYPE_INDICES:
                result = mIndBuff;
                break;
            case BUFFER_TYPE_NORMALS:
                result = mNormBuff;
            default:
                break;
        }
        return result;
    }

    @Override
    public int getNumObjectVertex() {
        return planeVertices.length / 3;
    }

    @Override
    public int getNumObjectIndex() {
        return planeIndices.length;
    }
}