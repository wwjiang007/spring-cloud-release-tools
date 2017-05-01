package org.springframework.cloud.release.internal.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.cloud.release.internal.PomUpdateAcceptanceTests;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.TestPomReader;
import org.springframework.cloud.release.internal.pom.TestUtils;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectBuilderTests {

	TestPomReader reader = new TestPomReader();
	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	File temporaryFolder;

	@Before
	public void checkOs() throws Exception {
		Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
		this.temporaryFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects"), this.temporaryFolder);
	}

	@Test
	public void should_successfully_execute_a_command_when_after_running_there_is_no_html_file_with_unresolved_tag() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setBuildCommand("ls -al");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		builder.build();

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("resolved.log");
	}

	@Test
	public void should_throw_exception_when_after_running_there_is_an_html_file_with_unresolved_tag() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setBuildCommand("ls -al");
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		thenThrownBy(builder::build).hasMessageContaining("contains a tag that wasn't resolved properly");
	}

	@Test
	public void should_throw_exception_when_command_took_too_long_to_execute() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setBuildCommand("sleep 1");
		properties.getMaven().setWaitTimeInMinutes(0);
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		thenThrownBy(builder::build).hasMessageContaining("Process waiting time of [0] minutes exceeded");
	}

	@Test
	public void should_successfully_execute_a_deploy_command() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setDeployCommand("ls -al");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		builder.deploy();

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("resolved.log");
	}

	@Test
	public void should_throw_exception_when_deploy_command_took_too_long_to_execute() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setDeployCommand("sleep 1");
		properties.getMaven().setWaitTimeInMinutes(0);
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		thenThrownBy(builder::deploy).hasMessageContaining("Process waiting time of [0] minutes exceeded");
	}

	@Test
	public void should_successfully_execute_a_publish_docs_command() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setPublishDocsCommands(new String[] { "ls -al", "ls -al" });
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		TestProcessExecutor executor = executor(properties);
		ProjectBuilder builder = new ProjectBuilder(properties, executor);

		builder.publishDocs("");

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("resolved.log");
		then(executor.counter).isEqualTo(2);
	}

	@Test
	public void should_successfully_execute_a_publish_docs_command_and_substitute_the_version() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setPublishDocsCommands(new String[] { "echo '{{version}}'" });
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		TestProcessExecutor executor = executor(properties);
		ProjectBuilder builder = new ProjectBuilder(properties, executor);

		builder.publishDocs("1.1.0.RELEASE");

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("1.1.0.RELEASE");
	}

	@Test
	public void should_throw_exception_when_publish_docs_command_took_too_long_to_execute() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setPublishDocsCommands(new String[] { "sleep 1", "sleep 1" });
		properties.getMaven().setWaitTimeInMinutes(0);
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		thenThrownBy(() -> builder.publishDocs("")).hasMessageContaining("Process waiting time of [0] minutes exceeded");
	}

	@Test
	public void should_throw_exception_when_process_exits_with_invalid_code() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setBuildCommand("exit 1");
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, new ProcessExecutor(properties) {
			@Override Process startProcess(ProcessBuilder builder) throws IOException {
				return processWithInvalidExitCode();
			}
		});

		thenThrownBy(builder::build).hasMessageContaining("The process has exited with exit code [1]");
	}

	@Test
	public void should_successfully_execute_a_bump_versions_command() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.setWorkingDir(tmpFile("/spring-cloud-contract").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		builder.bumpVersions("2.3.4.BUILD-SNAPSHOT");

		File rootPom = tmpFile("/spring-cloud-contract/pom.xml");
		File tools = tmpFile("/spring-cloud-contract/spring-cloud-contract-tools/pom.xml");
		File converters = tmpFile("/spring-cloud-contract/spring-cloud-contract-tools/spring-cloud-contract-converters/pom.xml");
		then(this.reader.readPom(rootPom).getVersion()).isEqualTo("2.3.4.BUILD-SNAPSHOT");
		then(this.reader.readPom(tools).getParent().getVersion()).isEqualTo("2.3.4.BUILD-SNAPSHOT");
		then(this.reader.readPom(converters).getParent().getVersion()).isEqualTo("2.3.4.BUILD-SNAPSHOT");
	}

	private Process processWithInvalidExitCode() {
		return new Process() {
			@Override public OutputStream getOutputStream() {
				return null;
			}

			@Override public InputStream getInputStream() {
				return null;
			}

			@Override public InputStream getErrorStream() {
				return null;
			}

			@Override public int waitFor() throws InterruptedException {
				return 0;
			}

			@Override public int exitValue() {
				return 1;
			}

			@Override public void destroy() {

			}
		};
	}

	private TestProcessExecutor executor(ReleaserProperties properties) {
		return new TestProcessExecutor(properties);
	}

	class TestProcessExecutor extends ProcessExecutor {

		int counter = 0;

		TestProcessExecutor(ReleaserProperties properties) {
			super(properties);
		}

		@Override ProcessBuilder builder(String[] commands, String workingDir) {
			this.counter++;
			return super.builder(commands, workingDir)
					.redirectOutput(tmpFile("/builder/resolved/resolved.log"));
		}
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(PomUpdateAcceptanceTests.class.getResource(relativePath).toURI());
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

}