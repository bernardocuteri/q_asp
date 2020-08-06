package it.unical.qasp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.junit.Test;

import it.unical.acr.qasp.QAsp;
import it.unical.acr.qasp.QAsp.ExceptionHandlingStrategy;
import it.unical.acr.qasp.QAspResult;

public class Benchmarks {

	private class TestResults {
		int tests;
		int satisfiables;
		int unsatisfiables;
		int failed;
		long aspQElapsed;

		@Override
		public String toString() {
			return "TestResults [tests=" + tests + ", satisfiables=" + satisfiables + ", unsatisfiables="
					+ unsatisfiables + ", failed=" + failed + ", aspQElapsed="
					+ TimeUnit.NANOSECONDS.toMillis(aspQElapsed) + " ms]";
		}
	}
	
	private void executeBenchmark(String baseDir, String encoding, String startFile, String instancesDirName) {
		QAsp.DEBUG_LEVEL = Level.FINE;
		File start = new File(baseDir + startFile);
		File instancesDir = new File(baseDir + instancesDirName);
		File encodingFile = new File(baseDir + encoding);
		List<File> filesList = new ArrayList<>(Arrays.asList(instancesDir.listFiles()));
		Collections.shuffle(filesList);

		TestResults res = new TestResults();
		for (File instance : filesList) {
			if (instance.getPath().endsWith(".apx")) {
				try {
					new TimeLimitedExecution(20, TimeUnit.SECONDS, () -> {
						QAsp solver = new QAsp();
						res.tests++;
						solver.setExceptionHandlingStrategy(ExceptionHandlingStrategy.THROW_RUNTIME);
						solver.setInputFiles(new File[] { start, instance, encodingFile });
						QAspResult result = solver.solve();
						boolean sat = result.isSat();
						res.satisfiables += sat ? 1 : 0;
						res.unsatisfiables += sat ? 0 : 1;
						System.out.println(result.toString() + " " + instance.getName());
						
						System.out.println(res);

					}).run();
				} catch (Exception e) {
					System.out.println("failed");
					res.failed++;
				}
			}
		}
	}

	@Test
	public void executeArgumentCoherenceBenchmark() {
		//executeBenchmark("./benchmarks/ArgumentCoherence/", "encoding.aspq", "start.aspq", "instances");
		executeBenchmark("./benchmarks/ArgumentCoherence/", "encoding_comp.aspq", "start_comp.aspq", "instances");
	}

	public interface CodeBlock {
		void execute();
	}

	private class TimeLimitedExecution {
		int timeout;
		TimeUnit timeUnit;
		CodeBlock codeblock;

		public TimeLimitedExecution(int timeout, TimeUnit timeUnit, CodeBlock codeblock) {
			super();
			this.timeout = timeout;
			this.timeUnit = timeUnit;
			this.codeblock = codeblock;
		}

		public void run() throws InterruptedException, ExecutionException, TimeoutException {

			final ExecutorService executor = Executors.newSingleThreadExecutor();

			final Future<?> future = executor.submit(() -> codeblock.execute());
			executor.shutdown(); // This does not cancel the already-scheduled task.
			try {
				System.out.println("future in");
				future.get(timeout, timeUnit);
			} finally {
				System.out.println("future out");
				if (!executor.isTerminated())
					executor.shutdownNow(); // If you want to stop the code that hasn't finished.
			}
		};
	}
}
