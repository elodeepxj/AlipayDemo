package com.jokerpeng.demo.alipaydemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.alipay.sdk.app.PayTask;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Created by PengXiaoJie on 2017/6/2.10 51..
 */

public class AlipayUtils {
    private static final String ALGORITHM = "RSA";
    private static final String SIGN_ALGORITHMS = "SHA1WithRSA";
    private static final String SIGN_SHA256RSA_ALGORITHMS = "SHA256WithRSA";
    private static final String DEFAULT_CHARSET = "UTF-8";

    public static final int SDK_PAY_FLAG = 1;
    public static final int SDK_AUTH_FLAG = 2;
    public static final String DEFAULT_TIMEOUT_EXPRESS = "30m";
    public static final String PRODUCT_CODE = "QUICK_MSECURITY_PAY";

    public static final String RSA2_PRIVATE = "";
    public static final String RSA_PRIVATE = "";

    private AlipayUtils mAlipayUtils;
    private Activity activity;
    private String appId;
    /**
     * 商品总金额
     * */
    private String total_amount;
    /**
     * 商品标题
     * */
    private String subject;
    /**
     * 交易描述信息
     * */
    private String body;
    /**
     * 商品订单
     * */
    private String out_trade_no;
    /**
     * 未付款支付宝交易的超时时间
     * */
    private String timeout_express;

    private Handler mHandler;
    /**
     * @param activity 上下文，当前activity
     * @param total_amount 总金额
     * @param subject 商品标题
     * @param body 商品内容
     * @param out_trade_no 订单号
     * @param timeout_express 未付款支付宝交易的超时时间
     * @param handler
     * */
    public AlipayUtils(@Nullable Activity activity, @Nullable String total_amount,@Nullable String subject, String body,@Nullable String out_trade_no, String timeout_express, Handler handler) {
        this.activity = activity;
        this.total_amount = total_amount;
        this.subject = subject;
        this.body = body;
        this.out_trade_no = out_trade_no;
        this.timeout_express = timeout_express;
        this.mHandler = handler;
    }

    /**
     * @param subject 商品的标题
     * @param out_trade_no 订单号
     * @param total_amount 总金额
     * @param product_code 销售产品码
     * @param body 产品内容
     * @param timeout_express 未付款支付宝交易的超时时间
     * */
    private String getOrderInfo(@Nullable String subject,@Nullable String out_trade_no,@Nullable String total_amount,@Nullable String product_code,String body,String timeout_express){
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append("\"subject\":\"");
        sb.append(subject);
        sb.append("\",");
        sb.append("\"out_trade_no\":\"");
        sb.append(out_trade_no);
        sb.append("\",");
        sb.append("\"total_amount\":\"");
        sb.append(total_amount);
        sb.append("\",");
        sb.append("\"product_code\":\"");
        sb.append(product_code);
        sb.append("\",");
        if(!TextUtils.isEmpty(body)){
            sb.append("\"body\":\"");
            sb.append(body);
            sb.append("\",");
        }
        if(!TextUtils.isEmpty(timeout_express)){
            sb.append("\"timeout_express\":\"");
            sb.append(timeout_express);
            sb.append("\"}");
        }else{
            sb.append("\"timeout_express\":\"");
            sb.append(DEFAULT_TIMEOUT_EXPRESS);
            sb.append("\"}");
        }
        return sb.toString();
    }

    public Map<String, String> buildOrderParamMap(boolean rsa2) {
        Map<String, String> keyValues = new HashMap<String, String>();

        keyValues.put("app_id", appId);

//        keyValues.put("biz_content", "{\"timeout_express\":\"30m\",\"product_code\":\"QUICK_MSECURITY_PAY\",\"total_amount\":\""+ total_amount +"\",\"subject\":\""+ subject +"\",\"body\":\""+ body +"\",\"out_trade_no\":\"" + out_trade_no +  "\"}");
        keyValues.put("biz_content", getOrderInfo(subject,out_trade_no,total_amount,PRODUCT_CODE,body,timeout_express));

        keyValues.put("charset", "utf-8");

        keyValues.put("method", "alipay.trade.app.pay");

        keyValues.put("sign_type", rsa2 ? "RSA2" : "RSA");

        keyValues.put("timestamp", "2016-07-29 16:55:53");

        keyValues.put("version", "1.0");

        return keyValues;
    }

    public String buildOrderParam(Map<String, String> map) {
        List<String> keys = new ArrayList<String>(map.keySet());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size() - 1; i++) {
            String key = keys.get(i);
            String value = map.get(key);
            sb.append(buildKeyValue(key, value, true));
            sb.append("&");
        }

        String tailKey = keys.get(keys.size() - 1);
        String tailValue = map.get(tailKey);
        sb.append(buildKeyValue(tailKey, tailValue, true));

        return sb.toString();
    }

    private String buildKeyValue(String key, String value, boolean isEncode) {
        StringBuilder sb = new StringBuilder();
        sb.append(key);
        sb.append("=");
        if (isEncode) {
            try {
                sb.append(URLEncoder.encode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                sb.append(value);
            }
        } else {
            sb.append(value);
        }
        return sb.toString();
    }

    public void pay(){
        if (TextUtils.isEmpty(appId) || (TextUtils.isEmpty(RSA2_PRIVATE) && TextUtils.isEmpty(RSA_PRIVATE))) {
            new AlertDialog.Builder(activity).setTitle("警告").setMessage("需要配置APPID | RSA_PRIVATE")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                            dialoginterface.dismiss();
                        }
                    }).show();
            return;
        }

        boolean rsa2 = (RSA2_PRIVATE.length() > 0);

        String orderParam = buildOrderParam(buildOrderParamMap(rsa2));
        String privateKey = rsa2 ? RSA2_PRIVATE : RSA_PRIVATE;
        String sign = getSign(buildOrderParamMap(rsa2), privateKey, rsa2);
        final String orderInfo = orderParam + "&" + sign;
        Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                PayTask alipay = new PayTask(activity);
                Map<String, String> result = alipay.payV2(orderInfo, true);
                Log.i("msp", result.toString());

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    /**
     * 对支付参数信息进行签名
     *
     * @param map
     *            待签名授权信息
     *
     * @return
     */
    public String getSign(Map<String, String> map, String rsaKey, boolean rsa2) {
        List<String> keys = new ArrayList<String>(map.keySet());
        // key排序
        Collections.sort(keys);

        StringBuilder authInfo = new StringBuilder();
        for (int i = 0; i < keys.size() - 1; i++) {
            String key = keys.get(i);
            String value = map.get(key);
            authInfo.append(buildKeyValue(key, value, false));
            authInfo.append("&");
        }

        String tailKey = keys.get(keys.size() - 1);
        String tailValue = map.get(tailKey);
        authInfo.append(buildKeyValue(tailKey, tailValue, false));

        String oriSign = sign(authInfo.toString(), rsaKey, rsa2);
        String encodedSign = "";

        try {
            encodedSign = URLEncoder.encode(oriSign, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "sign=" + encodedSign;
    }


    /**
     * 要求外部订单号必须唯一。
     * @return
     */
    private static String getOutTradeNo() {
        SimpleDateFormat format = new SimpleDateFormat("MMddHHmmss", Locale.getDefault());
        Date date = new Date();
        String key = format.format(date);

        Random r = new Random();
        key = key + r.nextInt();
        key = key.substring(0, 15);
        return key;
    }

    private String getAlgorithms(boolean rsa2) {
        return rsa2 ? SIGN_SHA256RSA_ALGORITHMS : SIGN_ALGORITHMS;
    }

    public String sign(String content, String privateKey, boolean rsa2) {
        try {
            PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(
                    Base64.decode(privateKey));
            KeyFactory keyf = KeyFactory.getInstance(ALGORITHM);
            PrivateKey priKey = keyf.generatePrivate(priPKCS8);

            java.security.Signature signature = java.security.Signature
                    .getInstance(getAlgorithms(rsa2));

            signature.initSign(priKey);
            signature.update(content.getBytes(DEFAULT_CHARSET));

            byte[] signed = signature.sign();

            return Base64.encode(signed);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
