package com.jslib.wtf.cli.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Home;
import com.jslib.commons.cli.Task;
import com.jslib.commons.cli.TemplateProcessor;

import js.util.Strings;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "project", description = "Create named project into current directory.")
public class CreateProject extends Task {
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Project name.", paramLabel = "name")
	private String projectName;

	private TemplateProcessor template = new TemplateProcessor();

	@Override
	protected ExitCode exec() throws IOException {
		Path workingDir = files.getWorkingDir();
		Path projectDir = workingDir.resolve(projectName);
		if (files.exists(projectDir)) {
			console.print("Project directory %s already existing.", projectDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}
		console.print("Creating project %s.", projectName);

		Path homeDir = files.getPath(Home.getPath());
		Path templateDir = homeDir.resolve("template/project");

		Map<String, String> variables = new HashMap<>();
		String technology = console.input("technology", Strings.join(files.getFileNames(templateDir), ", "));
		variables.put("technology", technology);
		String projectType = console.input("project type", Strings.join(files.getFileNames(templateDir.resolve(technology)), ", "));

		Path templateFile = templateDir.resolve(Strings.concat(technology, '/', projectType, ".zip"));
		if (!files.exists(templateFile)) {
			console.print("Missing template %s.", templateFile);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		variables.put("projectName", projectName);
		variables.put("author", console.input("developer name", config.get("user.name")));
		variables.put("package", console.input("package name"));
		variables.put("packagePath", variables.get("package").replace('.', '/'));
		variables.put("build", console.input("build directory", "build"));
		variables.put("display", console.input("site display", projectName));
		variables.put("description", console.input("project short description", projectName));
		variables.put("locale", console.input("list of comma separated locale", "en"));
		variables.put("runtimeName", console.input("runtime name", "test"));
		variables.put("runtimePort", console.input("runtime port", "8080"));

		files.createDirectory(projectDir);
		template.setTargetDir(projectDir.toFile());
		template.setVerbose(verbose);
		template.exec(templateFile.toFile(), variables);

		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	void setTemplate(TemplateProcessor template) {
		this.template = template;
	}

	void setName(String name) {
		this.projectName = name;
	}
}
