Google Cloud Metrics Library
==========================

This library wraps the creation and sending of HTTPS POST requests to the Google Analytics API for reporting end-user events, suitable for usage metrics. The event object model is tailored accordingly.

Usage
====

Create and hold an instance of MetricsSender or AsyncMetricsSender. This library uses Guice for dependency injection, in particular to inject the Google Analytics ID. A non-DI constructor is also available.

Whenever a user event occurs that needs to be recorded, create an Event object and invoke the send() method of your Sender object.

Each instance of MetricsSender and AsyncMetricsSender is expected to be long-lived; creating a new instance for each request imposes unnecessary overhead, especially in the case of AsyncMetricsSender.

To maintain user privacy, the fields of the Event object should never include personally identifying information.

Development
=========
`gradlew` is a script that invokes a wrapper to allow downloading and running
gradle on computers that do not have the package already installed. To build and
test your changes simply run:

    ./gradlew build -x sign
    ./gradlew test -x sign

This skips the signing step, which is required when releasing full versions.

License
------

	(The Apache v2 License)

    Copyright 2015 Google Inc. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
