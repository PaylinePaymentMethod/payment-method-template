package com.payline.payment.template.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.payline.payment.template.bean.TemplateCaptureRequest;
import com.payline.payment.template.bean.TemplatePaymentRequest;
import com.payline.payment.template.bean.TemplatePaymentResponse;
import com.payline.payment.template.bean.TemplateRequest;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class TemplateHttpClient {
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String CONTENT_TYPE_KEY = "Content-Type";
    private static final String AUTHENTICATION_KEY = "Authorization";
    private static final String CONTENT_TYPE = "application/json";
    private HttpClient client;
    private Gson parser;


    public TemplateHttpClient() {
        this.parser = new GsonBuilder().create();

        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2 * 1000)
                .setConnectionRequestTimeout(3 * 1000)
                .setSocketTimeout(4 * 1000).build();

        final HttpClientBuilder builder = HttpClientBuilder.create();
        builder.useSystemProperties()
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCredentialsProvider(new BasicCredentialsProvider())
                .setSSLSocketFactory(new SSLConnectionSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory(), SSLConnectionSocketFactory.getDefaultHostnameVerifier()));
        this.client = builder.build();
    }

    public String getHost(boolean isSandbox) {
        return isSandbox ? TemplateCardConstants.SANDBOX_URL : TemplateCardConstants.PRODUCTION_URL;
    }

    public String createPath(String... path) {
        StringBuilder sb = new StringBuilder("/");
        if (path != null && path.length > 0) {
            for (String aPath : path) {
                sb.append(aPath).append("/");
            }
        }

        return sb.toString();
    }

    //
    private Header[] createHeaders(String authentication) {
        Header[] headers = new Header[2];
        headers[0] = new BasicHeader(CONTENT_TYPE_KEY, CONTENT_TYPE);
        headers[1] = new BasicHeader(AUTHENTICATION_KEY, authentication);
        return headers;
    }


    public HttpResponse doGet(String scheme, String host, String path, Header[] headers) throws IOException, URISyntaxException {

        final URI uri = new URIBuilder()
                .setScheme(scheme)
                .setHost(host)
                .setPath(path)
                .build();

        final HttpGet httpGetRequest = new HttpGet(uri);
        httpGetRequest.setHeaders(headers);
        return client.execute(httpGetRequest);
    }

    public HttpResponse doPost(String scheme, String host, String path, Header[] headers, String body) throws IOException, URISyntaxException {

        final URI uri = new URIBuilder()
                .setScheme(scheme)
                .setHost(host)
                .setPath(path)
                .build();

        final HttpPost httpPostRequest = new HttpPost(uri);
        httpPostRequest.setHeaders(headers);
        httpPostRequest.setEntity(new StringEntity(body));
        return client.execute(httpPostRequest);
    }


    public TemplatePaymentResponse initiate(TemplateRequest request, boolean isSandbox) throws IOException, URISyntaxException {
        String host = getHost(isSandbox);
        String path = createPath(TemplateCardConstants.PATH_VERSION, TemplateCardConstants.PATH);
        String jsonBody = parser.toJson(request);
        Header[] headers = createHeaders(request.getAuthenticationHeader());

        // do the request
        HttpResponse response = doPost(TemplateCardConstants.SCHEME, host, path, headers, jsonBody);
        String responseString = EntityUtils.toString(response.getEntity(), DEFAULT_CHARSET);

        // create object from Template response
        return parser.fromJson(responseString, TemplatePaymentResponse.class);
    }

    public TemplatePaymentResponse retrievePaymentData(TemplateCaptureRequest request, boolean isSandbox) throws IOException, URISyntaxException {
        String host = getHost(isSandbox);
        String path = createPath(TemplateCardConstants.PATH_VERSION, TemplateCardConstants.PATH, request.getPaymentId());
        Header[] headers = createHeaders(request.getAuthenticationHeader());

        // do the request
        HttpResponse response = doGet(TemplateCardConstants.SCHEME, host, path, headers);

        // create object from Template response
        String responseString = EntityUtils.toString(response.getEntity(), DEFAULT_CHARSET);
        return parser.fromJson(responseString, TemplatePaymentResponse.class);
    }

    public TemplatePaymentResponse capture(TemplateCaptureRequest request, boolean isSandbox) throws IOException, URISyntaxException {
        String host = getHost(isSandbox);
        String path = createPath(TemplateCardConstants.PATH_VERSION, TemplateCardConstants.PATH, request.getPaymentId(), TemplateCardConstants.PATH_CAPTURE);

        String body = "";
        Header[] headers = createHeaders(request.getAuthenticationHeader());

        // do the request
        HttpResponse response = doPost(TemplateCardConstants.SCHEME, host, path, headers, body);

        // create object from Template response
        String responseString = EntityUtils.toString(response.getEntity(), DEFAULT_CHARSET);
        return parser.fromJson(responseString, TemplatePaymentResponse.class);
    }

    public TemplatePaymentResponse refund(TemplatePaymentRequest request, boolean isSandbox) throws IOException, URISyntaxException {
        String host = getHost(isSandbox);
        String path = createPath(TemplateCardConstants.PATH_VERSION, TemplateCardConstants.PATH, request.getPaymentId(), TemplateCardConstants.PATH_REFUND);
        String jsonBody = parser.toJson(request);
        Header[] headers = createHeaders(request.getAuthenticationHeader());

        // do the request
        HttpResponse response = doPost(TemplateCardConstants.SCHEME, host, path, headers, jsonBody);

        // create object from Template response
        String responseString = EntityUtils.toString(response.getEntity(), DEFAULT_CHARSET);
        return parser.fromJson(responseString, TemplatePaymentResponse.class);
    }

}
