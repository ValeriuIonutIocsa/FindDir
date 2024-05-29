package com.personal.scripts.gen.find_dir;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.utils.log.Logger;

final class AppStartFindDir {

	private AppStartFindDir() {
	}

	public static void main(
			final String[] args) {

		final Instant start = Instant.now();

		if (args.length >= 1 && "-help".equals(args[0])) {

			final String helpMessage = createHelpMessage();
			Logger.printLine(helpMessage);
			System.exit(0);
		}

		if (args.length < 2) {

			final String helpMessage = createHelpMessage();
			Logger.printError("insufficient arguments" +
					System.lineSeparator() + helpMessage);
			System.exit(-1);
		}

		final String rootPathString = args[0];
		final String folderPathPatternString = args[1];

		mainL2(rootPathString, folderPathPatternString);

		Logger.printFinishMessage(start);
	}

	private static String createHelpMessage() {

		return "usage: find_dir <folder_to_search_in> <folder_path_pattern>";
	}

	private static void mainL2(
			final String rootPathString,
			final String folderPathPatternString) {

		try {
			Logger.printProgress("starting \"find_dir\" script");

			final Path rootPath = Paths.get(rootPathString).toAbsolutePath().normalize();
			Logger.printLine("path to search in:" + System.lineSeparator() + rootPath);

			final Pattern folderPathPattern = Pattern.compile(folderPathPatternString);
			Logger.printLine("file path pattern: " + folderPathPatternString);

			final List<Path> folderPathList = Collections.synchronizedList(new ArrayList<>());

			final List<Runnable> runnableList = new ArrayList<>();
			Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {

				@Override
				public FileVisitResult preVisitDirectory(
						final Path folderPath,
						final BasicFileAttributes attrs) throws IOException {

					runnableList.add(() -> searchFolder(
							folderPathPattern, folderPath, folderPathList));
					return super.preVisitDirectory(folderPath, attrs);
				}
			});

			final ExecutorService executorService = Executors.newFixedThreadPool(12);
			for (final Runnable runnable : runnableList) {
				executorService.execute(runnable);
			}
			executorService.shutdown();
			final boolean success = executorService.awaitTermination(10, TimeUnit.SECONDS);
			if (!success) {
				Logger.printError("failed to terminate all threads");
			}

			for (int i = 0; i < folderPathList.size(); i++) {

				final Path folderPath = folderPathList.get(i);
				Logger.printLine(i + ". " + folderPath.toUri());
			}

		} catch (final Exception exc) {
			Logger.printException(exc);
		}
	}

	private static void searchFolder(
			final Pattern folderPathPattern,
			final Path folderPath,
			final List<Path> folderPathList) {

		final String folderPathString = folderPath.toString();
		if (folderPathPattern.matcher(folderPathString).matches()) {
			folderPathList.add(folderPath);
		}
	}
}
