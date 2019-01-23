package com.example.alipaydemo.http;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpUtil {

    /**
     * 使用OKhttp请求（GET）
     * @param address
     * @param callback
     */
    public static void sendOkHttpRequest(final String address, final Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(address)
                .build();
        client.newCall(request).enqueue(callback);
    }

    /**
     * 使用OKhttp请求（POST）
     * @param address
     * @param requestBody RequestBody对象用来存放待提交的参数
     * @param callback
     */
    public static void sendOkHttpRequestPost(final String address, RequestBody requestBody, final Callback callback){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(address)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(callback);
    }

}
