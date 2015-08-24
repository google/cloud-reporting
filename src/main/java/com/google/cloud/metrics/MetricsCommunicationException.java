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

import java.io.IOException;

/**
 * An exception raised when there is a problem communicating with the recording service.
 */
public class MetricsCommunicationException extends MetricsException {
  private static final long serialVersionUID = -5099045629448807064L;

  public MetricsCommunicationException(String msg, IOException cause) {
    super(msg, cause);
  }
}
