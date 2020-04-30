/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xliic.ci.audit.model.api.ApiCollections;
import com.xliic.ci.audit.model.api.ErrorMessage;
import com.xliic.ci.audit.model.api.Maybe;
import com.xliic.ci.audit.Logger;
import com.xliic.ci.audit.model.api.Api;
import com.xliic.ci.audit.model.api.ApiCollection;
import com.xliic.ci.audit.model.assessment.AssessmentResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.config.RequestConfig;

public class Client {
    private static String proxyHost;
    private static int proxyPort;
    private static String userAgent;

    public static void setUserAgent(String userAgent) {
        Client.userAgent = userAgent;
    }

    public static void setProxy(String proxyHost, int proxyPort) {
        Client.proxyHost = proxyHost;
        Client.proxyPort = proxyPort;
    }

    public static Maybe<RemoteApi> createApi(String collectionId, String name, String json, String apiKey,
            Logger logger) throws IOException {
        HttpPost request = new HttpPost(ClientConstants.PLATFORM_URL + "/api/v1/apis");

        HttpEntity data = MultipartEntityBuilder
                .create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addBinaryBody("specfile",
                        json.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON, "swagger.json")
                .addTextBody("name", name).addTextBody("cid", collectionId).build();
        request.setEntity(data);

        Maybe<Api> api = new ProxyClient<Api>(request, apiKey, Api.class, logger).execute();
        if (api.isError()) {
            return new Maybe<RemoteApi>(api.getError());

        }
        return new Maybe<RemoteApi>(new RemoteApi(api.getResult().desc.id, ApiStatus.freshApiStatus()));
    }

    public static Maybe<RemoteApi> updateApi(String apiId, String json, String apiKey, Logger logger)
            throws IOException {
        // read api status first
        Maybe<ApiStatus> status = readApiStatus(apiId, apiKey, logger);
        if (status.isError()) {
            new Maybe<RemoteApi>(status.getError());
        }
        // update the api
        HttpPut request = new HttpPut(ClientConstants.PLATFORM_URL + "/api/v1/apis/" + apiId);
        String encodedJson = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        request.setEntity(
                new StringEntity(String.format("{\"specfile\": \"%s\"}", encodedJson), ContentType.APPLICATION_JSON));
        Maybe<String> result = new ProxyClient<String>(request, apiKey, String.class, logger).execute();
        if (result.isError()) {
            return new Maybe<RemoteApi>(result.getError());
        }
        return new Maybe<RemoteApi>(new RemoteApi(apiId, status.getResult()));
    }

    public static Maybe<String> deleteApi(String apiId, String apiKey, Logger logger) throws IOException {
        HttpDelete request = new HttpDelete(String.format("%s/api/v1/apis/%s", ClientConstants.PLATFORM_URL, apiId));
        return new ProxyClient<String>(request, apiKey, String.class, logger).execute();
    }

    public static Maybe<AssessmentResponse> readAssessment(Maybe<RemoteApi> api, String apiKey, Logger logger)
            throws ClientProtocolException, IOException {
        if (api.isError()) {
            return new Maybe<AssessmentResponse>(api.getError());
        }
        HttpGet request = new HttpGet(
                ClientConstants.PLATFORM_URL + "/api/v1/apis/" + api.getResult().apiId + "/assessmentreport");
        ProxyClient<AssessmentResponse> client = new ProxyClient<AssessmentResponse>(request, apiKey,
                AssessmentResponse.class, logger);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        while (Duration.between(start, now).toMillis() < ClientConstants.ASSESSMENT_MAX_WAIT) {
            Maybe<ApiStatus> status = readApiStatus(api.getResult().apiId, apiKey, logger);

            // check if assessment is ready, or bail out with the error
            if (status.isOk() && status.getResult().isProcessed
                    && status.getResult().lastAssessment.isAfter(api.getResult().previousStatus.lastAssessment)) {
                return client.execute();
            } else if (status.isError()) {
                return new Maybe<AssessmentResponse>(status.getError());
            }

            // sleep if assessment is not yet ready
            try {
                Thread.sleep(ClientConstants.ASSESSMENT_RETRY);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            now = LocalDateTime.now();
        }

        return new Maybe<AssessmentResponse>(
                new ErrorMessage("Timed out waiting for audit result for API ID: " + api.getResult().apiId));
    }

    public static Maybe<ApiStatus> readApiStatus(String apiId, String apiKey, Logger logger) throws IOException {
        HttpGet request = new HttpGet(ClientConstants.PLATFORM_URL + "/api/v1/apis/" + apiId);
        Maybe<Api> result = new ProxyClient<Api>(request, apiKey, Api.class, logger).execute();
        if (result.isError()) {
            return new Maybe<ApiStatus>(result.getError());
        }
        return new Maybe<ApiStatus>(
                new ApiStatus(result.getResult().assessment.isProcessed, result.getResult().assessment.last));
    }

    public static Maybe<ApiCollection> listCollection(String collectionId, String apiKey, Logger logger)
            throws IOException {
        HttpGet request = new HttpGet(
                String.format("%s/api/v1/collections/%s/apis", ClientConstants.PLATFORM_URL, collectionId));
        return new ProxyClient<ApiCollection>(request, apiKey, ApiCollection.class, logger).execute();
    }

    public static Maybe<ApiCollections> listCollections(String apiKey, Logger logger) throws IOException {
        HttpGet request = new HttpGet(ClientConstants.PLATFORM_URL + "/api/v1/collections");
        return new ProxyClient<ApiCollections>(request, apiKey, ApiCollections.class, logger).execute();
    }

    public static Maybe<ApiCollections.ApiCollection> createCollection(String collectionName, String apiKey,
            Logger logger) throws IOException {
        HttpPost request = new HttpPost(ClientConstants.PLATFORM_URL + "/api/v1/collections");
        request.setEntity(new StringEntity(String.format("{\"name\": \"%s\", \"isShared\": false}", collectionName),
                ContentType.APPLICATION_JSON));
        return new ProxyClient<ApiCollections.ApiCollection>(request, apiKey, ApiCollections.ApiCollection.class,
                logger).execute();
    }

    static class ProxyClient<T> {
        private java.lang.Class<T> contentClass;
        private Logger logger;
        private HttpRequestBase request;
        private String apiKey;

        ProxyClient(HttpRequestBase request, String apiKey, Class<T> contentClass, Logger logger) {
            this.request = request;
            this.apiKey = apiKey;
            this.contentClass = contentClass;
            this.logger = logger;
        }

        Maybe<T> execute() throws IOException {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = null;
            configureRequest(request, apiKey, logger);

            try {
                response = httpClient.execute(request);
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();

                    if (contentClass.equals(String.class)) {
                        return new Maybe<T>((T) EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
                    } else {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.registerModule(new JavaTimeModule());
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        return new Maybe<T>(mapper.readValue(entity.getContent(), contentClass));
                    }
                }

                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (status == 409 && responseBody.contains("limit reached")) {
                    return new Maybe<T>(new ErrorMessage(String.format(
                            "You have reached your maximum number of APIs. Please sign into %s and upgrade your account.",
                            ClientConstants.PLATFORM_URL)));
                } else if (status == 403) {
                    return new Maybe<T>(new ErrorMessage(
                            "Received 'Forbidden 403' response. Check that your API IDs are correct and API Token has required permissions: "
                                    + responseBody));
                } else if (status == 401) {
                    return new Maybe<T>(new ErrorMessage(
                            "Received 'Unauthorized 401' response. Check that the API token is correct: "
                                    + responseBody));
                }
                return new Maybe<T>(
                        new ErrorMessage(String.format("HTTP Request: %s %s failed with unexpected status code %s",
                                request.getMethod(), request.getURI(), status)));
            } finally {
                if (response != null) {
                    response.close();
                }
                httpClient.close();
            }
        }

        private static HttpHost getProxyHost() {
            if (Client.proxyHost != null) {
                return new HttpHost(Client.proxyHost, Client.proxyPort, "http");
            }
            return null;
        }

        private static void configureRequest(HttpRequestBase request, String apiKey, Logger logger) {
            request.setHeader("Accept", "application/json");
            request.setHeader("X-API-KEY", apiKey);
            if (Client.userAgent != null) {
                request.setHeader("User-Agent", Client.userAgent);
            }

            HttpHost proxy = getProxyHost();
            if (proxy != null) {
                logger.log("Using proxy server: " + proxy);
                RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
                request.setConfig(config);
            }
        }
    }
}
