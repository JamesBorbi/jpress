package io.jpress.module.article.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.opensearch.sdk.dependencies.org.apache.http.HttpResponse;
import com.aliyun.opensearch.sdk.dependencies.org.apache.http.client.config.RequestConfig;
import com.aliyun.opensearch.sdk.dependencies.org.apache.http.client.methods.HttpGet;
import com.aliyun.opensearch.sdk.dependencies.org.apache.http.client.methods.HttpPost;
import com.aliyun.opensearch.sdk.dependencies.org.apache.http.entity.ContentType;
import com.aliyun.opensearch.sdk.dependencies.org.apache.http.entity.StringEntity;
import com.aliyun.opensearch.sdk.dependencies.org.apache.http.impl.client.CloseableHttpClient;
import com.aliyun.opensearch.sdk.dependencies.org.apache.http.impl.client.HttpClients;
import com.aliyun.opensearch.sdk.dependencies.org.apache.http.util.EntityUtils;
import com.jfinal.kit.PathKit;
import com.jfinal.upload.UploadFile;
import io.jboot.utils.StrUtil;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;

/**
 * http工具
 */
public class HttpUrlUtil {


    private static final String ATTACHMENT_TARGET_PREFIX = "/attachment";

    /**
     * Post
     * 登陆成功后返回cookie
     * @param json
     * @param outPath
     * @return
     * @throws IOException
     */
    public static String getCookieLogin(String json, String outPath) throws IOException {
        URL newURL = new URL(outPath);
        HttpURLConnection conn = (HttpURLConnection) newURL.openConnection();
        conn.setRequestProperty("Connection","keep-alive");
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.connect();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(),"utf-8");
        out.write(String.valueOf(json.getBytes()));
        out.flush();
        out.close();
        InputStream inputStream = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
        reader.close();
        String headerName = null;
        StringBuilder myCookies = new StringBuilder();

        for(int i =1;(headerName= conn.getHeaderFieldKey(i))!=null;i++){
            if(headerName.equals("Set-Cookie")){
                String cookie = conn.getHeaderField(i);
                cookie = cookie.substring(0, cookie.indexOf(";"));
                String cookieName = cookie.substring(0, cookie.indexOf("="));
                String cookieValue = cookie.substring(cookie.indexOf("=") + 1, cookie.length());
                myCookies.append(cookieName + "=");
                myCookies.append(cookieValue);
            }
        }
        String cookie = myCookies.toString();
        System.out.println(cookie);
        return cookie;
    }

    public static void main(String[] args) throws IOException {
        getCookieByLogin("","");
    }
    /**
     * Post
     * 登陆成功后返回cookie
     * @param json
     * @param outPath
     * @return
     * @throws IOException
     */
    public static String getCookieByLogin(String content, String loginUrl) throws IOException {
        String result = "";
        try {
            URL url = new URL(loginUrl);
            //通过调用url.openConnection()来获得一个新的URLConnection对象，并且将其结果强制转换为HttpURLConnection.
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            //设置连接的超时值为30000毫秒，超时将抛出SocketTimeoutException异常
            urlConnection.setConnectTimeout(30000);
            //设置读取的超时值为30000毫秒，超时将抛出SocketTimeoutException异常
            urlConnection.setReadTimeout(30000);
            //将url连接用于输出，这样才能使用getOutputStream()。getOutputStream()返回的输出流用于传输数据
            urlConnection.setDoOutput(true);
            //设置通用请求属性为默认浏览器编码类型
            urlConnection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
            //getOutputStream()返回的输出流，用于写入参数数据。
            OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(content.getBytes());
            outputStream.flush();
            outputStream.close();
            //此时将调用接口方法。getInputStream()返回的输入流可以读取返回的数据。
            InputStream inputStream = urlConnection.getInputStream();
            byte[] data = new byte[1024];
            StringBuilder sb = new StringBuilder();
            //inputStream每次就会将读取1024个byte到data中，当inputSteam中没有数据时，inputStream.read(data)值为-1
//            while (inputStream.read(data) != -1) {
//                String s = new String(data, Charset.forName("utf-8"));
//                sb.append(s);
//            }
//            result = sb.toString();
            inputStream.close();

            //读取cookie
            String headerName = null;
            StringBuilder myCookies = new StringBuilder();

            for(int i =1;(headerName= urlConnection.getHeaderFieldKey(i))!=null;i++){
                if(headerName.equals("Set-Cookie")){
                    String cookie = urlConnection.getHeaderField(i);
                    cookie = cookie.substring(0, cookie.indexOf(";"));
                    String cookieName = cookie.substring(0, cookie.indexOf("="));
                    String cookieValue = cookie.substring(cookie.indexOf("=") + 1, cookie.length());
                    myCookies.append(cookieName + "=");
                    myCookies.append(cookieValue+"; ");
                }
            }
            result = myCookies.toString();
            System.out.println(result);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    /**
     * GET请求获取网页信息
     * @param gatsss //参数
     * @param url    //url
     * @param cookie  //登陆成功获取的cookie
     * @return
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    public static String doGetForJson(String gatsss, String url, String cookie){
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String result = null;
        try {
            if(StrUtil.isNotBlank(gatsss)){
                // 采用utf-8字符集
                String encode = URLEncoder.encode( gatsss, "UTF-8" );
                //转码后拼接url
                url+=encode;
            }
            HttpGet httpget = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom().
                    setConnectTimeout( 180 * 1000 ).setConnectionRequestTimeout( 180 * 1000 )
                    .setSocketTimeout( 180 * 1000 ).setRedirectsEnabled( true ).build();
            httpget.setConfig( requestConfig );
            httpget.setHeader( "Content-Type", "application/json" );
            httpget.setHeader("Cookie", cookie);
            HttpResponse response = httpClient.execute( httpget );

            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString( response.getEntity() );
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }
    }

    /**
     * post获取网页信息
     * @param url   //url
     * @param jsonParams  //参数
     */
    public JSONObject postEquipmentId(String url, String jsonParams) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        JSONObject jsonObject = null;
        HttpPost httpPost = new HttpPost( url );
        RequestConfig requestConfig = RequestConfig.custom().
                setConnectTimeout( 180 * 1000 ).setConnectionRequestTimeout( 180 * 1000 )
                .setSocketTimeout( 180 * 1000 ).setRedirectsEnabled( true ).build();
        httpPost.setConfig( requestConfig );
        httpPost.setHeader( "Content-Type", "application/json" );
        try {
            httpPost.setEntity( new StringEntity( jsonParams, ContentType.create( "application/json", "utf-8" ) ) );
            HttpResponse response = httpClient.execute( httpPost );
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                String result = EntityUtils.toString( response.getEntity() );
                jsonObject = JSONObject.parseObject(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return jsonObject;
        }
    }

    /**
     * 上传图片
     * @param url
     * @param cookie
     * @return
     */
    public static UploadFile doGetImg(String url, String cookie){
        CloseableHttpClient httpClient = HttpClients.createDefault();
        UploadFile uploadFile = null;
        try {
            // 采用utf-8字符集
//            String encode = URLEncoder.encode( gatsss, "UTF-8" );
            //转码后拼接url
//            url+=encode;
            HttpGet httpget = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom().
                    setConnectTimeout( 180 * 1000 ).setConnectionRequestTimeout( 180 * 1000 )
                    .setSocketTimeout( 180 * 1000 ).setRedirectsEnabled( true ).build();
            httpget.setConfig( requestConfig );
            httpget.setHeader( "Content-Type", "application/json" );
            httpget.setHeader("Cookie", cookie);
            HttpResponse response = httpClient.execute( httpget );

            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                String fielName = url.split("/")[url.split("/").length-1];

                String pathweb = PathKit.getWebRootPath() + ATTACHMENT_TARGET_PREFIX;
                FileOutputStream downloadFile = new FileOutputStream(pathweb+"/"+fielName);
                response.getEntity().writeTo(downloadFile);


                downloadFile.close();

                String parameterName = "files[]";
                String uploadPath = pathweb;
                String fileName = fielName;
                String originalFileName = fielName;
                String contentType = response.getEntity().getContentType().getValue();
                uploadFile = new UploadFile(parameterName,uploadPath,fileName,originalFileName,contentType);


            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return uploadFile;
        }
    }


}
