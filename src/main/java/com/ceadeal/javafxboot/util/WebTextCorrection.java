package com.ceadeal.javafxboot.util;

import com.google.gson.JsonObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author huangHy
 * @description: TODO
 * @date 2022/1/2 14:31
 */
public class WebTextCorrection {
    private String requestUrl;
    private String apiKey;
    private String apiSecret;
    private String text;
    private String appid ;
    private String serviceId;
    public WebTextCorrection(String requestUrl, String apiKey, String apiSecret, String text, String appid, String serviceId) {
        this.requestUrl = requestUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.text = text;
        this.appid = appid;
        this.serviceId = serviceId;
    }
    public String buildRequetUrl(){
        return assembleRequestUrl(this.requestUrl,this.apiKey,this.apiSecret);
    }
    private String  buildParam() throws IOException {
        JsonObject req = new JsonObject();
        //平台参数
        JsonObject header = new JsonObject();
        header.addProperty("app_id",appid);
        header.addProperty("status",3);
        //功能参数
        JsonObject parameter = new JsonObject();
        JsonObject inputAcp = new JsonObject();
        JsonObject result = new JsonObject();
        //构建result段参数
        inputAcp.addProperty("encoding","utf8");
        inputAcp.addProperty("compress","raw");//raw,gzip
        inputAcp.addProperty("format","json");//plain,json,xml
        result.add("result", inputAcp);
        parameter.add(this.serviceId, result);
        //请求数据
        JsonObject payload = new JsonObject();
        JsonObject input = new JsonObject();
        input.addProperty("encoding","utf8"); //jpg:jpg格式,jpeg:jpeg格式,png:png格式,bmp:bmp格式
        input.addProperty("compress","raw");//raw,gzip
        input.addProperty("format","json");//plain,json,xml
        input.addProperty("status",3);   //3:一次性传完
        input.addProperty("text", Base64.getEncoder().encodeToString(this.text.getBytes("UTF-8"))); //文本数据，base64
        //System.out.println(Base64.getEncoder().encodeToString(this.text.getBytes()).length());
        payload.add("input",input);
        req.add("header",header);
        req.add("parameter",parameter);
        req.add("payload",payload);

        return req.toString();
    }

    private String makeRequest() throws Exception {
        URL realUrl = new URL(buildRequetUrl());
        //System.out.println("realUrl=>"+realUrl);
        URLConnection connection = realUrl.openConnection();
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-type","application/json");

        OutputStream out = httpURLConnection.getOutputStream();
        String params = buildParam();
        //System.out.println("params=>"+params);
        out.write(params.getBytes("UTF-8"));
        out.flush();
        InputStream is = null;
        try{
            is = httpURLConnection.getInputStream();
            System.out.println("code is "+httpURLConnection.getResponseCode()+";"+"message is "+httpURLConnection.getResponseMessage());
        }catch (Exception e){
            is = httpURLConnection.getErrorStream();
            byte[] resp = readAllBytesToString(is);// byte [] resp = is.readAllBytes(); // JDK8=>JDK10
            throw new Exception("make request error:"+"code is "+httpURLConnection.getResponseCode()+";"+httpURLConnection.getResponseMessage()+new String(resp));
        }
        byte[] resp = readAllBytesToString(is);// var resp = is.readAllBytes();
        return new String(resp);
    }

    public String doRequest() throws Exception {
        return this.makeRequest();
    }
    /**
     * JDK8=>JDK10
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public byte[] readAllBytesToString(InputStream inputStream) throws IOException {
        //StringBuffer sb = new StringBuffer();
        byte[] bytes = new byte[inputStream.available()];
        int z;
        while ((z = inputStream.read(bytes, 0, bytes.length)) != -1) {
            //java.lang.String s = new java.lang.String(bytes, "utf-8");
            //sb.append(s);
        }
        inputStream.close();
        return bytes;
    }
    public static String assembleRequestUrl(String requestUrl, String apiKey, String apiSecret) {
        URL url = null;
        // 替换调schema前缀 ，原因是URL库不支持解析包含ws,wss schema的url
        String  httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://","https://" );
        try {
            url = new URL(httpRequestUrl);
            //获取当前日期并格式化
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());
            //System.out.println("date=>"+date);

            String host = url.getHost();
            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").//
                    append("date: ").append(date).append("\n").//
                    append("POST ").append(url.getPath()).append(" HTTP/1.1");
            //System.out.println("builder=>"+builder);
            Charset charset = Charset.forName("UTF-8");
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = Base64.getEncoder().encodeToString(hexDigits);
            //System.out.println("sha=>"+sha);

            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
            //System.out.println("authorization=>"+authorization);
            String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            //System.out.println("authBase=>"+authBase);
            return String.format("%s?authorization=%s&host=%s&date=%s", requestUrl, URLEncoder.encode(authBase), URLEncoder.encode(host), URLEncoder.encode(date));
        } catch (Exception e) {
            throw new RuntimeException("assemble requestUrl error:"+e.getMessage());
        }
    }

    public static class ResponseData {
        private Header header;
        private PayLoad payload;
        public Header getHeader() {
            return header;
        }
        public PayLoad getPayLoad() {
            return payload;
        }
    }
    public static class Header {
        private int code;
        private String message;
        private String sid;
        public int getCode() {
            return code;
        }
        public String getMessage() {
            return message;
        }
        public String getSid() {
            return sid;
        }
    }
    public static class PayLoad {
        private Result result;
        public Result getResult() {
            return result;
        }
    }
    public static class Result {
        private String compress;
        private String encoding;
        private String format;
        private String text;
        public String getCompress() {
            return compress;
        }
        public String getEncoding() {
            return encoding;
        }
        public String getFormat() {
            return format;
        }
        public String getText() {
            return text;
        }
    }

}
