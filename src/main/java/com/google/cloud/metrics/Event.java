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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

/**
 * Encapsulates an event to be reported.
 */
@Immutable
public class Event {
  private final String name;
  private final String type;
  private final String clientId;
  private final boolean isUserSignedIn;
  private final boolean isUserInternal;
  private final Optional<Boolean> isUserTrialEligible;
  private final Optional<String> objectType;
  private final Optional<String> projectNumberHash;
  private final Optional<String> billingIdHash;
  private final Optional<String> clientHostname;
  private final ImmutableMap<String, String> metadata;

  /**
   * Should only be invoked by {@link #Builder#build()}
   */
  Event(String name, String type, String clientId, boolean isUserSignedIn, boolean isUserInternal,
      Optional<Boolean> isUserTrialEligible, Optional<String> objectType,
      Optional<String> projectNumberHash, Optional<String> billingIdHash,
      Optional<String> clientHostname, ImmutableMap<String, String> metadata) {
    this.name = checkNotNull(name);
    this.type = checkNotNull(type);
    this.clientId = checkNotNull(clientId);
    this.isUserSignedIn = isUserSignedIn;
    this.isUserInternal = isUserInternal;
    this.isUserTrialEligible = checkNotNull(isUserTrialEligible);
    this.objectType = checkNotNull(objectType);
    this.projectNumberHash = checkNotNull(projectNumberHash);
    this.billingIdHash = checkNotNull(billingIdHash);
    this.clientHostname = checkNotNull(clientHostname);
    this.metadata = checkNotNull(metadata);
  }

  /**
   * Creates a new {@link #Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Event name.
   */
  public String name() {
    return name;
  }

  /**
   * Event type.
   */
  public String type() {
    return type;
  }

  /**
   * Client ID. This should be a UUID per Google Analytics spec. Should never include PII.
   * This value should remain constant over multiple requests from a single client.
   */
  public String clientId() {
    return clientId;
  }

  /**
   * Whether the user is signed in.
   */
  public boolean isUserSignedIn() {
    return isUserSignedIn;
  }

  /**
   * Whether the user is internal.
   */
  public boolean isUserInternal() {
    return isUserInternal;
  }

  /**
   * Whether the user is free-trial eligible, if known.
   */
  public Optional<Boolean> isUserTrialEligible() {
    return isUserTrialEligible;
  }

  /**
   * Optional object type.
   */
  public Optional<String> objectType() {
    return objectType;
  }

  /**
   * Optional hashed numeric project ID.
   */
  public Optional<String> projectNumberHash() {
    return projectNumberHash;
  }

  /**
   * Optional hashed billing ID.
   */
  public Optional<String> billingIdHash() {
    return billingIdHash;
  }

  /**
   * Optional client hostname - the hostname where the event occurred.
   */
  public Optional<String> clientHostname() {
    return clientHostname;
  }

  /**
   * Metadata map.
   */
  public Map<String, String> metadata() {
    return metadata;
  }

  /**
   * Constructs an event to be reported.
   */
  public static class Builder {
    private ImmutableMap.Builder<String, String> mapBuilder =
        new ImmutableMap.Builder<String, String>();
    private String name = null;
    private String type = null;
    private String clientId = null;
    private boolean isUserSignedIn = false;
    private boolean isUserInternal = false;
    private Optional<Boolean> isUserTrialEligible = Optional.absent();
    private Optional<String> objectType = Optional.absent();
    private Optional<String> projectNumberHash = Optional.absent();
    private Optional<String> billingIdHash = Optional.absent();
    private Optional<String> clientHostname = Optional.absent();

    /**
     * Should never be explicitly invoked.
     */
    Builder() {}

    /**
     * @param name must not be null.
     */
    public Builder setName(String name) {
      this.name = checkNotNull(name);
      return this;
    }
    /**
     * @param type must not be null.
     */
    public Builder setType(String type) {
      this.type = checkNotNull(type);
      return this;
    }
    /**
     * @param clientId must not be null.
     */
    public Builder setClientId(String clientId) {
      this.clientId = checkNotNull(clientId);
      return this;
    }
    public Builder setIsUserSignedIn(boolean isUserSignedIn) {
      this.isUserSignedIn = isUserSignedIn;
      return this;
    }
    public Builder setIsUserInternal(boolean isUserInternal) {
      this.isUserInternal = isUserInternal;
      return this;
    }
    public Builder setIsUserTrialEligible(boolean isUserTrialEligible) {
      this.isUserTrialEligible = Optional.of(isUserTrialEligible);
      return this;
    }
    /**
     * Clears the free-trial eligibility state.
     */
    public Builder clearIsUserTrialEligible() {
      this.isUserTrialEligible = Optional.absent();
      return this;
    }
    /**
     * @param hostname must not be null.
     */
    public Builder setClientHostname(String hostname) {
      this.clientHostname = Optional.of(hostname);
      return this;
    }
    /**
     * Clears the client hostname, using {@link Optional#absent()} instead.
     */
    public Builder clearClientHostname() {
      this.clientHostname = Optional.absent();
      return this;
    }
    /**
     * @param objectType must not be null.
     */
    public Builder setObjectType(String objectType) {
      this.objectType = Optional.of(objectType);
      return this;
    }
    /**
     * Clears the object type, using {@link Optional#absent()} instead.
     */
    public Builder clearObjectType() {
      this.objectType = Optional.absent();
      return this;
    }
    /**
     * @param projectNumberHash must not be null.
     */
    public Builder setProjectNumberHash(String projectNumberHash) {
      this.projectNumberHash = Optional.of(projectNumberHash);
      return this;
    }
    /**
     * Clears the numeric project ID hash, using {@link Optional#absent()} instead.
     */
    public Builder clearProjectNumberHash() {
      this.projectNumberHash = Optional.absent();
      return this;
    }
    /**
     * @param billingIdHash must not be null.
     */
    public Builder setBillingIdHash(String billingIdHash) {
      this.billingIdHash = Optional.of(billingIdHash);
      return this;
    }
    /**
     * Clears the billing ID hash, using {@link Optional#absent()} instead.
     */
    public Builder clearBillingIdHash() {
      this.billingIdHash = Optional.absent();
      return this;
    }

    /**
     * Adds a key-value pair to the metadata. If the key is already present, existing data is
     * overwritten.
     */
    public Builder addMetadata(String key, String value) {
      mapBuilder.put(checkNotNull(key), checkNotNull(value));
      return this;
    }

    /**
     * Constructs the event object.
     */
    public Event build() {
      checkState(name != null, "build() method invoked without setting a name");
      checkState(type != null, "build() method invoked without setting a type");
      checkState(clientId != null, "build() method invoked without setting a clientId");
      return new Event(name, type, clientId, isUserSignedIn, isUserInternal, isUserTrialEligible,
          objectType, projectNumberHash, billingIdHash, clientHostname, mapBuilder.build());
    }
  }
}
