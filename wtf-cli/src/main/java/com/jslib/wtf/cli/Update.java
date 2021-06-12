package com.jslib.wtf.cli;

import java.net.URI;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Home;
import com.jslib.commons.cli.IFile;
import com.jslib.commons.cli.IProgress;
import com.jslib.commons.cli.Task;

import js.format.FileSize;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "update", description = "Update WTF install.")
public class Update extends Task {

	private static final URI DISTRIBUTION_URI = URI.create("http://maven.js-lib.com/com/js-lib/wtf-assembly/");
	private static final Pattern ARCHIVE_DIRECTORY_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d*(-[a-z0-9]+)?/$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ARCHIVE_FILE_PATTERN = Pattern.compile("^wtf-assembly.+\\.zip$");
	private static final Pattern UPDATER_FILE_PATTERN = Pattern.compile("^wtf-update.+\\.jar$");

	private static final DateTimeFormatter modificationTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final FileSize fileSizeFormatter = new FileSize();

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

		IProgress<IFile> fileProgress = file -> {
			if (verbose) {
				console.print("%s %s\t%s", modificationTimeFormatter.format(file.getModificationTime()), fileSizeFormatter.format(file.getSize()), file.getName());
			}
		};

		IFile assemblyDir = httpRequest.scanLatestFileVersion(DISTRIBUTION_URI, ARCHIVE_DIRECTORY_PATTERN, fileProgress);
		if (assemblyDir == null) {
			console.print("Empty WTF assemblies repository %s.", DISTRIBUTION_URI);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		IFile assemblyFile = httpRequest.scanLatestFileVersion(assemblyDir.getURI(), ARCHIVE_FILE_PATTERN, fileProgress);
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
		httpRequest.download(assemblyFile.getURI(), downloadFile, length -> {
			if (verbose) {
				console.print('.');
			}
		});

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
