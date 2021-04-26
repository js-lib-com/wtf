package com.jslib.wtf.cli;

import java.nio.file.Path;
import java.util.Properties;

import com.jslib.commons.cli.ExitCode;

import picocli.CommandLine.Command;

@Command(name = "setup", description = "Set up a new WTF install.")
public class Setup extends Task {
	@Override
	protected ExitCode exec() throws Exception {
		console.print("WTF setup.");

		Path home = files.getPath(getHome());
		Properties properties = config.getGlobalProperties();
		properties.put("wtf.home", home.toString());
		properties.put("repository.dir", home.resolve("repository").toString());

		properties.put("user.name", console.input("User name"));
		properties.put("user.email", console.input("User email"));
		properties.put("runtime.home", console.input("Runtime home"));

		Path propertiesFile = home.resolve("bin/wtf.properties");
		properties.store(files.getOutputStream(propertiesFile), null);

		return ExitCode.SUCCESS;
	}
}
