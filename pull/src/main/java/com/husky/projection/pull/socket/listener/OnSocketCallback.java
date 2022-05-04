package com.husky.projection.pull.socket.listener;

public interface OnSocketCallback {
    void onReceiveMessage(byte[] frame);
}
