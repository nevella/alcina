package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SEUtilities;

public class AnalyseThreadDump {
	public static boolean ignoreableStackTrace(String joined) {
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
						+ "|jdk.internal.misc.Unsafe.park"
						+ "|java.lang.ProcessHandleImpl.waitForProcessExit0"
						+ "|java.util.concurrent.locks.LockSupport.parkNanos"
						+ "|org.hornetq.core.).*");
		if (matches) {
			boolean notMatches = SEUtilities.normalizeWhitespace(joined)
					.matches("(?is).*(cc.alcina).*");
			if (joined.contains("AlcinaParallel")) {
				// really want this one
				matches = false;
			}
			// if (notMatches) {
			//// return false;
			// }
			// matches &= !notMatches;
			if (matches) {
				return true;
			}
		}
		List<String> lineList = Arrays.asList(joined.split("\n")).stream()
				.filter(line -> !line.matches(".*waiting on.*"))
				.collect(Collectors.toList());
		String[] lines = (String[]) lineList
				.toArray(new String[lineList.size()]);
		if (lines.length < 2) {
			return false;
		} else {
			if (lines[0].matches(".*java.lang.Object.wait.*")) {
				if (lines[1].matches(".*java.util.TimerThread.mainLoop.*")) {
					return true;
				}
				if (joined.contains(
						"org.apache.curator.framework.recipes.leader.LeaderSelector")) {
					return true;
				}
				if (lines[1].matches(
						".*com.arjuna.ats.internal.arjuna.coordinator.ReaperThread.run.*")) {
					return true;
				}
				if (lines.length > 2) {
					if (lines[1].matches(".*java.lang.Object.wait.*")) {
						if (lines[2]
								.matches(".*ava.util.TimerThread.mainLoop.*")) {
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
			if (lines[0].matches(".*sun.nio.fs.LinuxWatchService.poll.*")) {
				return true;
			}
			if (lines[0].matches(".*java.lang.Thread.sleep.*")) {
				if (joined.contains(
						"cc.alcina.framework.entity.persistence.metric.InternalMetrics.profile")) {
					return true;
				}
			}
			if (lines[0].matches(".*.EPoll.wait.*")) {
				return true;
			}
			if (lines[0].matches(".*.SocketInputStream.socketRead0.*")) {
				if (joined.contains("com.sun.mail.iap.ResponseInputStream")) {
					return true;
				}
			}
			if (joined.contains(
					"javax.management.remote.JMXConnectorFactory.connect")) {
				return true;
			}
		}
		return matches;
	}

	public static class TdModel {
		public static TdModel parse(String dump) {
			return parse(dump, true);
		}

		public static TdModel parse(String dump, boolean jStack) {
			TdModel model = new TdModel();
			TdModelThread currentThread = null;
			State state = State.outside_thread;
			Pattern firstLinePattern = Pattern
					.compile("\"(.+?)\".+?os_prio=\\d+.+?(?: in (\\S+).+)?");
			if (!jStack) {
				firstLinePattern = Pattern
						.compile("^Thread\\[(.+),(\\d+),(.+)\\]");
			}
			Pattern synchronizersPattern = Pattern
					.compile("Locked ownable synchronizers:");
			Pattern statePattern = Pattern
					.compile("java.lang.Thread.State: .*");
			for (String line : dump.split("\n")) {
				line = Ax.ntrim(line);
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
					.filter(tmt -> !tmt.ignoreable()).collect(AlcinaCollectors
							.toKeyMultimap(tmt -> tmt.lines.toString()));
			String distinctWaits = byLines.entrySet().stream().sorted(
					(e1, e2) -> e1.getValue().size() - e2.getValue().size())
					.map(e -> e.getValue().get(0).toStringForDump())
					.collect(Collectors.joining("\n"));
			return distinctWaits;
		}

		public String dumpDistinctNames() {
			Multimap<String, List<TdModelThread>> byLines = threads.stream()
					.filter(tmt -> !tmt.ignoreable()).collect(AlcinaCollectors
							.toKeyMultimap(tmt -> tmt.lines.toString()));
			String distinctWaits = byLines.entrySet().stream().sorted(
					(e1, e2) -> e1.getValue().size() - e2.getValue().size())
					.map(e -> e.getValue().get(0).name)
					.collect(Collectors.joining("\n"));
			return distinctWaits;
		}

		public enum Flavour {
			JSTACK, PRINT_STACK_TRACE, MBEAN
		}

		enum State {
			outside_thread, first_thread_line, post_first_thread_line,
			synchronizers
		}

		static class TdModelThread {
			static final Pattern IGNOREABLE_PATTERN = Pattern.compile(Ax.format(
					"(%s|VM Periodic Task Thread|C2 CompilerThread\\d+"
							+ "|Reference Handler|C1 CompilerThread\\d+|pool-shell-io.*|Keep-Alive-Timer"
							+ "|cluster1-timeouter-0|threadDeathWatcher-.*"
							+ "|Signal Dispatcher|Service Thread|Monitor Deflation Thread|Sweeper thread"
							+ "|DeploymentScanner-threads.*"
							+ "|kafka-coordinator-heartbeat-thread|Keep-Alive-SocketCleaner)",
					Configuration.get(AnalyseThreadDump.class,
							"ignoreableThreadNamePattern")));

			public String stateLine;

			public List<String> lines = new ArrayList<>();

			public List<String> synchronizers = new ArrayList<>();

			public String in;

			public String name;

			boolean ignoreable() {
				String joined = lines.stream()
						.collect(Collectors.joining("\n"));
				return ignoreableStackTrace(joined)
						|| IGNOREABLE_PATTERN.matcher(name).matches();
			}

			String toStringForDump() {
				return lines
						.isEmpty()
								? ""
								: Ax.format("Thread: %s\n%s", name,
										CommonUtils.tabify(
												lines.stream()
														.collect(Collectors
																.joining("\n")),
												200, 1));
			}
		}
	}
}
