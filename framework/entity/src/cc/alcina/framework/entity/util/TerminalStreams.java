package cc.alcina.framework.entity.util;

import java.io.PrintStream;

import cc.alcina.framework.entity.util.BiPrintStream.StreamNumber;

/*
 * This class wraps the terminal (stdout, stderr) streams, providing the ability
 * to reroute both to a FileOutputStream post-init
 * 
 * This class must be called in a static constructor of the app - otherwise
 * stdout/err redirection doesn't work
 * 
 * This class allows swapping of existing output (once) to write *all* app
 * content
 */
public class TerminalStreams {
	static TerminalStreams instance;

	public static synchronized TerminalStreams get() {
		if (instance == null) {
			instance = new TerminalStreams();
		}
		return instance;
	}

	BiPrintStream out;

	BiPrintStream err;

	public enum TerminalStream {
		out, err
	}

	public void start() {
		err = new BiPrintStream();
		err.s1 = System.err;
		err.s2 = BiPrintStream.swappablePrintStreams();
		out = new BiPrintStream();
		out.s1 = System.out;
		out.s2 = BiPrintStream.swappablePrintStreams();
		System.setErr(err);
		System.setOut(out);
	}

	public void stdSysOut() {
		System.setErr(err.s1);
		System.setOut(out.s1);
	}

	@SuppressWarnings("resource")
	public void redirect(TerminalStream terminalStream,
			StreamNumber streamNumber, PrintStream printStream) {
		BiPrintStream bps = terminalStream == TerminalStream.out
				? (BiPrintStream) this.out.s2
				: (BiPrintStream) this.err.s2;
		bps.redirect(streamNumber, printStream);
	}
}
