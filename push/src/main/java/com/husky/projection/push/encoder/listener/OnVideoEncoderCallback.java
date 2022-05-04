package com.husky.projection.push.encoder.listener;

import android.media.MediaFormat;

public interface OnVideoEncoderCallback {
    void onEncoderOver();

    void onEncoderError();

    void onEncoderFormatChanged(MediaFormat mediaFormat);
}
