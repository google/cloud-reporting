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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.escape.CharEscaperBuilder;
import com.google.common.escape.Escaper;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Utility class to construct requests used by the metric sender classes.
 */
class MetricsUtils {
  private static final Logger LOGGER = Logger.getLogger(MetricsUtils.class.getName());
  private static final String VIRTUAL_PAGE_PREFIX = "/virtual";
  // Escapes {, = \} as {\, \= \\}.
  private static final Escaper METADATA_ESCAPER = new CharEscaperBuilder()
      .addEscape(',', "\\,")
      .addEscape('=', "\\=")
      .addEscape('\\', "\\\\")
      .toEscaper();

  // Values from https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide
  // Parameters
  @VisibleForTesting static final String PARAM_CACHEBUSTER = "z";
  @VisibleForTesting static final String PARAM_CLIENT_ID = "cid";
  @VisibleForTesting static final String PARAM_IS_NON_INTERACTIVE = "ni";
  @VisibleForTesting static final String PARAM_PAGE = "dp";
  @VisibleForTesting static final String PARAM_PAGE_TITLE = "dt";
  @VisibleForTesting static final String PARAM_HOSTNAME = "dh";
  @VisibleForTesting static final String PARAM_PROPERTY_ID = "tid";
  @VisibleForTesting static final String PARAM_PROTOCOL = "v";
  @VisibleForTesting static final String PARAM_TYPE = "t";

  // Custom parameters
  @VisibleForTesting static final String PARAM_PROJECT_NUM_HASH = "cd31";
  @VisibleForTesting static final String PARAM_USER_SIGNED_IN = "cd17";
  @VisibleForTesting static final String PARAM_USER_INTERNAL = "cd16";
  @VisibleForTesting static final String PARAM_USER_TRIAL_ELIGIBLE = "cd22";
  @VisibleForTesting static final String PARAM_BILLING_ID_HASH = "cd18";
  @VisibleForTesting static final String PARAM_EVENT_TYPE = "cd19";
  @VisibleForTesting static final String PARAM_EVENT_NAME = "cd20";
  @VisibleForTesting static final String PARAM_IS_VIRTUAL = "cd21";
  // Values
  @VisibleForTesting static final String VALUE_TYPE_PAGEVIEW = "pageview";
  @VisibleForTesting static final String VALUE_TRUE = "1";
  @VisibleForTesting static final String VALUE_FALSE = "0";

  /** User agent associated with metric-reporting HTTP requests. */
  static final String USER_AGENT = "Automated";
  /** Google Analytics URL to receive metrics reports. */
  static final String GA_ENDPOINT_URL = "https://www.google-analytics.com/collect";

  /**
   * Utility class should never be instantiated.
   */
  private MetricsUtils() {}

  /**
   * Creates the body of a POST request encoding the metric report for a single {@link Event}.
   *
   * @param event The event to report.
   * @param analyticsId the Google Analytics ID to receive the report.
   * @param random Random number generator to use for cache busting.
   * @return A URL-encoded POST request body, in the format expected by Google Analytics.
   */
  static UrlEncodedFormEntity buildPostBody(Event event, String analyticsId, Random random) {
    checkNotNull(event);
    checkNotNull(analyticsId);
    checkNotNull(random);

    String virtualPageName = buildVirtualPageName(event.type(),
        event.objectType(), event.name());
    String virtualPageTitle = buildVirtualPageTitle(event.metadata());
    String combinedEventType = buildCombinedType(event.type(), event.objectType());
    return new UrlEncodedFormEntity(buildParameters(
        analyticsId,
        event.clientId(),
        virtualPageName,
        virtualPageTitle,
        combinedEventType,
        event.name(),
        event.isUserSignedIn(),
        event.isUserInternal(),
        event.isUserTrialEligible(),
        event.projectNumberHash(),
        event.billingIdHash(),
        event.clientHostname(),
        random),
        StandardCharsets.UTF_8);
  }

  /**
   * Combines event and object type into a single type string.
   *
   * @param eventType type or category of the reporting event.
   * @param objectType optional type of the object this event applies to.
   * @return Combined type string.
   */
  static String buildCombinedType(String eventType, Optional<String> objectType) {
    return Joiner.on("/").skipNulls().join(eventType, objectType.orNull());
  }

  /**
   * Creates a virtual page name (relative URL) from an event type, object type and event name.
   *
   * @param eventType type or category of the reporting event.
   * @param objectType optional type of the object this event applies to.
   * @param eventName name of the reporting event.
   * @return Virtual page name constructed for this event.
   */
  static String buildVirtualPageName(String eventType, Optional<String> objectType,
      String eventName) {
    return Joiner.on("/").skipNulls()
        .join(VIRTUAL_PAGE_PREFIX, eventType, objectType.orNull(), eventName);
  }

  /**
   * Creates a virtual page title from a set of metadata key-value pairs.
   *
   * @param metadata Metadata map.
   * @return Virtual page title constructed from the metadata, escaped as required by the format.
   */
  static String buildVirtualPageTitle(Map<String, String> metadata) {
    checkNotNull(metadata);
    List<String> escapedMetadata = new ArrayList<>();
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      escapedMetadata.add(METADATA_ESCAPER.escape(entry.getKey()) + "="
          + METADATA_ESCAPER.escape(entry.getValue()));
    }
    return Joiner.on(",").join(escapedMetadata);
  }

  /**
   * Creates the parameters required to record the Google Analytics event.
   *
   * @see https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters
   *
   * @param analyticsId Google Analytics ID to receive the report data.
   * @param clientId Client ID - must not include PII.
   * @param virtualPageName Full relative URL of the virtual page for the event.
   * @param virtualPageTitle Title of the virtual page for the event.
   * @param eventType Full event type string.
   * @param eventName Event name.
   * @param isUserSignedIn Whether the event involves a signed-in user.
   * @param isUserInternal Whether the event involves an internal user.
   * @param isUserTrialEligible Whether the event involves a user eligible for free trial.
   *        Use {@link Optional#absent()} if not known.
   * @param projectNumberHash Hashed numeric project ID.
   * @param billingIdHash Hashed billing ID, if applicable.
   * @param clientHostname Hostname of the client where the event occurred, if any.
   * @param random Random number generator to use for cache busting.
   * @return immutable list of parameters as name-value pairs.
   */
  static ImmutableList<NameValuePair> buildParameters(
      String analyticsId,
      String clientId,
      String virtualPageName,
      String virtualPageTitle,
      String eventType,
      String eventName,
      boolean isUserSignedIn,
      boolean isUserInternal,
      Optional<Boolean> isUserTrialEligible,
      Optional<String> projectNumberHash,
      Optional<String> billingIdHash,
      Optional<String> clientHostname,
      Random random) {
    checkNotNull(analyticsId);
    checkNotNull(clientId);
    checkNotNull(virtualPageTitle);
    checkNotNull(virtualPageName);
    checkNotNull(eventType);
    checkNotNull(eventName);
    checkNotNull(projectNumberHash);
    checkNotNull(billingIdHash);
    checkNotNull(clientHostname);
    checkNotNull(random);

    ImmutableList.Builder<NameValuePair> listBuilder = new ImmutableList.Builder<>();

    // Analytics information
    // Protocol version
    listBuilder.add(new BasicNameValuePair(PARAM_PROTOCOL, "1"));
    // Analytics ID to send report to
    listBuilder.add(new BasicNameValuePair(PARAM_PROPERTY_ID, analyticsId));
    // Always report as a pageview
    listBuilder.add(new BasicNameValuePair(PARAM_TYPE, VALUE_TYPE_PAGEVIEW));
    // Always report as interactive
    listBuilder.add(new BasicNameValuePair(PARAM_IS_NON_INTERACTIVE, VALUE_FALSE));
    // Add a randomly generated cache buster
    listBuilder.add(new BasicNameValuePair(PARAM_CACHEBUSTER, Long.toString(random.nextLong())));

    // Event information
    listBuilder.add(new BasicNameValuePair(PARAM_EVENT_TYPE, eventType));
    listBuilder.add(new BasicNameValuePair(PARAM_EVENT_NAME, eventName));
    if (clientHostname.isPresent() && !clientHostname.get().isEmpty()) {
      listBuilder.add(new BasicNameValuePair(PARAM_HOSTNAME, clientHostname.get()));
    }

    // User information
    listBuilder.add(new BasicNameValuePair(PARAM_CLIENT_ID, clientId));
    if (projectNumberHash.isPresent() && !projectNumberHash.get().isEmpty()) {
      listBuilder.add(new BasicNameValuePair(PARAM_PROJECT_NUM_HASH, projectNumberHash.get()));
    }
    if (billingIdHash.isPresent() && !billingIdHash.get().isEmpty()) {
      listBuilder.add(new BasicNameValuePair(PARAM_BILLING_ID_HASH, billingIdHash.get()));
    }
    listBuilder.add(new BasicNameValuePair(PARAM_USER_SIGNED_IN, toValue(isUserSignedIn)));
    listBuilder.add(new BasicNameValuePair(PARAM_USER_INTERNAL, toValue(isUserInternal)));
    if (isUserTrialEligible.isPresent()) {
      listBuilder.add(new BasicNameValuePair(PARAM_USER_TRIAL_ELIGIBLE,
          toValue(isUserTrialEligible.get())));
    }

    // Virtual page information
    listBuilder.add(new BasicNameValuePair(PARAM_IS_VIRTUAL, VALUE_TRUE));
    listBuilder.add(new BasicNameValuePair(PARAM_PAGE, virtualPageName));
    if (!virtualPageTitle.isEmpty()) {
      listBuilder.add(new BasicNameValuePair(PARAM_PAGE_TITLE, virtualPageTitle));
    }

    return listBuilder.build();
  }

  /**
   * @return the value string associated with the given boolean value.
   */
  private static String toValue(boolean b) {
    return b ? VALUE_TRUE : VALUE_FALSE;
  }
}
