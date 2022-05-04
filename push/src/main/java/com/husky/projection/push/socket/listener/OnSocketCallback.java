package com.husky.projection.push.socket.listener;

public interface OnSocketCallback {
    void onSendMessage(byte[] frame);
}
