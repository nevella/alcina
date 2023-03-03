package cc.alcina.extras.dev.console;

import java.io.File;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.AnalyseThreadDump;

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
			rpi = Io.read().path(file.getPath()).asString();
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
		Ax.out(analyseStacktrace(
				Io.read().resource("res/sample-stack-trace.txt").asString(),
				""));
	}

	private String analyseStacktrace(String dump, String filter) {
		try {
			LooseContext.push();
			if (!Ax.isBlank(filter)) {
				LooseContext.set(CONTEXT_FILTER, filter);
			}
			AnalyseThreadDump.TdModel model = AnalyseThreadDump.TdModel
					.parse(dump);
			return model.dumpDistinct();
		} finally {
			LooseContext.pop();
		}
	}
}