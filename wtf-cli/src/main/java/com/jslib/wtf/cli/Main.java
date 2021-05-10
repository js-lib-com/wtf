package com.jslib.wtf.cli;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Properties;

import com.jslib.commons.cli.Config;
import com.jslib.commons.cli.Console;
import com.jslib.commons.cli.Home;
import com.jslib.commons.cli.Task;
import com.jslib.wtf.cli.config.ConfigCommands;
import com.jslib.wtf.cli.config.ConfigList;
import com.jslib.wtf.cli.icons.CreateIcons;
import com.jslib.wtf.cli.project.ProjectCommands;
import com.jslib.wtf.cli.project.ProjectCreate;
import com.jslib.wtf.cli.project.ProjectDestroy;

import js.lang.BugError;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "wtf", description = "Command line tools for Web Tiny Framework projects.", mixinStandardHelpOptions = true, version = "WTF, version 0.0.1-SNAPSHOT")
public class Main {
	public static void main(String... args) throws IOException {
		Home.setMainClass(Main.class);
		Properties globalProperties = new Properties();
		Properties projectProperties = new Properties();
		Config config = new Config(globalProperties, projectProperties);

		Main main = new Main(config);
		// use wtf.home property to detect if WTF install is properly initialized;
		// force 'setup' if not
		if (!config.has("wtf.home")) {
			args = new String[] { "setup" };
		}
		main.run(args);
	}

	private final Config config;
	private final Console console;

	public Main(Config config) {
		this.config = config;
		this.console = new Console();
	}

	private void run(String... args) {
		CommandLine createCommands = new CommandLine(new CreateCommands());
		createCommands.addSubcommand(task(CreateIcons.class));

		CommandLine projectCommands = new CommandLine(ProjectCommands.class);
		projectCommands.addSubcommand(task(ProjectCreate.class));
		projectCommands.addSubcommand(task(ProjectDestroy.class));

		CommandLine configCommands = new CommandLine(new ConfigCommands());
		configCommands.addSubcommand(task(ConfigList.class));

		CommandLine commandLine = new CommandLine(this);
		commandLine.addSubcommand(createCommands);
		commandLine.addSubcommand(projectCommands);
		commandLine.addSubcommand(configCommands);
		commandLine.addSubcommand(task(Update.class));
		commandLine.addSubcommand(task(Setup.class));

		console.print("Web Tiny Framework");
		console.crlf();

		System.exit(commandLine.execute(args));
	}

	private Object task(Class<? extends Task> taskClass) {
		Annotation commandAnnotation = taskClass.getAnnotation(Command.class);
		if (commandAnnotation == null) {
			throw new BugError("Not annotated task class |%s|.", taskClass);
		}
		try {
			Task task = taskClass.newInstance();
			task.setConfig(config);
			task.setConsole(console);
			return task;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new BugError("Not instantiable task class |%s|.", taskClass);
		}
	}
}
