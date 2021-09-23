package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.List;

public class RangeParser {
	private String text;

	private ArrayList<Integer> ints;

	private RangeParserCallback callback;

	int startOfName = -1;

	public String parse(RangeParserCallback callback, String text) {
		this.callback = callback;
		this.text = text;
		if (CommonUtils.isNullOrEmpty(text)) {
			return null;
		}
		int idx = 0;
		ints = new ArrayList<Integer>();
		int startOfInt = -1;
		boolean lastDelimWasDash = false;
		for (; idx < text.length(); idx++) {
			char c = text.charAt(idx);
			switch (c) {
			case '-':
				if (startOfName == -1 && startOfInt != -1
						&& !lastDelimWasDash) {
					int i = Integer.parseInt(text.substring(startOfInt, idx));
					ints.add(i);
					startOfInt = -1;
					lastDelimWasDash = true;
					continue;
				}
				break;
			case ' ':
			case ',':
			case ';':
				if (startOfInt != -1) {
					int i = Integer.parseInt(text.substring(startOfInt, idx));
					if (lastDelimWasDash) {
						int rs = ints.get(ints.size() - 1);
						for (int j = rs + 1; j <= i && j - rs < 100; j++) {
							ints.add(j);
						}
						lastDelimWasDash = false;
					} else {
						ints.add(i);
					}
					startOfInt = -1;
				} else {
					if (idx > 0 && text.substring(idx - 1)
							.matches("[a-zA-Z] [a-zA-Z].*")) {
						// Qd R or similar
					} else {
						String err = maybeHandleDelim(idx);
						if (err != null) {
							return err;
						}
					}
				}
				continue;
			}
			if (startOfName == -1 && (c >= '0' && c <= '9')) {
				startOfInt = startOfInt == -1 ? idx : startOfInt;
			} else {
				startOfName = startOfName == -1
						? startOfInt == -1 ? idx : startOfInt
						: startOfName;
			}
		}
		String err = maybeHandleDelim(idx);
		if (err != null) {
			return err;
		}
		return null;
	}

	private String err(int errStart, int errEnd, String message) {
		return Ax.format("%s >>> %s <<< >>> (%s) <<< %s",
				text.substring(0, errStart), text.substring(errStart, errEnd),
				message, text.substring(errEnd));
	}

	private String maybeHandleDelim(int idx) {
		if (startOfName == -1) {
			return null;
		}
		String msg = null;
		if (ints.isEmpty()) {
			msg = err(startOfName, idx,
					callback.rangeWithoutNumbersErrMessage());
		} else {
			String objectName = text.substring(startOfName, idx);
			String maybeErr = callback.processRangelet(ints, objectName);
			if (maybeErr != null) {
				msg = err(startOfName, idx, maybeErr);
			}
		}
		ints.clear();
		startOfName = -1;
		return msg;
	}

	public interface RangeParserCallback {
		public String processRangelet(List<Integer> points, String objectName);

		public String rangeWithoutNumbersErrMessage();
	}
}
