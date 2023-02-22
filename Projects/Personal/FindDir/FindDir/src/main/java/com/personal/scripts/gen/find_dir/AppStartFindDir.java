package com.personal.scripts.gen.find_dir;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

final class AppStartFindDir {

	private AppStartFindDir() {
	}

	public static void main(
			final String[] args) {

		final Instant start = Instant.now();

		if (args.length >= 1 && "-help".equals(args[0])) {

			final String helpMessage = createHelpMessage();
			System.out.println(helpMessage);
			System.exit(0);
		}

		if (args.length < 2) {

			final String helpMessage = createHelpMessage();
			System.err.println("ERROR - insufficient arguments" +
					System.lineSeparator() + helpMessage);
			System.exit(-1);
		}

		final String rootPathString = args[0];
		final String folderPathPatternString = args[1];

		main(rootPathString, folderPathPatternString);

		final Duration executionTime = Duration.between(start, Instant.now());
		System.out.println("done; execution time: " + durationToString(executionTime));
	}

	private static String createHelpMessage() {

		return "usage: find_dir <folder_to_search_in> <folder_path_pattern>";
	}

	static void main(
			final String rootPathString,
			final String folderPathPatternString) {

		try {
			final Path rootPath = Paths.get(rootPathString).toAbsolutePath().normalize();
			System.out.println("path to search in:" + System.lineSeparator() + rootPath);

			final Pattern folderPathPattern = Pattern.compile(folderPathPatternString);
			System.out.println("file path pattern: " + folderPathPatternString);

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
				System.err.println("ERROR - failed to terminate all threads");
			}

			for (int i = 0; i < folderPathList.size(); i++) {

				final Path folderPath = folderPathList.get(i);
				System.out.println(i + ". " + folderPath.toUri());
			}

		} catch (final Throwable thr) {
			thr.printStackTrace();
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

	private static String durationToString(
			final Duration duration) {

		final StringBuilder stringBuilder = new StringBuilder();
		final long allSeconds = duration.get(ChronoUnit.SECONDS);
		final long hours = allSeconds / 3600;
		if (hours > 0) {
			stringBuilder.append(hours).append("h ");
		}

		final long minutes = (allSeconds - hours * 3600) / 60;
		if (minutes > 0) {
			stringBuilder.append(minutes).append("m ");
		}

		final long nanoseconds = duration.get(ChronoUnit.NANOS);
		final double seconds = allSeconds - hours * 3600 - minutes * 60 +
				nanoseconds / 1_000_000_000.0;
		stringBuilder.append(doubleToString(seconds)).append('s');

		return stringBuilder.toString();
	}

	private static String doubleToString(
			final double d) {

		final String str;
		if (Double.isNaN(d)) {
			str = "";

		} else {
			final String format;
			format = "0.000";
			final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.US);
			final DecimalFormat decimalFormat = new DecimalFormat(format, decimalFormatSymbols);
			str = decimalFormat.format(d);
		}
		return str;
	}
}
