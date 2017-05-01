package org.springframework.cloud.release.internal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.apache.maven.model.Model;
import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.pom.TestPomReader;
import org.springframework.cloud.release.internal.pom.TestUtils;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class PomUpdateAcceptanceTests {

	TestPomReader testPomReader = new TestPomReader();
	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	File temporaryFolder;

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_update_all_versions_for_a_release_train() throws Exception {
		ReleaserProperties releaserProperties = releaserProperties();
		releaserProperties.getFixedVersions().put("checkstyle", "100.0.0.RELEASE");
		ProjectPomUpdater projectPomUpdater = new ProjectPomUpdater(releaserProperties);
		Projects projects = projectPomUpdater.retrieveVersionsFromSCRelease();

		projectPomUpdater
				.updateProjectFromSCRelease(new File(this.temporaryFolder, "/spring-cloud-sleuth"), projects);

		then(this.temporaryFolder).exists();
		Model rootPom = this.testPomReader.readPom(tmpFile("/spring-cloud-sleuth/pom.xml"));
		Model depsPom = this.testPomReader.readPom(tmpFile("/spring-cloud-sleuth/spring-cloud-sleuth-dependencies/pom.xml"));
		Model corePom = this.testPomReader.readPom(tmpFile("/spring-cloud-sleuth/spring-cloud-sleuth-core/pom.xml"));
		Model zipkinStreamPom = this.testPomReader.readPom(tmpFile("/spring-cloud-sleuth/spring-cloud-sleuth-samples/spring-cloud-sleuth-sample-zipkin-stream/pom.xml"));
		then(rootPom.getVersion()).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		then(rootPom.getProperties())
				.containsEntry("spring-cloud-build.version","1.3.1.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-commons.version","1.2.0.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-stream.version","Chelsea.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-netflix.version","1.3.0.BUILD-SNAPSHOT")
				.containsEntry("checkstyle.version","100.0.0.RELEASE");
		then(depsPom.getVersion()).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		then(depsPom.getParent().getVersion()).isEqualTo("1.3.1.BUILD-SNAPSHOT");
		then(corePom.getParent().getVersion()).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		then(zipkinStreamPom.getParent().getVersion()).isEqualTo("1.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_fail_when_after_updating_a_release_version_there_still_is_a_snapshot_version() throws Exception {
		ReleaserProperties releaserProperties = branchReleaserProperties();
		ProjectPomUpdater projectPomUpdater = new ProjectPomUpdater(releaserProperties);
		Projects projects = projectPomUpdater.retrieveVersionsFromSCRelease();

		BDDAssertions.thenThrownBy(() ->
			projectPomUpdater
					.updateProjectFromSCRelease(new File(this.temporaryFolder, "/spring-cloud-sleuth-with-unmatched-property"), projects)
		).hasMessageContaining("<spring-cloud-unmatched.version>0.6.0.BUILD-SNAPSHOT</spring-cloud-unmatched.version>");
	}

	@Test
	public void should_not_update_a_project_that_is_not_on_the_list() throws Exception {
		ReleaserProperties releaserProperties = releaserProperties();
		ProjectPomUpdater projectPomUpdater = new ProjectPomUpdater(releaserProperties);
		File beforeProcessing = pom("/projects/project/");
		Projects projects = projectPomUpdater.retrieveVersionsFromSCRelease();

		projectPomUpdater.updateProjectFromSCRelease(tmpFile("/project/"), projects);

		then(this.temporaryFolder).exists();
		File afterProcessing = tmpFile("/project/pom.xml");
		then(asString(beforeProcessing)).isEqualTo(asString(afterProcessing));
	}

	private ReleaserProperties releaserProperties() throws URISyntaxException {
		ReleaserProperties releaserProperties = new ReleaserProperties();
		releaserProperties.getGit().setSpringCloudReleaseGitUrl(file("/projects/spring-cloud-release/").toURI().getPath());
		return releaserProperties;
	}

	private ReleaserProperties branchReleaserProperties() throws URISyntaxException {
		ReleaserProperties releaserProperties = releaserProperties();
		releaserProperties.getPom().setBranch("vCamden.SR4");
		return releaserProperties;
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(PomUpdateAcceptanceTests.class.getResource(relativePath).toURI());
	}

	private File pom(String relativePath) throws URISyntaxException {
		return new File(new File(PomUpdateAcceptanceTests.class.getResource(relativePath).toURI()), "pom.xml");
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}
}
