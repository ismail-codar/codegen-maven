package codegen;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import codegen.util.jaxb.SwaggerType;

@Mojo(name = "generate", defaultPhase = GENERATE_SOURCES, requiresDependencyResolution = TEST)
public class Plugin extends AbstractMojo {

	@Parameter
	private SwaggerType swagger;

	/**
	 * The Maven project.
	 */
	@Parameter(property = "project", required = true, readonly = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException {
		Generator generator = new Generator();
		try {
			System.out.println("************************* Codegen started *************************");
			generator.generate(swagger);
			System.out.println("************************* Codegen ended *************************");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
