package cc.alcina.extras.dev.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ReplayInstruction;
import cc.alcina.framework.common.client.entity.ReplayInstruction.ReplayInstructionType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;

public class DevConsoleCommandsReplay {
	public static class CmdConvertCommandLogToReplays
			extends DevConsoleCommand {
		@Override
		public boolean clsBeforeRun() {
			return true;
		}

		public String extractReplayInstructions(String rpi) {
			List<ReplayInstruction> instructions = new ArrayList<ReplayInstruction>();
			if (rpi.contains("Deobfuscated stacktrace:")) {
				String enumStr = CommonUtils
						.join(ReplayInstructionType.values(), "|")
						.toLowerCase();
				Pattern p1 = Pattern.compile(
						String.format(".+?\\| (%s)\\s+\\| (.+)", enumStr));
				Pattern p2 = Pattern.compile(" {60,}(\\S.+)");
				String type = null;
				String txt = null;
				for (String l : rpi.split("\n")) {
					Matcher m1 = p1.matcher(l);
					Matcher m2 = p2.matcher(l);
					if (m1.matches()) {
						if (type != null) {
							addInstruction(instructions, type, txt, txt);
						}
						type = m1.group(1);
						txt = m1.group(2);
					} else if (m2.matches()) {
						txt += m2.group(1);
					}
				}
				if (type != null) {
					addInstruction(instructions, type, txt, txt);
				}
				Collections.reverse(instructions);
			} else {
				Pattern p1 = Pattern.compile("(.+?)\t(.+)");
				Matcher m1 = p1.matcher(rpi);
				while (m1.find()) {
					String group1 = m1.group(1);
					String group0 = m1.group();
					String g2 = m1.group(2);
					addInstruction(instructions, group1, group0, g2);
				}
			}
			return CommonUtils.join(instructions, "\n");
		}

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
			String rpi = console.getMultilineInput(
					"Enter the log text, or blank for clipboard: ");
			rpi = rpi.isEmpty() ? console.getClipboardContents() : rpi;
			String ser = extractReplayInstructions(rpi);
			System.out.println(ser);
			console.setClipboardContents(ser);
			System.out.println("\n");
			return "ok";
		}

		private void addInstruction(List<ReplayInstruction> instructions,
				String group1, String group0, String g2) {
			if (group0.contains("DIV.test-overlay")) {
				return;
			}
			ReplayInstructionType type = CommonUtils
					.getEnumValueOrNull(ReplayInstructionType.class, group1);
			if (type != null) {
				if (g2.contains("\\tvalue :: ")) {
					g2 = ReplayInstruction.unescape(g2);
				}
				StringPair locValuePair = ClientLogRecord
						.parseLocationValue(g2);
				instructions.add(new ReplayInstruction(type, locValuePair.s1,
						locValuePair.s2));
			}
		}
	}
}
