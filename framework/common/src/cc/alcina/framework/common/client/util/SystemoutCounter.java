package cc.alcina.framework.common.client.util;

public class SystemoutCounter {
	private int dotsPerLine;

	private int ticks;

	private int tickCtr;

	private int dotCtr;

	private int allTicks;

	private boolean showPercentAtEndOfLine;

	private int size;

	int lines = 0;

	private String name;

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

	public SystemoutCounter name(String name) {
		this.name = name;
		return this;
	}

	public void tick() {
		tick("");
	}

	public void tick(String message) {
		++allTicks;
		if (++tickCtr == ticks) {
			tickCtr = 0;
			System.out.print(".");
			if (++dotCtr == dotsPerLine) {
				dotCtr = 0;
				if (showPercentAtEndOfLine || name != null) {
					message += CommonUtils.formatJ(" - %s%",
							(dotsPerLine * (lines + 1) * ticks * 100 / size));
				}
				if (name != null) {
					message += " - " + name;
				}
				System.out.println("  " + message);
				lines++;
			}
		}
	}
}
