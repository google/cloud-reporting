/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.metrics;

import com.google.cloud.metrics.Annotations.GoogleAnalyticsId;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Sends metric reports asynchronously via the Google Analytics API. Note that this class creates
 * and maintains an asynchronous HTTP client. Therefore, clients should avoid creating many
 * instances and prefer to reuse a single instance for multiple metrics reports.
 *
 * @see MetricsSender
 */
public class AsyncMetricsSender {
  private static final Logger LOGGER = Logger.getLogger(AsyncMetricsSender.class.getName());

  private final String analyticsId;
  private final Random random;
  private final CloseableHttpAsyncClient client;
  // These objects are immutable, so we can just hold on to one instance.
  private static final BasicResponseHandler RESPONSE_HANDLER = new BasicResponseHandler();

  /**
   * Non-injected constructor for clients that do not want to use dependency injection.
   *
   * @param analyticsId The Google Analytics ID to which reports will be sent.
   */
  public AsyncMetricsSender(String analyticsId) {
    this(analyticsId, HttpAsyncClientBuilder.create(), new Random());
  }

  /**
   * @param analyticsId The Google Analytics ID to which reports will be sent.
   * @param clientBuilder A builder for HTTP client objects. This injection is for testing; most
   *        clients should never need to bind anything but the default implementation here.
   * @param random A random number generator for cache-busting. This injection is for testing; most
   *        clients should never need to bind anything but the default implementation here.
   */
  @Inject
  public AsyncMetricsSender(@GoogleAnalyticsId String analyticsId,
      HttpAsyncClientBuilder clientBuilder, Random random) {
    this.analyticsId = analyticsId;
    this.random = random;
    this.client = clientBuilder.setUserAgent(MetricsUtils.USER_AGENT).build();
    this.client.start();
  }

  /**
   * Translates an encapsulated event into a request to the Google Analytics API and asynchronously
   * sends the resulting request. Note that errors will only be caught in the asynchronous execution
   * thread and therefore will not result in an exception, unlike {@link MetricsSender}.
   */
  public void send(Event event) {
    HttpPost request = new HttpPost(MetricsUtils.GA_ENDPOINT_URL);
    request.setEntity(MetricsUtils.buildPostBody(event, analyticsId, random));
    client.execute(request, new FutureCallback<HttpResponse>() {
      @Override
      public void completed(HttpResponse result) {
        try {
          // Let BasicResponseHandler do the work of checking the status
          // code. The actual response body is irrelevant.
          RESPONSE_HANDLER.handleResponse(result);
        } catch (IOException e) {
          // For our purposes, this is exactly identical to a failure.
          failed(e);
        }
      }

      @Override
      public void failed(Exception ex) {
        // Can't really do anything but log the problem.
        LOGGER.warning("Problem sending request to server: " + ex);
      }

      @Override
      public void cancelled() {
        LOGGER.warning("Metrics-reporting HTTP request was cancelled.");
      }
    });
  }
}
