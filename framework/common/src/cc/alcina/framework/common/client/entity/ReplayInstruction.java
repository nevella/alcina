package cc.alcina.framework.common.client.entity;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;

public class ReplayInstruction {
	public enum ReplayInstructionType {
		CLICK, CHANGE, HISTORY, COMMENT, CONTAINER
	}

	public static final String ALLOW_MULTIPLE_TARGETS = "allow-multiple-targets";

	public static final String LONG_TIMEOUT = "long-timeout";

	public ReplayInstructionType type;

	public String param1;

	public String param2;

	public static final String SEP = " :: [";

	public static final String REPLAY_TEXT_WILDCARD = "::replay-wildcard";

	public boolean isAllowMultipleTargets() {
		return type == ReplayInstructionType.CLICK && param2 != null
				&& param2.contains(ALLOW_MULTIPLE_TARGETS);
	}

	public boolean isLongTimeout() {
		return type == ReplayInstructionType.CLICK && param2 != null
				&& param2.contains(LONG_TIMEOUT);
	}

	public ReplayInstruction() {
	}

	public ReplayInstruction(ReplayInstructionType type, String param1,
			String param2) {
		this.type = type;
		this.param1 = param1;
		this.param2 = param2;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("%s\t%s\t%s", type, param1,
				param2 == null ? "" : param2);
	}

	public static ReplayInstruction fromString(String s) {
		ReplayInstruction replayInstruction = new ReplayInstruction();
		int idx = 0, idx1 = s.indexOf("\t");
		if (idx1 == -1) {
			return replayInstruction;
		}
		replayInstruction.type = ReplayInstructionType.valueOf(s.substring(idx,
				idx1));
		idx = idx1;
		idx1 = s.indexOf("\t", idx1 + 1);
		if (idx1 == -1) {
			replayInstruction.param1 = s.substring(idx + 1);
		} else {
			replayInstruction.param1 = s.substring(idx + 1, idx1);
			replayInstruction.param2 = s.substring(idx1 + 1);
		}
		return replayInstruction;
	}

	public static String escape(String str) {
		return str == null
				|| (str.indexOf("\n") == -1 && str.indexOf("\t") == -1 && str
						.indexOf("\\") == -1) ? str : str.replace("\\", "\\\\")
				.replace("\n", "\\n").replace("\t", "\\t");
	}

	public static String unescape(String str) {
		if (str == null) {
			return null;
		}
		int idx = 0, x = 0;
		StringBuilder sb = new StringBuilder(str.length());
		while ((idx = str.indexOf("\\", x)) != -1) {
			sb.append(str.substring(x, idx));
			char c = str.charAt(idx + 1);
			switch (c) {
			case '\\':
				sb.append("\\");
				break;
			case 'n':
				sb.append("\n");
				break;
			case 't':
				sb.append("\t");
				break;
			}
			x = idx + 2;
		}
		sb.append(str.substring(x));
		return sb.toString();
	}

	public static ReplayInstruction fromClientLogRecord(ClientLogRecord record) {
		ReplayInstructionType type = CommonUtils.getEnumValueOrNull(
				ReplayInstructionType.class, record.getTopic());
		if (type != null) {
			StringPair pair = ClientLogRecord.parseLocationValue(record
					.getMessage());
			return new ReplayInstruction(type, pair.s1, pair.s2);
		} else {
			return null;
		}
	}

	public boolean changeTextSelector(String newText) {
		ReplayLocator locator = parseReplayBody(param1);
		if (locator.indexStr == null && !newText.equals(locator.text)) {
			param1 = CommonUtils.formatJ("%s :: [%s]", locator.path,
					newText);
			return true;
		}
		return false;
	}

	public static String createReplayBody(String text, String path,
			String valueMessage) {
		return CommonUtils.formatJ("%s :: [%s]%s", path, text, valueMessage);
	}

	public static class ReplayLocator {
		public String path;

		public String indexStr;

		public String text;
	}

	public static ReplayLocator parseReplayBody(String param) {
		// pattern is always $XPATH :: [$TEXT]
		ReplayLocator result = new ReplayLocator();
		RegExp regex = RegExp.compile("(.+)(?:\\[idx:([0-9]+)\\])$");
		MatchResult matchResult = regex.exec(param);
		String locationish = param;
		if (matchResult != null) {
			locationish = matchResult.getGroup(1);
			result.indexStr = matchResult.getGroup(2);
		}
		int idx1 = locationish.indexOf(SEP);
		if (idx1 == -1) {
			return null;
		}
		result.path = locationish.substring(0, idx1);
		result.text = locationish.substring(idx1 + SEP.length(),
				locationish.length() - 1);
		return result;
	}
}