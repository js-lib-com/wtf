package com.jslib.wtf.cli;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Home;
import com.jslib.commons.cli.Task;
import com.jslib.commons.cli.WebsUtil;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "update", description = "Update WTF install.")
public class Update extends Task {

	private static final URI DISTRIBUTION_URI = URI.create("http://maven.js-lib.com/com/js-lib/wtf-assembly/");
	private static final Pattern ARCHIVE_DIRECTORY_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d*(-[a-z0-9]+)?/$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ARCHIVE_FILE_PATTERN = Pattern.compile("^wtf-assembly.+\\.zip$");
	private static final Pattern UPDATER_FILE_PATTERN = Pattern.compile("^wtf-update.+\\.jar$");

	@Option(names = { "-f", "--force" }, description = "Force update regardless release date.")
	private boolean force;
	@Option(names = { "-y", "--yes" }, description = "Auto-confirm update.")
	private boolean yes;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about processed files.")
	private boolean verbose;

	@Override
	protected ExitCode exec() throws Exception {
		if (verbose) {
			console.print("Checking WTF assemblies repository...");
		}

		WebsUtil.File assemblyDir = webs.latestVersion(DISTRIBUTION_URI, ARCHIVE_DIRECTORY_PATTERN, verbose);
		if (assemblyDir == null) {
			console.print("Empty WTF assemblies repository %s.", DISTRIBUTION_URI);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		WebsUtil.File assemblyFile = webs.latestVersion(assemblyDir.getURI(), ARCHIVE_FILE_PATTERN, verbose);
		if (assemblyFile == null) {
			console.print("Invalid WTF assembly version %s. No assembly found.", assemblyDir.getURI());
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		Path homeDir = files.getPath(Home.getPath());
		Path binariesDir = homeDir.resolve("bin");
		Path updaterJar = files.getFileByNamePattern(binariesDir, UPDATER_FILE_PATTERN);
		if (updaterJar == null) {
			console.print("Corrupt WTF install. Missing updater.");
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		// uses wtf.properties file to detect last update time
		Path propertiesFile = homeDir.resolve("bin/wtf.properties");
		if (files.exists(propertiesFile)) {
			if (!force && !assemblyFile.getModificationTime().isAfter(files.getModificationTime(propertiesFile))) {
				console.print("Current WTF install is updated.");
				console.print("Command abort.");
				return ExitCode.ABORT;
			}
		}

		console.print("Updating WTF install from %s...", assemblyFile.getName());
		if (!yes && !console.confirm("Please confirm: yes | [no]", "yes")) {
			console.print("User cancel.");
			return ExitCode.CANCEL;
		}

		console.print("Downloading WTF assembly %s...", assemblyFile.getName());
		Path downloadFile = homeDir.resolve(assemblyFile.getName());
		webs.download(assemblyFile, downloadFile, verbose);

		console.print("Download complete. Start WTF install update.");
		List<String> command = new ArrayList<>();
		command.add("java");
		command.add("-cp");
		command.add(updaterJar.toAbsolutePath().toString());
		command.add("com.jslib.wtf.update.Main");
		if (verbose) {
			command.add("--verbose");
		}

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(files.getWorkingDir().toFile()).inheritIO().start();

		return ExitCode.SUCCESS;
	}
}
