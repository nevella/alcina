package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class StackDebug {
	private Stack<StackTraceElement[]> pushTraces = new Stack<StackTraceElement[]>();

	private String stackFilter;

	public StackDebug(String stackFilter) {
		this.stackFilter = stackFilter;
	}

	public void debugCurrentThread() {
		debugThreadId = Thread.currentThread().getId();
		debugLines = 5;
	}

	public void maybeDebugStack(Stack stack, boolean push) {
		if (debugLines > 0) {
			Thread t = Thread.currentThread();
			if (debugThreadId != -1 && debugThreadId != t.getId()) {
				return;
			}
			List<String> lines = new ArrayList<String>();
			StackTraceElement[] traces = t.getStackTrace();
			traces = filterTraces(traces);
			for (int i = 3; i < 3 + debugLines && i < traces.length; i++) {
				lines.add(traces[i].toString());
			}
			if (push) {
				pushTraces.push(traces);
			}
			if (debugThreadId != -1 && !push) {
				boolean debug = false;
				StackTraceElement[] lastTraces = new StackTraceElement[0];
				if (pushTraces.isEmpty()) {
					debug = true;
				} else {
					lastTraces = pushTraces.pop();
					if (lastTraces.length != traces.length) {
						debug = true;
					}
				}
				if (debug == true) {
					System.err.println(CommonUtils.formatJ(
							"***unbalanced stack***"
									+ "\npush:\n%s\n\n\npop:\n%s\n\n",
							CommonUtils.join(lastTraces, "\n"),
							CommonUtils.join(traces, "\n")));
				}
			}
			System.err.println(CommonUtils.formatJ("%s-%s-%s-%s - %s -: %s\n",
					t.getId(), hashCode(), push, stack.size(), traces.length,
					CommonUtils.join(lines, "\n")));
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

	public long debugThreadId = -1;

	public int debugLines = 0;
}
