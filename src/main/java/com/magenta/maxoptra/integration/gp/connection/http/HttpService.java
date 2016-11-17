package com.magenta.maxoptra.integration.gp.connection.http;

import com.magenta.maxoptra.integration.gp.application.ArchiveMessageComponent;
import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import com.magenta.maxoptra.integration.gp.configuration.GeoPalConf;
import com.magenta.maxoptra.integration.gp.connection.http.exeption.FailRequestException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

@Named
public class HttpService {

    private static final Logger log = LoggerFactory.getLogger(HttpService.class);

    private String BASE_URL;

    @PostConstruct
    void init() throws Exception {
        BASE_URL = configurationUtils.getConfigurations().geopalBaseUrl;
    }

    @Inject
    private ConfigurationUtils configurationUtils;

    @Inject
    ArchiveMessageComponent archiveMessageComponent;

    public String sendXML(String xml, String url) throws JAXBException {
        log.info("Send orders api request to Maxoptra");
        //ResponseRecord responseRecord = new ResponseRecord();
        String err = "";
        try {
            HttpClient client = createHttpClient();
            URI uri = URI.create(url);
            HttpPost post = new HttpPost(uri);
            StringEntity entity = new StringEntity(xml, ContentType.create("application/xml", Consts.UTF_8));
            post.setEntity(entity);
            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() != 200) {
                return "Error "
                        + response.getStatusLine().getStatusCode()
                        + ", url: " + url + " " + response.getStatusLine().getReasonPhrase();
            }

            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            err = "Error when send orders xml to Maxoptra" + e.getMessage();
            log.error("Error when send orders xml to Maxoptra", e);
        }
        return err;
    }

    public String get(String url, Map<String, String> params, GeoPalConf geoPalConf) throws Exception {
        log.info("Get request: " + url);
        StringBuilder archiveUrl = new StringBuilder(BASE_URL + url);

        HttpClient client = createHttpClient();

        UriBuilder uriBuilder = UriBuilder.fromUri(BASE_URL + url);//URI.create(url);
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                uriBuilder.queryParam(param.getKey(), param.getValue());
                archiveUrl.append("  " + param.getKey() + "=" + param.getValue());
            }
        }

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        for (Map.Entry<String, String> header : getHeaders("get", url, geoPalConf).entrySet()) {
            httpGet.setHeader(header.getKey(), header.getValue());
        }

        HttpResponse response = client.execute(httpGet);

        if (response.getStatusLine().getStatusCode() != 200) {
            log.error("Error Get request");
            throw new FailRequestException("Error with request to Geopal. Error "
                    + response.getStatusLine().getStatusCode()
                    + ", url: " + BASE_URL + url + " " + response.getStatusLine().getReasonPhrase());
        }

        String result = EntityUtils.toString(response.getEntity());
        archiveMessageComponent.add(archiveUrl.toString(), result);
        return result;
    }

    public String post(String url, Map<String, String> params, GeoPalConf geopal) throws Exception {
        log.info("Post request: " + url);
        StringBuilder archiveUrl = new StringBuilder(BASE_URL + url);

        HttpClient client = createHttpClient();

        List<BasicNameValuePair> list = new ArrayList<>();
        UriBuilder uriBuilder = UriBuilder.fromUri(BASE_URL + url);//URI.create(url);
        for (Map.Entry<String, String> param : params.entrySet()) {
            list.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            archiveUrl.append(" " + param.getKey() + "=" + param.getValue());
        }

        HttpPost httpPost = new HttpPost(uriBuilder.build());
        for (Map.Entry<String, String> header : getHeaders("post", url, geopal).entrySet()) {
            httpPost.setHeader(header.getKey(), header.getValue());
        }

        httpPost.setEntity(new UrlEncodedFormEntity(list, "UTF-8"));

        HttpResponse response = client.execute(httpPost);

        if (response.getStatusLine().getStatusCode() != 200) {
            log.error("Error Post request");
            throw new FailRequestException("Error with request GeoPal. Error "
                    + response.getStatusLine().getStatusCode()
                    + ", url: " + BASE_URL + url + " " + response.getStatusLine().getReasonPhrase());
        }

        String result = EntityUtils.toString(response.getEntity());
        archiveMessageComponent.add(archiveUrl.toString(), result);
        return result;

    }

    private HttpClient createHttpClient() throws FileNotFoundException, JAXBException {
        int timeout = configurationUtils.getConfigurations().httpTimeout;
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(timeout * 1000).build();
        HttpClient hc = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        return hc;
    }

    private Map<String, String> getHeaders(String method, String uri, GeoPalConf geoPalConf) {
        String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        String timestamp = format.format(Calendar.getInstance().getTime());

        String signature = getSignature(method + uri + geoPalConf.userId + timestamp, geoPalConf.apiKey);

        Map<String, String> headers = new HashMap<>();
        headers.put("GEOPAL-SIGNATURE", signature);
        headers.put("GEOPAL-EMPLOYEEID", geoPalConf.userId);
        headers.put("GEOPAL-TIMESTAMP", timestamp);
        return headers;
    }

    private String getSignature(String signtext, String privateKey) {
        String test = getHMAC256(privateKey, signtext);
        return Base64.encodeBase64String(test.getBytes());
    }

    public String getHMAC256(String pwd, String inputdata) {
        String temp = null;
        SecretKeySpec keySpec = new SecretKeySpec(pwd.getBytes(), "HmacSHA256");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            // update method adds the given byte to the Mac's input data.
            mac.update(inputdata.getBytes());
            byte[] m = mac.doFinal();
            // The base64-encoder in Commons Codec
            temp = new String(Hex.encodeHex(m));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

}
