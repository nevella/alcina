package cc.alcina.framework.common.client.entity;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;

public class ReplayInstruction {
	public enum ReplayInstructionType {
		CLICK, CHANGE, HISTORY
	}

	public ReplayInstructionType type;

	public String xpath;

	public String value;

	public ReplayInstruction() {
	}

	public ReplayInstruction(ReplayInstructionType type, String xpath,
			String value) {
		this.type = type;
		this.xpath = xpath;
		this.value = value;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("%s\t%s\t%s", type, xpath, value==null?"":value);
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
			StringPair pair = ClientLogRecord.parseXpathValue(record
					.getMessage());
			return new ReplayInstruction(type, pair.s1, pair.s2);
		} else {
			return null;
		}
	}
}