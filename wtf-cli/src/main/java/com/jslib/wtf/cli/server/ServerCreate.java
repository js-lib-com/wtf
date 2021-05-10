package com.jslib.wtf.cli.server;

import static java.lang.String.format;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;
import com.jslib.commons.cli.TemplateProcessor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "create", description = "Create server.")
public class ServerCreate extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-r", "--runtime-type" }, description = "Runtime template from ${wood.home}/template/runtime directory.", required = true)
	private String type;
	@Option(names = { "-p", "--port" }, description = "Listening port. Default: ${DEFAULT-VALUE}", defaultValue = "8080")
	private int port;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Unique runtime name. Default: project name.", arity = "0..1")
	private String name;

	private TemplateProcessor template = new TemplateProcessor();

	@Override
	protected ExitCode exec() throws Exception {
		File projectDir = files.getWorkingDir().toFile();
		if (name == null) {
			name = projectDir.getName();
		}
		console.print("Creating runtime %s...", name);

		File runtimeDir = new File(config.get("runtime.home", File.class), name);
		if (runtimeDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Runtime %s already existing.", runtimeDir));
		}

		Map<String, String> variables = new HashMap<>();
		variables.put("port", Integer.toString(port));

		template.setTargetDir(runtimeDir);
		template.setVerbose(verbose);
		template.exec("runtime", type, variables);

		config.put("runtime.name", name);
		config.put("runtime.port", port);
		return ExitCode.SUCCESS;
	}
}
