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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.cloud.release.internal.pom.SpringCloudConstants.BOOT_STARTER_ARTIFACT_ID;
import static org.springframework.cloud.release.internal.pom.SpringCloudConstants.BUILD_ARTIFACT_ID;
import static org.springframework.cloud.release.internal.pom.SpringCloudConstants.CLOUD_DEPENDENCIES_ARTIFACT_ID;

/**
 * Represents versions taken out from Spring Cloud Release pom
 *
 * @author Marcin Grzejszczak
 */
class Versions {

	private static final String SPRING_BOOT_PROJECT_NAME = "spring-boot";
	static final Versions EMPTY_VERSION = new Versions("");

	String bootVersion;
	String scBuildVersion;
	Set<Project> projects = new HashSet<>();

	Versions(String bootVersion) {
		this.bootVersion = bootVersion;
		this.projects.add(new Project(SPRING_BOOT_PROJECT_NAME, bootVersion));
		this.projects.add(new Project(BOOT_STARTER_ARTIFACT_ID, bootVersion));
	}

	Versions(String scBuildVersion, Set<Project> projects) {
		this.scBuildVersion = scBuildVersion;
		this.projects.add(new Project(BUILD_ARTIFACT_ID, scBuildVersion));
		this.projects.add(new Project(CLOUD_DEPENDENCIES_ARTIFACT_ID, scBuildVersion));
		this.projects.addAll(projects);
	}

	Versions(String bootVersion, String scBuildVersion, Set<Project> projects) {
		this.bootVersion = bootVersion;
		this.scBuildVersion = scBuildVersion;
		this.projects.add(new Project(SPRING_BOOT_PROJECT_NAME, bootVersion));
		this.projects.add(new Project(BOOT_STARTER_ARTIFACT_ID, bootVersion));
		this.projects.add(new Project(BUILD_ARTIFACT_ID, scBuildVersion));
		this.projects.add(new Project(CLOUD_DEPENDENCIES_ARTIFACT_ID, scBuildVersion));
		this.projects.addAll(projects);
	}

	Versions(Set<ProjectVersion> versions) {
		this.bootVersion = versions.stream().filter(projectVersion -> SPRING_BOOT_PROJECT_NAME.equals(projectVersion.projectName))
				.findFirst().orElseThrow(() -> new IllegalStateException("Boot Version is Missing")).version;
		this.scBuildVersion = versions.stream().filter(projectVersion -> BUILD_ARTIFACT_ID.equals(projectVersion.projectName))
				.findFirst().orElseThrow(() -> new IllegalStateException("Spring Cloud Build Version is Missing")).version;
		this.projects = versions.stream()
				.map(projectVersion -> new Project(projectVersion.projectName, projectVersion.version))
				.collect(Collectors.toSet());
	}

	String versionForProject(String projectName) {
		return this.projects.stream()
				.filter(project -> nameMatches(projectName, project))
				.findFirst()
				.orElse(Project.EMPTY_PROJECT)
				.version;
	}

	boolean shouldBeUpdated(String projectName) {
		return this.projects.stream()
				.anyMatch(project -> nameMatches(projectName, project));
	}

	boolean shouldSetProperty(Properties properties) {
		return this.projects.stream()
				.anyMatch(project -> properties.containsKey(project.name + ".version"));
	}

	Projects toProjectVersions() {
		return new Projects(this.projects.stream().map(project -> new ProjectVersion(project.name, project.version))
				.collect(Collectors.toSet()));
	}

	private boolean nameMatches(String projectName, Project project) {
		if (project.name.equals(projectName)) {
			return true;
		}
		boolean containsParent = projectName.endsWith("-parent");
		if (!containsParent) {
			return false;
		}
		String withoutParent = projectName.substring(0, projectName.indexOf("-parent"));
		return project.name.equals(withoutParent);
	}

	boolean isSnapshot() {
		return this.projects.stream().anyMatch(project -> project.version.endsWith("BUILD-SNAPSHOT"));
	}

	Versions setVersion(String projectName, String version) {
		switch (projectName) {
			case SPRING_BOOT_PROJECT_NAME:
			case BOOT_STARTER_ARTIFACT_ID:
				this.bootVersion = version;
				remove(SPRING_BOOT_PROJECT_NAME);
				remove(BOOT_STARTER_ARTIFACT_ID);
				this.projects.add(new Project(SPRING_BOOT_PROJECT_NAME, version));
				this.projects.add(new Project(BOOT_STARTER_ARTIFACT_ID, version));
				break;
			case BUILD_ARTIFACT_ID:
			case CLOUD_DEPENDENCIES_ARTIFACT_ID:
				this.scBuildVersion = version;
				remove(BUILD_ARTIFACT_ID);
				remove(CLOUD_DEPENDENCIES_ARTIFACT_ID);
				this.projects.add(new Project(BUILD_ARTIFACT_ID, version));
				this.projects.add(new Project(CLOUD_DEPENDENCIES_ARTIFACT_ID, version));
				break;
			default:
				remove(projectName);
				this.projects.add(new Project(projectName, version));
		}
		return this;
	}

	private void remove(String expectedProjectName) {
		this.projects.removeIf(project -> expectedProjectName.equals(project.name));
	}

	@Override public String toString() {
		return "Spring Boot Version=[" + this.bootVersion + ']' + "\nSpring Cloud Build Version=["
				+ this.scBuildVersion + ']' + "\nProjects=\n\t" + this.projects.stream().map(Object::toString).collect(
				Collectors.joining("\n\t"));
	}
}

/**
 * @author Marcin Grzejszczak
 */
class Project {

	static Project EMPTY_PROJECT = new Project("", "");

	final String name;
	final String version;

	Project(String name, String version) {
		this.name = name;
		this.version = version;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Project project = (Project) o;
		if (this.name != null ? !this.name.equals(project.name) : project.name != null)
			return false;
		return this.version != null ?
				this.version.equals(project.version) :
				project.version == null;
	}

	@Override public int hashCode() {
		int result = this.name != null ? this.name.hashCode() : 0;
		result = 31 * result + (this.version != null ? this.version.hashCode() : 0);
		return result;
	}

	@Override public String toString() {
		return "name=[" + this.name + "], version=[" + this.version + ']';
	}
}