package com.zkteco.android.graphics;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

/**
 * Utilities for converting raw image.
 */
public class ImageConverter {
    public static final int DEFAULT_ID_PHOTO_WIDTH = 102;
    public static final int DEFAULT_ID_PHOTO_HEIGHT = 126;

    static {
        System.loadLibrary("imageutils");
    }

    private ImageConverter() {
        // Forbidden to initialize.
    }

    public native static void YUV420sp2BGR(byte[] yuv420sp, byte[] bgr, int width, int height);

    public native static void YUV420p2YUV420sp(byte[] yuv420p, byte[] yuv420sp, int width, int height);

    public native static void YUV420pRotate90(byte[] src, byte[] dst, int width, int height);

    public native static void YUV420pRotate270(byte[] src, byte[] dst, int width, int height);

    public native static void YUV420pRotateNegative90(byte[] src, byte[] dst, int width, int height);

    public native static void YUV420p2YLuminance(byte[] src, byte[] dst, int width, int height);

    public native static void YUV420p2RGB8888(byte[] yuv420p, int[] rgb8888, int width, int height);

    public native static void luminanceRotate90(byte[] src, byte[] dst, int width, int height);

    public native static void luminanceRotateNegative90(byte[] src, byte[] dst, int width, int height);

    public native static void cropRGBImage(int[] src, int[] dst, int height, int left, int top, int right, int bottom);

    public native static void NV212RGB(byte[] nv21, byte[] rgb, int width, int height);

    public native static void rotateNV21Degree90(byte[] src, byte[] dst, int width, int height);

    public native static void rotateNV21Degree180(byte[] src, byte[] dst, int width, int height);

    public native static void rotateNV21Degree270(byte[] src, byte[] dst, int width, int height);

    public native static void rotateNV21(byte[] input, byte[] output, int width, int height, final int rotation, boolean flipHorizontal);

    public native static void rotateNV21Ext(byte[] input, byte[] output, int width, int height, final int rotation);

    public native static void BGR2RGB565(byte[] bgr, int[] rgb565, int width, int height);

    public native static void ARGB88882BGR(byte[] argb8888, byte[] bgr, int width, int height);

    public native static void RGB5652BGR(byte[] rgb565, byte[] bgr, int width, int height);

    public native static void Grey2RGB565(byte[] yuv, int[] rgb565, int width, int height);

    public native static double calculateLuminance(byte[] yuv, int left, int top, int right, int bottom);

    public native static void cropNV21Image(byte[] src, byte[] dst, int width, int height, int startX, int startY, int cropWidth, int cropHeight);

    public static void YUV_NV212RGB(byte[] yuv, byte[] rgb, int width, int height) {
        final int frameSize = width * height;

        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;

        int a = 0;
        for (int i = 0, ci = ii; i < height; ++i, ci += di) {
            for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
                int y = (0xff & ((int) yuv[ci * width + cj]));
                int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
                int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
                int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                rgb[a++] = (byte) r;
                rgb[a++] = (byte) g;
                rgb[a++] = (byte) b;
            }
        }
    }

    public static void erectImage(byte[] src, byte[] dst, int width, int height, int depth) {
        for (int h = 0; h < height; h++) {
            System.arraycopy(src, h * width * depth, dst, (height - h - 1) * width * depth, width * depth);
        }
    }

    public static Bitmap createIdPhotoBitmap(byte[] bgrbuf) {
        int width = DEFAULT_ID_PHOTO_WIDTH;
        int height = DEFAULT_ID_PHOTO_HEIGHT;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        int[] pixels = new int[width * height];
        BGR2RGB565(bgrbuf, pixels, width, height);
        bmp.setPixels(pixels, 0, width, 0, 0, width, height);

        return bmp;
    }

    /**
     * Using the array generate a Bitmap object
     *
     * @param yuvData
     * @param width
     * @param height
     * @return
     */
    public static Bitmap createGreyScaleBitmap(byte[] yuvData, final int width, final int height) {
        if (yuvData == null || width <= 0 || height <= 0) {
            return null;
        }

        int[] pixels = new int[width * height];
        Grey2RGB565(yuvData, pixels, width, height);
        final Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.RGB_565);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * Gets the pixels from bitmap.
     *
     * @param bm
     * @return
     */
    private static byte[] getPixels(Bitmap bm) {
        // calculate how many bytes our image consists of
        int bytes = bm.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        bm.copyPixelsToBuffer(buffer); // Move the byte data to the buffer

        byte[] pixels = buffer.array(); // Get the underlying array containing the data.
        return pixels;
    }

    /**
     * Convert bitmap to bgr.
     *
     * @param bm
     * @param bgrBuf
     * @param width
     * @param height
     * @return
     */
    public static boolean Bitmap2BGR(Bitmap bm, byte[] bgrBuf, int width, int height) {
        boolean ret = true;

        switch (bm.getConfig()) {
            case RGB_565: {
                byte[] rgb565Buf = getPixels(bm);
                RGB5652BGR(rgb565Buf, bgrBuf, width, height);
                break;
            }
            case ARGB_8888: {
                byte[] argb8888Buf = getPixels(bm);
                ARGB88882BGR(argb8888Buf, bgrBuf, width, height);
                break;
            }
            default: {
                Bitmap argb8888Bitmap = bm.copy(Bitmap.Config.ARGB_8888, false);
                if (argb8888Bitmap == null) {
                    ret = false;
                    break;
                }

                byte[] argb8888Buf = getPixels(bm);
                ARGB88882BGR(argb8888Buf, bgrBuf, width, height);

                argb8888Bitmap.recycle();
                argb8888Bitmap = null;
                break;
            }
        }

        return ret;
    }
}