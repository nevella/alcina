package cc.alcina.framework.common.client.util;

public class SystemoutCounter {
	private int dotsPerLine;

	private int ticks;

	private int tickCtr;

	private int dotCtr;

	public SystemoutCounter(int ticks, int dotsPerLine) {
		this.ticks = ticks;
		this.dotsPerLine = dotsPerLine;
	}

	public void tick() {
		if (++tickCtr == ticks) {
			tickCtr = 0;
			System.out.print(".");
			if (++dotCtr == dotsPerLine) {
				dotCtr = 0;
				System.out.println();
			}
		}
	}

	public void end() {
		System.out.println();
	}
}
