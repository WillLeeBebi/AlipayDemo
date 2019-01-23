package com.example.alipaydemo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alipay.sdk.app.EnvUtils;
import com.alipay.sdk.app.PayTask;
import com.example.alipaydemo.R;
import com.example.alipaydemo.http.HttpUtil;
import com.example.alipaydemo.util.PayResult;

import java.io.IOException;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private Button btn_pay;
    private TextView tv_result;
    private EditText et_out_trade_no,et_total_amount;

    private String url = "....";//这里换上自己的服务端接口地址

    private String subject = "APP支付测试";
    private String out_trade_no = "";//订单号，必须唯一
    private String timeout_express = "30m";//该笔订单允许的最晚付款时间，逾期将关闭交易
    private String total_amount = "";
    private String body = "this is body";

    private static final int SDK_PAY_FLAG = 1;


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressWarnings("unused")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_PAY_FLAG:
                    @SuppressWarnings("unchecked")
                    PayResult payResult = new PayResult((Map<String, String>) msg.obj);
                    /**
                     * 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                     */
                    String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                    String resultStatus = payResult.getResultStatus();
                    Log.e("MainActivity", payResult + "");
                    // 判断resultStatus 为9000则代表支付成功
                    if (TextUtils.equals(resultStatus, "9000")) {
                        // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                        showAlert(MainActivity.this, "支付成功" + payResult);
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        showAlert(MainActivity.this, "支付失败" + payResult);
                    }
                    break;

                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX);//接入沙箱联调
        setContentView(R.layout.activity_main);
        tv_result=findViewById(R.id.tv_result);
        et_out_trade_no=findViewById(R.id.et_out_trade_no);
        et_total_amount=findViewById(R.id.et_total_amount);
        btn_pay=findViewById(R.id.btn_pay);
        btn_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                total_amount=et_total_amount.getText().toString();
                out_trade_no=et_out_trade_no.getText().toString();
                if(total_amount.length()<=0||out_trade_no.length()<=0){
                    Toast.makeText(MainActivity.this,"请输入订单号和金额",Toast.LENGTH_SHORT).show();
                    return;
                }
                getRSASignedOrder(body,subject,out_trade_no,timeout_express,total_amount);
            }
        });

    }

    /**
     * 从服务端获取 orderInfo
     *
     * @param body            这里传入用户ID
     * @param subject         商品的标题/交易标题/订单标题/订单关键字等
     * @param out_trade_no    订单编号（必须唯一）
     * @param timeout_express 该笔订单允许的最晚付款时间，逾期将关闭交易
     * @param total_amount    订单总金额，单位为元，精确到小数点后两位，取值范围[0.01,100000000]
     */
    private void getRSASignedOrder(String body, String subject, String out_trade_no, String timeout_express, String total_amount) {
        Log.e("=========",body+subject+out_trade_no+timeout_express+total_amount);
        String address = url + "GetRSASignedOrder";
        RequestBody requestBody = new FormBody.Builder()
                .add("body", body)
                .add("subject", subject)
                .add("out_trade_no", out_trade_no)
                .add("timeout_express", timeout_express)
                .add("total_amount", total_amount)
                .build();
        HttpUtil.sendOkHttpRequestPost(address, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                Log.e("MainActivity", responseText);

                if (responseText.length() <= 0) {
                    showToast(MainActivity.this, "支付请求失败");
                    return;
                }
                showResult(responseText);
                pay(responseText);
            }
        });
    }

    /**
     * 支付（加签过程不允许在客户端进行，必须在服务端，否则有极大的安全隐患）
     *
     * @param orderInfo 加签后的支付请求参数字符串（主要包含商户的订单信息，key=value形式，以&连接）。
     */
    private void pay(final String orderInfo) {
        final Runnable payRunnable = new Runnable() {
            @Override
            public void run() {
                PayTask alipay = new PayTask(MainActivity.this);
                Map<String, String> result = alipay.payV2(orderInfo, true);//第二个参数设置为true，将会在调用pay接口的时候直接唤起一个loading
                Log.i("msp", result.toString());

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }


    private static void showAlert(Context ctx, String info) {
        showAlert(ctx, info, null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static void showAlert(Context ctx, String info, DialogInterface.OnDismissListener onDismiss) {
        new AlertDialog.Builder(ctx)
                .setMessage(info)
                .setPositiveButton("确定", null)
                .setOnDismissListener(onDismiss)
                .show();
    }

    private static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    private void showResult(final String result){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //UI操作
                tv_result.setText(result);
            }
        });
    }
}
