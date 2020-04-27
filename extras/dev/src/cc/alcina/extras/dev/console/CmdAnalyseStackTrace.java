package cc.alcina.extras.dev.console;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.Sx;

public class CmdAnalyseStackTrace extends DevConsoleCommand {
	public static final String CONTEXT_FILTER = CmdAnalyseStackTrace.class
			.getName() + ".CONTEXT_FILTER";

	protected static boolean ignoreableStackTrace(String joined) {
		if (LooseContext.has(CONTEXT_FILTER)
				&& !joined.matches(LooseContext.get(CONTEXT_FILTER))) {
			return true;
		}
		boolean matches = SEUtilities.normalizeWhitespace(joined).matches(
				"(?is).*((parking to wait for .*a|Waiting for lock: className:) "
						+ "(java.util.concurrent.ForkJoinPool|"
						+ "java.lang.ref.Reference.Lock|"
						+ "java.lang.ref.ReferenceQueue.Lock|"
						+ "com.arjuna.ats.arjuna.coordinator.TransactionReaper|"
						+ "com.arjuna.ats.internal.arjuna.recovery.ExpiredEntryMonitor|"
						+ "java.util.TaskQueue|"
						+ "org.apache.curator.framework.recipes.queue.ChildrenCache|"
						+ "org.apache.kafka.clients.consumer.internals.ConsumerCoordinator|"
						+ "java.util.concurrent.SynchronousQueue.TransferStack|"
						+ "java.util.concurrent.locks.AbstractQueuedSynchronizer.?ConditionObject)"
						+ "|EPollArrayWrapper.epollWait|ChildrenCache.blockingNextGetData|ReferenceQueue.remove"
						+ "|RequestController.waitForQueue"
						+ "|org.hornetq.core.).*");
		if (matches) {
			return true;
		}
		List<String> lineList = Arrays.asList(joined.split("\n")).stream()
				.filter(line -> !line.matches(".*waiting on.*"))
				.collect(Collectors.toList());
		String[] lines = (String[]) lineList
				.toArray(new String[lineList.size()]);
		if (lines.length < 2) {
			return false;
		} else {
			if (lines[0].matches(".*at java.lang.Object.wait.*")) {
				if (lines[1].matches(".*at java.util.TimerThread.mainLoop.*")) {
					return true;
				}
				if (lines[1].matches(
						".*at com.arjuna.ats.internal.arjuna.coordinator.ReaperThread.run.*")) {
					return true;
				}
				if (lines.length > 2) {
					if (lines[1].matches(".*at java.lang.Object.wait.*")) {
						if (lines[2].matches(
								".*at java.util.TimerThread.mainLoop.*")) {
							return true;
						}
					}
				}
				if (joined.contains("com.arjuna.ats.arjuna")) {
					return true;
				}
				if (joined.contains("com.arjuna.ats.internal.arjuna")) {
					return true;
				}
			}
			if (lines[0].matches(".*at sun.nio.fs.LinuxWatchService.poll.*")) {
				return true;
			}
		}
		return matches;
	}

	@Override
	public boolean clsBeforeRun() {
		return true;
	}

	@Override
	public String[] getCommandIds() {
		return new String[] { "stt" };
	}

	@Override
	public String getDescription() {
		return "analyse stack trace";
	}

	@Override
	public String getUsage() {
		return "stt (will prompt for text, or copy from clipboard)";
	}

	@Override
	public String run(String[] argv) throws Exception {
		String rpi = null;
		File file = new File("/tmp/stacktrace.txt");
		if (file.exists()) {
			rpi = ResourceUtilities.read(file.getPath());
		} else {
			if (!console.isHeadless()) {
				rpi = console.getMultilineInput(
						"Enter the stacktrace, or blank for clipboard: ");
			}
			rpi = Ax.isBlank(rpi) ? console.getClipboardContents() : rpi;
		}
		String filter = argv.length == 0 ? "" : argv[0];
		String ser = analyseStacktrace(rpi, filter);
		console.clear();
		System.out.println(ser);
		System.out.println("\n");
		return "ok";
	}

	public void testAnalysis() {
		Ax.out(analyseStacktrace(ResourceUtilities.read(
				CmdAnalyseStackTrace.class, "res/sample-stack-trace.txt"), ""));
	}

	private String analyseStacktrace(String dump, String filter) {
		try {
			LooseContext.push();
			if (!Ax.isBlank(filter)) {
				LooseContext.set(CONTEXT_FILTER, filter);
			}
			TdModel model = TdModel.parse(dump);
			return model.dumpDistinct();
		} finally {
			LooseContext.pop();
		}
	}

	static class TdModel {
		static TdModel parse(String dump) {
			TdModel model = new TdModel();
			TdModelThread currentThread = null;
			State state = State.outside_thread;
			Pattern firstLinePattern = Pattern
					.compile("\"(.+?)\".+?os_prio=\\d+.+?(?: in (\\S+).+)?");
			Pattern synchronizersPattern = Pattern
					.compile("Locked ownable synchronizers:");
			Pattern statePattern = Pattern
					.compile("java.lang.Thread.State: .*");
			for (String line : dump.split("\n")) {
				line = Sx.ntrim(line);
				if (line.isEmpty()) {
					continue;
				}
				Matcher firstLineMatcher = firstLinePattern.matcher(line);
				Matcher synchronizersMatcher = synchronizersPattern
						.matcher(line);
				Matcher stateMatcher = statePattern.matcher(line);
				if (firstLineMatcher.matches()) {
					state = State.first_thread_line;
					currentThread = new TdModelThread();
					model.threads.add(currentThread);
					currentThread.name = firstLineMatcher.group(1);
					currentThread.in = firstLineMatcher.group(2);
				} else if (synchronizersMatcher.matches()) {
					state = State.synchronizers;
				} else if (stateMatcher.matches()) {
					currentThread.stateLine = line;
				} else {
					switch (state) {
					case outside_thread:
						break;
					case first_thread_line:
					case post_first_thread_line:
						currentThread.lines.add(line);
						state = State.post_first_thread_line;
						break;
					case synchronizers:
						currentThread.synchronizers.add(line);
						break;
					default:
						throw new UnsupportedOperationException();
					}
				}
			}
			return model;
		}

		List<TdModelThread> threads = new ArrayList<>();

		public String dumpDistinct() {
			Multimap<String, List<TdModelThread>> byLines = threads.stream()
					.filter(tmt -> !tmt.ignoreable()).collect(
							AlcinaCollectors.toKeyMultimap(tmt -> tmt.lines.toString()));
			String distinctWaits = byLines.entrySet().stream().sorted(
					(e1, e2) -> e1.getValue().size() - e2.getValue().size())
					.map(e -> String.format("%5s %-40s", e.getValue().size(),
							e.getValue().get(0).toStringForDump()))
					.collect(Collectors.joining("\n"));
			return distinctWaits;
		}

		enum State {
			outside_thread, first_thread_line, post_first_thread_line,
			synchronizers
		}
	}

	static class TdModelThread {
		public String stateLine;

		public List<String> lines = new ArrayList<>();

		public List<String> synchronizers = new ArrayList<>();

		public String in;

		public String name;

		boolean ignoreable() {
			String joined = lines.stream().collect(Collectors.joining("\n"));
			return ignoreableStackTrace(joined);
		}

		String toStringForDump() {
			return lines.isEmpty() ? ""
					: CommonUtils.tabify(
							lines.stream().collect(Collectors.joining("\n")),
							200, 1);
		}
	}
}