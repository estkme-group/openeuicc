package com.truphone.es9plus;

import com.truphone.util.LogStub;
import com.truphone.util.TextUtil;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class HttpRSPClient {
    private static final Logger LOG = Logger.getLogger(HttpRSPClient.class.getName());

    public HttpResponse clientRSPRequest(final String body,
                                         final String rspServerUrl,
                                         final String url) throws Exception {

        Pair<String, String> contentType = new Pair<>("Content-Type", "application/json");
        Pair<String, String> accept = new Pair<>("Accept", "application/json");
        Pair<String, String> userAgent = new Pair<>("User-Agent", "gsma-rsp-com.truphone.lpad");
        Pair<String, String> xAdminProtocol = new Pair<>("X-Admin-Protocol", "gsma/rsp/v2.2.0");

        return invoke("POST", body, rspServerUrl, url, Arrays.asList(contentType, accept, userAgent, xAdminProtocol));
    }

    public HttpResponse clientSimpleRequest(final String body,
                                            final String rspServerUrl,
                                            final  String url) throws Exception {

        Pair<String, String> contentType = new Pair<>("Content-type", "application/x-www-form-urlencoded");
        Pair<String, String> userAgent = new Pair<>("User-Agent", "gsma-rsp-com.truphone.lpad");
        Pair<String, String> xAdminProtocol = new Pair<>("X-Admin-Protocol", "gsma/rsp/v2.2.0");


        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - HttpRSPClient - clientSimpleRequest parameters - body : " +
                    body + " rspServerUrl:" + rspServerUrl + " contentType: " + contentType + " user agent: " + userAgent +
                    " xAdminProtocol: " + xAdminProtocol + " url: " + url);
        }

        return invoke("POST", body, rspServerUrl, url, Arrays.asList(contentType, userAgent, xAdminProtocol));
    }

    private HttpResponse invoke(final String method,
                        final String body,
                        final String rspServerUrl,
                        final String url,
                        final List<Pair<String, String>> headers) throws Exception {

        StringBuilder endpoint = new StringBuilder(rspServerUrl);
        HttpResponse httpResponse = new HttpResponse();

        if (TextUtil.isNotBlank(url)) {
            endpoint.append(url);
        }

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - HttpRSPClient - invoke URL: " + endpoint.toString());
        }

        URL urlResource = new URL(endpoint.toString());
        HttpURLConnection con = (HttpURLConnection) urlResource.openConnection();

        con.setDoInput(true);
        con.setDoOutput(true);
        con.setRequestMethod(method);
        con.setConnectTimeout(600000);
        con.setReadTimeout(600000);

        if (headers != null) {
            for (Pair<String, String> header : headers) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        OutputStream os = con.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

        writer.write(body);
        writer.flush();
        writer.close();

        os.close();

        httpResponse.setStatusCode(con.getResponseCode());
        httpResponse.setContent(new String(TextUtil.readInputStream(con.getInputStream()), StandardCharsets.UTF_8));

        return httpResponse;
    }

    private class Pair<T, E> {
        private T key;
        private E value;

        public Pair(T key, E value) {
            this.key = key;
            this.value = value;
        }

        public T getKey() {
            return key;
        }

        public void setKey(T key) {
            this.key = key;
        }

        public E getValue() {
            return value;
        }

        public void setValue(E value) {
            this.value = value;
        }
    }
}
