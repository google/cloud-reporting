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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Unit tests for {@link MetricsUtils} class.
 */
@RunWith(EasyMockRunner.class)
public class MetricsUtilsTest {
  @Mock
  private Random mockRandom;

  @Test
  public void testCreateVirtualPageTitle() {
    // We use a sorted map to ensure that the order of the entries is
    // consistent for simpler testing. Normally clients don't care about order.
    Map<String, String> metadata = new TreeMap<>();
    metadata.put("key1", "value1");
    metadata.put("key2", "value2");

    // Test escaping behavior
    metadata.put("key3,=\\", "value3,=\\");

    String result = MetricsUtils.buildVirtualPageTitle(metadata);
    assertThat(result).isEqualTo(
        "key1=value1,key2=value2,key3\\,\\=\\\\=value3\\,\\=\\\\");
  }

  @Test
  public void testCreateVirtualPageName() {
    String result = MetricsUtils.buildVirtualPageName("testEventType",
        Optional.of("testObjectType"), "testEventName");
    assertThat(result).isEqualTo(
        "/virtual/testEventType/testObjectType/testEventName");
  }

  @Test
  public void testCreateVirtualPageNameNoObjectType() {
    String result = MetricsUtils.buildVirtualPageName("testEventType",
        Optional.<String>absent(), "testEventName");
    assertThat(result).isEqualTo(
        "/virtual/testEventType/testEventName");
  }

  @Test
  public void testCreateCombinedEventType() {
    String result = MetricsUtils.buildCombinedType("testEventType", Optional.of("testObjectType"));
    assertThat(result).isEqualTo("testEventType/testObjectType");
  }

  @Test
  public void testCreateCombinedEventTypeNoObjectType() {
    String result = MetricsUtils.buildCombinedType("testEventType", Optional.<String>absent());
    assertThat(result).isEqualTo("testEventType");
  }

  @Test
  public void testBuildParametersFull() {
    expect(mockRandom.nextLong()).andReturn(12345L).anyTimes();
    replay(mockRandom);
    List<NameValuePair> result = MetricsUtils.buildParameters(
        "testAnalyticsId", /* analyticsId */
        "testClientId", /* client ID */
        "testVirtualPageName", /* virtualPageName */
        "testVirtualPageTitle", /* virtualPageTitle */
        "testEventType", /* eventType */
        "testEventName", /* eventName */
        true, /* isUserSignedIn */
        true, /* isUserInternal */
        Optional.of(true), /* isUserTrialEligible */
        Optional.of("testProjectNumberHash"), /* projectNumberHash */
        Optional.of("testBillingIdHash"), /* billingIdHash */
        Optional.of("testClientHostname"), /* clientHostname */
        mockRandom);
    verify(mockRandom);

    // Verify static values.
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_PROTOCOL, "1"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_TYPE, "pageview"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_IS_NON_INTERACTIVE, "0"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_IS_VIRTUAL, "1"));

    // Verify dynamic values.
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_USER_SIGNED_IN, "1"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_USER_INTERNAL, "1"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_USER_TRIAL_ELIGIBLE, "1"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_PROPERTY_ID,
            "testAnalyticsId"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_EVENT_TYPE,
            "testEventType"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_EVENT_NAME,
            "testEventName"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_EVENT_TYPE,
            "testEventType"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_EVENT_NAME,
            "testEventName"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_CLIENT_ID,
            "testClientId"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_HOSTNAME,
            "testClientHostname"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_PROJECT_NUM_HASH,
            "testProjectNumberHash"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_BILLING_ID_HASH,
            "testBillingIdHash"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_PAGE,
            "testVirtualPageName"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_PAGE_TITLE,
            "testVirtualPageTitle"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_CACHEBUSTER,
            "12345"));
  }

  @Test
  public void testBuildParametersSkipOptional() {
    expect(mockRandom.nextLong()).andReturn(12345L).anyTimes();
    replay(mockRandom);
    List<NameValuePair> result = MetricsUtils.buildParameters(
        "testAnalyticsId", /* analyticsId */
        "testClientId", /* client ID */
        "testVirtualPageName", /* virtualPageName */
        "", /* virtualPageTitle */
        "testEventType", /* eventType */
        "testEventName", /* eventName */
        false, /* isUserSignedIn */
        false, /* isUserInternal */
        Optional.<Boolean>absent(), /* isUserTrialEligible */
        Optional.<String>absent(), /* projectNumberHash */
        Optional.<String>absent(), /* billingIdHash */
        Optional.<String>absent(), /* clientHostname */
        mockRandom);
    verify(mockRandom);

    // Verify static values.
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_PROTOCOL, "1"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_TYPE, "pageview"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_IS_NON_INTERACTIVE, "0"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_IS_VIRTUAL, "1"));

    // Verify dynamic values.
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_USER_SIGNED_IN, "0"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_USER_INTERNAL, "0"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_PROPERTY_ID,
            "testAnalyticsId"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_EVENT_TYPE,
            "testEventType"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_EVENT_NAME,
            "testEventName"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_EVENT_TYPE,
            "testEventType"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_EVENT_NAME,
            "testEventName"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_CLIENT_ID,
            "testClientId"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_PAGE,
            "testVirtualPageName"));
    assertThat(result).contains(
        new BasicNameValuePair(MetricsUtils.PARAM_CACHEBUSTER,
            "12345"));
    for (NameValuePair pair : result) {
      // Check that the optional/empty parameters are skipped.
      assertThat(pair.getName()).isNotEqualTo(MetricsUtils.PARAM_USER_TRIAL_ELIGIBLE);
      assertThat(pair.getName()).isNotEqualTo(MetricsUtils.PARAM_PAGE_TITLE);
      assertThat(pair.getName()).isNotEqualTo(MetricsUtils.PARAM_PROJECT_NUM_HASH);
      assertThat(pair.getName()).isNotEqualTo(MetricsUtils.PARAM_BILLING_ID_HASH);
      assertThat(pair.getName()).isNotEqualTo(MetricsUtils.PARAM_HOSTNAME);
    }
  }

  @Test
  public void testBuildPostBodyFull() throws Exception {
    expect(mockRandom.nextLong()).andReturn(12345L).anyTimes();
    replay(mockRandom);
    Event event = Event.builder()
        .setName("testEventName")
        .setType("testEventType")
        .setClientId("testClientId")
        .setIsUserSignedIn(true)
        .setIsUserInternal(true)
        .setIsUserTrialEligible(true)
        .setClientHostname("testClientHostname")
        .setObjectType("testObjectType")
        .setProjectNumberHash("testProjectNumberHash")
        .setBillingIdHash("testBillingIdHash")
        .addMetadata("key1,", "value1=\\")
        .build();
    UrlEncodedFormEntity entity = MetricsUtils.buildPostBody(event,
        "testAnalyticsId", mockRandom);
    verify(mockRandom);
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        entity.getContent(), StandardCharsets.UTF_8));

    // We don't want to do an exact comparison to a specific string, because
    // we want the test to be order-agnostic.
    // Create a new list to ensure remove() is supported without making assumptions about Splitter.
    ArrayList<String> body = new ArrayList<>(Splitter.on("&").splitToList(reader.readLine()));

    // Verify static values.
    assertContainsAndRemove(body, "t=pageview");
    assertContainsAndRemove(body, "v=1");
    assertContainsAndRemove(body, "ni=0");
    assertContainsAndRemove(body, "cd21=1");

    // Verify dynamic values.
    assertContainsAndRemove(body, "z=12345");
    assertContainsAndRemove(body, "tid=testAnalyticsId");
    assertContainsAndRemove(body, "cd31=testProjectNumberHash");
    assertContainsAndRemove(body, "cd18=testBillingIdHash");
    assertContainsAndRemove(body, "cd17=1");
    assertContainsAndRemove(body, "cd16=1");
    assertContainsAndRemove(body, "cd22=1");
    assertContainsAndRemove(body, "cd19=testEventType%2FtestObjectType");
    assertContainsAndRemove(body, "cd20=testEventName");
    assertContainsAndRemove(body, "cid=testClientId");
    assertContainsAndRemove(body, "dh=testClientHostname");

    // Verify encoding.
    assertContainsAndRemove(body,
        "dp=%2Fvirtual%2FtestEventType%2FtestObjectType%2FtestEventName");
    assertContainsAndRemove(body, "dt=key1%5C%2C%3Dvalue1%5C%3D%5C%5C");

    // Verify that the above values are the only values sent.
    assertThat(body).isEmpty();
  }

  @Test
  public void testBuildPostBodyMinimal() throws Exception {
    expect(mockRandom.nextLong()).andReturn(12345L).anyTimes();
    replay(mockRandom);
    Event event = Event.builder()
        .setName("testEventName")
        .setType("testEventType")
        .setClientId("testClientId")
        .build();
    UrlEncodedFormEntity entity = MetricsUtils.buildPostBody(event,
        "testAnalyticsId", mockRandom);
    verify(mockRandom);
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        entity.getContent(), StandardCharsets.UTF_8));

    // We don't want to do an exact comparison to a specific string, because
    // we want the test to be order-agnostic.
    // Create a new list to ensure remove() is supported without making assumptions about Splitter.
    ArrayList<String> body = new ArrayList<>(Splitter.on("&").splitToList(reader.readLine()));

    // Verify static values.
    assertContainsAndRemove(body, "t=pageview");
    assertContainsAndRemove(body, "v=1");
    assertContainsAndRemove(body, "ni=0");
    assertContainsAndRemove(body, "cd21=1");

    // Verify dynamic values.
    assertContainsAndRemove(body, "z=12345");
    assertContainsAndRemove(body, "tid=testAnalyticsId");
    assertContainsAndRemove(body, "cd17=0");
    assertContainsAndRemove(body, "cd16=0");
    assertContainsAndRemove(body, "cd19=testEventType");
    assertContainsAndRemove(body, "cd20=testEventName");
    assertContainsAndRemove(body, "cid=testClientId");

    // Verify encoding.
    assertContainsAndRemove(body, "dp=%2Fvirtual%2FtestEventType%2FtestEventName");

    // Verify that the above values are the only values sent.
    assertThat(body).isEmpty();
  }

  /**
   * Helper method to check that the given value is present in the list, then remove that value
   * from the list.
   */
  private static void assertContainsAndRemove(ArrayList<String> list, String value)
      throws Exception {
    assertThat(list).contains(value);
    list.remove(value);
  }
}
