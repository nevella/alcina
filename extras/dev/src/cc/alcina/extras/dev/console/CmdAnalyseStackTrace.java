package cc.alcina.extras.dev.console;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.J8Utils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.Sx;

public class CmdAnalyseStackTrace extends DevConsoleCommand {
	public static final String CONTEXT_FILTER = CmdAnalyseStackTrace.class
			.getName() + ".CONTEXT_FILTER";

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
		return "convert a client log to a series of replay instructions";
	}

	@Override
	public String getUsage() {
		return "rpi (will prompt for text, or copy from clipboard)";
	}

	@Override
	public String run(String[] argv) throws Exception {
		String rpi = null;
		File file = new File("/tmp/stacktrace.txt");
		if (file.exists()) {
			rpi = ResourceUtilities.read(file.getPath());
		} else {
			rpi = console.getMultilineInput(
					"Enter the stacktrace, or blank for clipboard: ");
			rpi = rpi.isEmpty() ? console.getClipboardContents() : rpi;
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
			for (String line : dump.split("\n")) {
				line = Sx.ntrim(line);
				if (line.isEmpty()) {
					continue;
				}
				Matcher firstLineMatcher = firstLinePattern.matcher(line);
				Matcher synchronizersMatcher = synchronizersPattern
						.matcher(line);
				if (firstLineMatcher.matches()) {
					state = State.first_thread_line;
					currentThread = new TdModelThread();
					model.threads.add(currentThread);
					currentThread.name = firstLineMatcher.group(1);
					currentThread.in = firstLineMatcher.group(2);
				} else if (synchronizersMatcher.matches()) {
					state = State.synchronizers;
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
							J8Utils.toKeyMultimap(tmt -> tmt.lines.toString()));
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
		public List<String> lines = new ArrayList<>();

		public List<String> synchronizers = new ArrayList<>();

		public String in;

		public String name;

		boolean ignoreable() {
			String joined = lines.stream().collect(Collectors.joining("\n"));
			if (LooseContext.has(CONTEXT_FILTER)
					&& !joined.matches(LooseContext.get(CONTEXT_FILTER))) {
				return true;
			}
			return joined.matches("(?s).*(parking to wait for .*a "
					+ "(java.util.concurrent.ForkJoinPool|"
					+ "java.util.concurrent.SynchronousQueueTransferStack|"
					+ "java.util.concurrent.locks.AbstractQueuedSynchronizer.?ConditionObject)"
					+ "|EPollArrayWrapper.epollWait|ChildrenCache.blockingNextGetData|ReferenceQueue.remove"
					+ "|RequestController.waitForQueue"
					+ "|org.hornetq.core.).*");
		}

		String toStringForDump() {
			return lines.isEmpty() ? ""
					: CommonUtils.tabify(
							lines.stream().collect(Collectors.joining("\n")),
							200, 1);
		}
	}
}