package com.example.alipaydemo.http;

public interface HttpCallbackListener {

    void onFinish(String response);

    void onError(Exception e);

}