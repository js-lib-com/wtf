package com.jslib.wtf.cli.server;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;
import com.jslib.util.Files;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(name = "destroy", description = "Destroy server.")
public class ServerDestroy extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Override
	protected ExitCode exec() throws Exception {
		String name = config.get("runtime.name");
		File runtimeDir = new File(config.get("runtime.home", File.class), name);
		if (!runtimeDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Runtime %s does not exist.", runtimeDir));
		}

		console.print("You are about to destroy runtime '%s'.", name);
		console.print("Runtime location: %s", runtimeDir);
		console.crlf();
		if (!console.confirm("Please confirm: yes | [no]", "yes")) {
			console.print("User abort.");
			return ExitCode.ABORT;
		}

		console.print("Destroying runtime %s...", name);
		Files.removeFilesHierarchy(runtimeDir);
		if (!runtimeDir.delete()) {
			throw new IOException(format("Cannot remove runtime directory %s.", runtimeDir));
		}

		config.remove("runtime.name");
		config.remove("runtime.port");
		return ExitCode.SUCCESS;
	}
}
