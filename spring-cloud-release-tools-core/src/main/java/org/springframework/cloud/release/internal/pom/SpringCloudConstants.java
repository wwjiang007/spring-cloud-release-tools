/*
 *  Copyright 2013-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.release.internal.pom;

/**
 * @author Marcin Grzejszczak
 */
final class SpringCloudConstants {
	static final String BOOT_STARTER_ARTIFACT_ID = "spring-boot-starter-parent";
	static final String CLOUD_DEPENDENCIES_ARTIFACT_ID = "spring-cloud-dependencies-parent";
	static final String BUILD_ARTIFACT_ID = "spring-cloud-build";

	private SpringCloudConstants() {
		throw new IllegalStateException("Don't instantiate a utility class");
	}

}
