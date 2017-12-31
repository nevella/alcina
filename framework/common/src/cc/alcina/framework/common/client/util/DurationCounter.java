package cc.alcina.framework.common.client.util;

public class DurationCounter {
	private long t1;

	public DurationCounter() {
		t1 = System.currentTimeMillis();
	}

	public void end(String message, Object... args) {
		Object[] iArgs = new Object[] { System.currentTimeMillis() - t1 };
		if (args.length == 0) {
		} else {
			Object[] t = new Object[args.length + 1];
			System.arraycopy(args, 0, t, 0, args.length);
			t[t.length - 1] = iArgs[0];
			iArgs = t;
		}
		System.out.println(CommonUtils.formatJ(message, iArgs));
	}
}