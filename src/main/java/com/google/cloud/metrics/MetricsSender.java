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

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Random;

import javax.inject.Inject;

/**
 * Sends metric reports via the Google Analytics API.
 *
 * @see AsyncMetricsSender
 */
public class MetricsSender {
  private final String analyticsId;
  private final Random random;
  private final CloseableHttpClient client;
  // These objects are immutable, so we can just hold on to one instance.
  private static final BasicResponseHandler RESPONSE_HANDLER = new BasicResponseHandler();

  /**
   * Non-injected constructor for clients that do not want to use dependency injection.
   *
   * @param analyticsId The Google Analytics ID to which reports will be sent.
   */
  public MetricsSender(String analyticsId) {
    this(analyticsId, HttpClientBuilder.create(), new Random());
  }

  /**
   * @param analyticsId The Google Analytics ID to which reports will be sent.
   * @param clientBuilder A builder for HTTP client objects. This injection is for testing; most
   *        clients should never need to bind anything but the default implementation here.
   * @param random A random number generator for cache-busting. This injection is for testing; most
   *        clients should never need to bind anything but the default implementation here.
   */
  @Inject
  public MetricsSender(@GoogleAnalyticsId String analyticsId, HttpClientBuilder clientBuilder,
      Random random) {
    this.analyticsId = analyticsId;
    this.random = random;
    this.client = clientBuilder.setUserAgent(MetricsUtils.USER_AGENT).build();
  }

  /**
   * Translates an encapsulated event into a request to the Google Analytics API and sends the
   * resulting request.
   */
  public void send(Event event) throws MetricsException {
    HttpPost request = new HttpPost(MetricsUtils.GA_ENDPOINT_URL);
    request.setEntity(MetricsUtils.buildPostBody(event, analyticsId, random));
    try {
      client.execute(request, RESPONSE_HANDLER);
    } catch (IOException e) {
      // All errors in the request execution, including non-2XX HTTP codes,
      // will throw IOException or one of its subclasses.
      throw new MetricsCommunicationException("Problem sending request to server", e);
    }
  }
}
