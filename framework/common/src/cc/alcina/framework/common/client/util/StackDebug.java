package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class StackDebug {
	private CachingMap<Long, Stack<StackTraceElement[]>> perThreadTraces = new CachingMap<Long, Stack<StackTraceElement[]>>(
			tid -> new Stack<>());

	private String stackFilter;

	public int debugLines = 0;

	public StackDebug(String stackFilter) {
		this.stackFilter = stackFilter;
	}

	public void clearCurrentThread() {
		long tId = Thread.currentThread().getId();
		perThreadTraces.remove(tId);
	}

	public synchronized void debugCurrentThread() {
		long tId = Thread.currentThread().getId();
		perThreadTraces.get(tId).clear();
		debugLines = 25;
	}

	public void maybeDebugStack(Stack stack, boolean push) {
		if (debugLines > 0) {
			synchronized (this) {
				Thread thread = Thread.currentThread();
				long tId = thread.getId();
				if (!perThreadTraces.getMap().containsKey(tId)) {
					return;
				}
				List<String> lines = new ArrayList<String>();
				StackTraceElement[] traces = thread.getStackTrace();
				traces = filterTraces(traces);
				for (int i = 0; i < debugLines && i < traces.length; i++) {
					lines.add(traces[i].toString());
				}
				if (push) {
					perThreadTraces.get(tId).push(traces);
				}
				if (!push) {
					boolean debug = false;
					StackTraceElement[] lastTraces = new StackTraceElement[0];
					if (perThreadTraces.get(tId).isEmpty()) {
						debug = true;
					} else {
						lastTraces = perThreadTraces.get(tId).pop();
						if (lastTraces.length != traces.length) {
							debug = true;
						}
					}
					if (debug == true) {
						System.err.println(CommonUtils.formatJ(
								"***unbalanced stack***"
										+ "\nThread - %s\npush:\n%s\n\n\npop:\n%s\n\n",
								thread, CommonUtils.join(lastTraces, "\n"),
								CommonUtils.join(traces, "\n")));
					}
				}
				String template = "**stack-debug: %s-%s-%s-%s - %s -: \n\t%s\n\n***end-stack-debug\n\n";
				System.err.println(CommonUtils.formatJ(template, tId,
						hashCode(), (push ? "PUSH" : "POP"),
						stack == null ? 0 : stack.size(), traces.length,
						CommonUtils.join(lines, "\n\t")));
				Stack<StackTraceElement[]> traceStack = perThreadTraces
						.get(tId);
				List<StackTraceElement[]> traceList = new ArrayList<>(
						traceStack);
				for (int idx = 0; idx < traceList.size(); idx++) {
					System.out.println(
							CommonUtils.formatJ("\tDebug stack - #%s\n", idx));
					System.out.println(CommonUtils.formatJ("\t\t%s\n\t\t%s\n\n",
							traceList.get(idx)[0], traceList.get(idx)[1]));
				}
			}
		}
	}

	private StackTraceElement[] filterTraces(StackTraceElement[] traces) {
		boolean seenFilter = false;
		int idx = 0;
		for (; idx < traces.length; idx++) {
			String s = traces[idx].toString();
			boolean filter = s.contains(stackFilter);
			if (filter) {
				seenFilter = true;
			} else {
				if (seenFilter) {
					StackTraceElement[] dest = new StackTraceElement[traces.length
							- idx];
					System.arraycopy(traces, idx, dest, 0, traces.length - idx);
					return dest;
				}
			}
		}
		return traces;
	}
}
