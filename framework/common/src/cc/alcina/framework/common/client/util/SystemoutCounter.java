package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.function.Supplier;

import org.slf4j.Logger;

public class SystemoutCounter {
	static Supplier<String> emptySupplier = () -> "";

	public static SystemoutCounter standardJobCounter(Collection collection,
			Object caller) {
		return standardJobCounter(collection.size(),
				caller instanceof String ? caller.toString()
						: caller.getClass().getSimpleName());
	}

	public static SystemoutCounter standardJobCounter(int size, String name) {
		SystemoutCounter counter = new SystemoutCounter(Math.max(1, size / 400),
				20, size, true);
		counter.withName(name + "::jobProgress");
		return counter;
	}

	private int dotsPerLine;

	private int ticks;

	private int tickCtr;

	private int dotCtr;

	private int allTicks;

	private boolean showPercentAtEndOfLine;

	private int size;

	int lines = 0;

	private String name;

	private Logger logger;

	String buffer = "";

	public SystemoutCounter(int ticks, int dotsPerLine) {
		this(ticks, dotsPerLine, 1, false);
	}

	public SystemoutCounter(int ticks, int dotsPerLine, int size,
			boolean showPercentAtEndOfLine) {
		this.ticks = ticks;
		this.dotsPerLine = dotsPerLine;
		this.size = size;
		this.showPercentAtEndOfLine = showPercentAtEndOfLine;
	}

	public void end() {
		System.out.println();
	}

	public int getAllTicks() {
		return this.allTicks;
	}

	public double getFractionComplete() {
		return (double) allTicks / (double) size;
	}

	public Logger getLogger() {
		return this.logger;
	}

	public SystemoutCounter withName(String name) {
		this.name = name;
		return this;
	}

	public void newLine() {
		outLine("");
		tickCtr = 0;
		dotCtr = 0;
	}

	private void outLine(String string) {
		string = buffer + string;
		if (logger == null) {
			System.out.println(string);
		} else {
			logger.debug(string);
		}
		buffer = "";
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public void tick() {
		tick(emptySupplier);
	}

	public void tick(String message) {
		tick(() -> message);
	}

	public synchronized void tick(Supplier<String> messageSupplier) {
		++allTicks;
		if (++tickCtr == ticks) {
			tickCtr = 0;
			if (logger == null) {
				System.out.print(".");
			} else {
				buffer += ".";
			}
			if (++dotCtr == dotsPerLine) {
				dotCtr = 0;
				String message = messageSupplier.get();
				if (showPercentAtEndOfLine || name != null) {
					message += Ax.format(" - %s%", (allTicks * 100) / size);
				}
				if (name != null) {
					message += " - " + name;
				}
				outLine("  " + message);
				lines++;
			}
		}
	}
}
