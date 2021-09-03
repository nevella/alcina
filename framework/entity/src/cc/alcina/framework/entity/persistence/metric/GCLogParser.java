package cc.alcina.framework.entity.persistence.metric;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.ResourceUtilities;

public class GCLogParser {
	public Events parse(String path, int from, int logPausesGtMillis) {
		String dateRegex = "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})((?:\\+|-)\\d{4})";
		String timeRegex = "(\\d+\\.\\d+)";
		String start = Ax.format("%s: %s: Application time: %s seconds.*",
				dateRegex, timeRegex, timeRegex);
		String end = Ax.format(
				"%s: %s: Total time for which application threads were stopped: %s seconds.*",
				dateRegex, timeRegex, timeRegex);
		String logContents = null;
		Events events = new Events();
		try {
			File file = new File(path);
			FileInputStream fis = new FileInputStream(file);
			if (from > file.length()) {
				from = 0;
			}
			fis.skip(from);
			byte[] bytes = ResourceUtilities
					.readStreamToByteArray(new BufferedInputStream(fis));
			events.end = from + bytes.length;
			logContents = new String(bytes, StandardCharsets.UTF_8);
		} catch (Exception e) {
			Ax.simpleExceptionOut(e);
			return events;
		}
		Pattern startPattern = Pattern.compile(start);
		Pattern endPattern = Pattern.compile(end);
		Matcher m0 = startPattern.matcher(logContents);
		CharBuffer charBuffer = CharBuffer.wrap(logContents);
		while (m0.find()) {
			Record record = new Record();
			record.parseStart(m0);
			CharBuffer remaining = charBuffer.subSequence(m0.end(),
					charBuffer.length());
			Matcher m1 = endPattern.matcher(remaining);
			if (m1.find()) {
				record.parseEnd(m1, remaining.subSequence(0, m1.start()));
			}
			if (record.gcTimeMillis > logPausesGtMillis) {
				events.records.add(record);
			}
		}
		return events;
	}

	public static class Events {
		public int end;

		public List<Record> records = new ArrayList<>();

		@Override
		public String toString() {
			return records.stream().map(Record::toString)
					.collect(Collectors.joining("\n"));
		}
	}

	static class Record {
		ZonedDateTime gcStart;

		ZonedDateTime gcEnd;

		long applicationRunningTimeMillis;

		long gcTimeMillis;

		Type type;

		CharSequence contents;

		public void parseEnd(Matcher m, CharSequence contents) {
			this.contents = contents;
			MatcherCounter c = new MatcherCounter(m);
			gcEnd = consumeDate(c);
			consumeTime(c);
			gcTimeMillis = (long) (consumeTime(c) * 1000);
		}

		public void parseStart(Matcher m) {
			MatcherCounter c = new MatcherCounter(m);
			gcStart = consumeDate(c);
			consumeTime(c);
			applicationRunningTimeMillis = (long) (consumeTime(c) * 1000);
		}

		@Override
		public String toString() {
			FormatBuilder fb = new FormatBuilder();
			fb.line("Event: %s", gcStart.toString().replaceFirst("\\[.+", ""));
			fb.line("Pause: %s millis", gcTimeMillis);
			fb.indent(2);
			fb.line("Prior application run time: %s millis",
					applicationRunningTimeMillis);
			fb.newLine();
			fb.appendBlock(contents.toString());
			return fb.toString();
		}

		private ZonedDateTime consumeDate(MatcherCounter c) {
			LocalDateTime ldt = LocalDateTime.of(c.nint(), c.nint(), c.nint(),
					c.nint(), c.nint(), c.nint(), c.nint() * 1000000);
			ZoneOffset offset = ZoneOffset.of(c.next());
			return ZonedDateTime.ofInstant(ldt, offset, ZoneId.systemDefault());
		}

		private double consumeTime(MatcherCounter c) {
			return Double.parseDouble(c.next());
		}

		static class MatcherCounter {
			private Matcher m;

			int groupCounter = 1;

			public MatcherCounter(Matcher m) {
				this.m = m;
			}

			public int nint() {
				return Integer.parseInt(next());
			}

			String next() {
				return m.group(groupCounter++);
			}
		}
	}

	enum Type {
		YOUNG, ALLOCATION_FAILURE, CMS
	}
}
