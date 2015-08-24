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

import static com.google.common.truth.Truth.assertThat;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.common.base.Splitter;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

/**
 * Unit tests for {@link AsyncMetricsSender} class.
 */
@RunWith(EasyMockRunner.class)
public class AsyncMetricsSenderTest {
  @Mock private Random mockRandom;
  @Mock private CloseableHttpAsyncClient mockClient;
  @Mock private HttpAsyncClientBuilder mockClientBuilder;

  @Before
  public void initMocks() {
    expect(mockClientBuilder.build()).andReturn(mockClient).anyTimes();
  }

  @Test
  public void testSendEvent() throws Exception {
    expect(mockRandom.nextLong()).andReturn(12345L);
    Event event = Event.builder()
        .setName("testEventName")
        .setType("testEventType")
        .setClientId("testClientId")
        .setClientHostname("testClientHostname")
        .setObjectType("testObjectType")
        .setProjectNumberHash("testProjectNumberHash")
        .addMetadata("key1,", "value1=\\")
        .build();

    Capture<HttpPost> request = new Capture<>();
    mockClient.start();
    expectLastCall();
    expect(mockClient.execute(capture(request),
        EasyMock.<FutureCallback>anyObject())).andReturn(null);

    replay(mockRandom, mockClient, mockClientBuilder);

    AsyncMetricsSender sender = new AsyncMetricsSender("testAnalyticsId",
        mockClientBuilder, mockRandom);
    sender.send(event);

    verify(mockRandom, mockClient, mockClientBuilder);

    assertThat(request.getValue().getURI().toString()).isEqualTo(
        MetricsUtils.GA_ENDPOINT_URL);

    // We don't want to do an exact comparison to a specific string, because
    // we want the test to be order-agnostic.
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        request.getValue().getEntity().getContent(), StandardCharsets.UTF_8));
    List<String> body = Splitter.on("&").splitToList(reader.readLine());

    // Verify static values.
    assertThat(body).contains("t=pageview");
    assertThat(body).contains("v=1");
    assertThat(body).contains("ni=0");

    // Verify dynamic values.
    assertThat(body).contains("z=12345");
    assertThat(body).contains("tid=testAnalyticsId");
    assertThat(body).contains("cd31=testProjectNumberHash");
    assertThat(body).contains("cid=testClientId");
    assertThat(body).contains("dh=testClientHostname");

    // Verify encoding.
    assertThat(body).contains(
        "dp=%2Fvirtual%2FtestEventType%2FtestObjectType%2FtestEventName");
    assertThat(body).contains("dt=key1%5C%2C%3Dvalue1%5C%3D%5C%5C");
  }
}
