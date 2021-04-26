package com.jslib.wtf.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jslib.commons.cli.Config;
import com.jslib.commons.cli.Console;
import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.FilesUtil;

import js.lang.BugError;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public abstract class Task implements Runnable {
	@Option(names = "--time", description = "Measure execution time. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean time;
	@Option(names = { "-x", "--exception" }, description = "Print stack trace on exception.")
	private boolean stacktrace;

	protected Console console;
	protected Config config;
	protected FilesUtil files;

	protected Task() {
		this.console = new Console();
		this.files = new FilesUtil(FileSystems.getDefault(), this.console);
	}

	public void setConsole(Console console) {
		this.console = console;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public void setFiles(FilesUtil files) {
		this.files = files;
	}

	@Override
	public void run() {
		long start = System.nanoTime();
		ExitCode exitCode = ExitCode.SUCCESS;
		try {
			exitCode = exec();
		} catch (IOException e) {
			handleException(e);
			exitCode = ExitCode.SYSTEM_FAIL;
		} catch (Throwable t) {
			handleException(t);
			exitCode = ExitCode.APPLICATION_FAIL;
		}
		if (time) {
			console.print("Processing time: %.04f msec.", (System.nanoTime() - start) / 1000000.0);
		}
		System.exit(exitCode.ordinal());
	}

	protected abstract ExitCode exec() throws Exception;

	private void handleException(Throwable t) {
		if (stacktrace) {
			StringWriter buffer = new StringWriter();
			t.printStackTrace(new PrintWriter(buffer));
			console.error(buffer);
		} else {
			String message = t.getMessage();
			console.error(message != null ? message : t.getClass());
		}
	}

	private static final Pattern JAR_PATH_PATTERN = Pattern.compile("^(.+)[\\\\/]bin[\\\\/].+\\.jar$");

	public static String getHome() {
		File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		Matcher matcher = JAR_PATH_PATTERN.matcher(jarFile.getAbsolutePath());
		if (!matcher.find()) {
			String home = System.getProperty("WTF_HOME");
			if (home != null) {
				return home;
			}
			throw new BugError("Invalid jar file pattern.");
		}
		return matcher.group(1);
	}
}
