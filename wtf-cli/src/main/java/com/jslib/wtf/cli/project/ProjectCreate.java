package com.jslib.wtf.cli.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.jslib.wtf.cli.ExitCode;
import com.jslib.wtf.cli.Task;
import com.jslib.wtf.cli.TemplateProcessor;
import com.jslib.wtf.cli.TemplateType;

import js.util.Strings;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create named project into current directory.")
public class ProjectCreate extends Task {
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

		Path homeDir = files.getPath(getHome());
		Path templateDir = homeDir.resolve("template/project");

		Map<String, String> variables = new HashMap<>();
		variables.put("technology", console.input("technology", config.get("project.technology")));
		String type = console.input("project type");

		Path templateFile = templateDir.resolve(Strings.concat(type, ".zip"));
		if (!files.exists(templateFile)) {
			console.print("Missing template %s.", templateFile);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		variables.put("author", console.input("developer name", config.get("user.name")));
		variables.put("package", console.input("package name", projectName));
		variables.put("package-path", variables.get("package").replace('.', '/'));
		variables.put("build", console.input("build directory", "build"));
		variables.put("title", console.input("site title", projectName));
		variables.put("description", console.input("project short description", projectName));
		variables.put("locale", console.input("list of comma separated locale", "en"));

		files.createDirectory(projectDir);
		template.setTargetDir(projectDir.toFile());
		template.setVerbose(verbose);
		template.exec(TemplateType.project, type, variables);

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
