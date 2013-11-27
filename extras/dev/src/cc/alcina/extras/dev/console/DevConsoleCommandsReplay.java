package cc.alcina.extras.dev.console;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ReplayInstruction;
import cc.alcina.framework.common.client.entity.ReplayInstruction.ReplayInstructionType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;

public class DevConsoleCommandsReplay {
	public static class CmdConvertCommandLogToReplays extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "rpi" };
		}

		@Override
		public String getDescription() {
			return "convert a client log to a series of replay instructions";
		}

		@Override
		public String getUsage() {
			return "rpi (will prompt for text, or copy from clipboard)";
		}

		@Override
		public String run(String[] argv) throws Exception {
			String rpi = console
					.getMultilineInput("Enter the log text, or blank for clipboard: ");
			rpi = rpi.isEmpty() ? console.getClipboardContents() : rpi;
			String ser = extractReplayInstructions(rpi);
			System.out.println(ser);
			console.setClipboardContents(ser);
			System.out.println("\n");
			return "ok";
		}

		@Override
		public boolean clsBeforeRun() {
			return true;
		}
	}

	public static String extractReplayInstructions(String rpi) {
		Pattern p1 = Pattern.compile("(.+?)\t(.+)");
		Matcher m1 = p1.matcher(rpi);
		List<ReplayInstruction> instructions = new ArrayList<ReplayInstruction>();
		while (m1.find()) {
			ReplayInstructionType type = CommonUtils.getEnumValueOrNull(
					ReplayInstructionType.class, m1.group(1));
			if (m1.group().contains("DIV.test-overlay")) {
				continue;
			}
			if (type != null) {
				String g2 = m1.group(2);
				if (g2.contains("\\tvalue :: ")) {
					g2 = ReplayInstruction.unescape(g2);
				}
				StringPair locValuePair = ClientLogRecord
						.parseLocationValue(g2);
				instructions.add(new ReplayInstruction(type, locValuePair.s1,
						locValuePair.s2));
			}
		}
		return CommonUtils.join(instructions, "\n");
	}
}
