package com.husky.projection.pull.decoder.listener;

import android.media.MediaFormat;

public interface OnVideoDecoderCallback {
    void onDecoderOver();

    void onDecoderError();

    void onDecoderFormatChanged(MediaFormat mediaFormat);
}
