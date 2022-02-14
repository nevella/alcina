package cc.alcina.framework.entity.stat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

@Registration(ClearStaticFieldsOnAppShutdown.class)
public class DevStats {
	static Topic<String> topicEmitStat = Topic.local();

	public static Topic<String> topicEmitStat() {
		return topicEmitStat;
	}

	public StatResults parse(LogProvider logProvider) {
		StatResults result = new StatResults();
		result.stats = Registry.query(StatProvider.class).implementations()
				.map(parser -> parser.generate(logProvider))
				.filter(Objects::nonNull).collect(Collectors.toList());
		return result;
	}

	public static abstract class KeyedStat extends StatProvider {
		private Class<? extends StatCategory> start;

		private Class<? extends StatCategory> end;

		private StatCategory category;

		public KeyedStat(Class<? extends StatCategory> start,
				Class<? extends StatCategory> end) {
			this(start, end, Reflections.newInstance(end));
		}

		public KeyedStat(Class<? extends StatCategory> start,
				Class<? extends StatCategory> end, StatCategory category) {
			this.start = start;
			this.end = end;
			this.category = category;
		}

		@Override
		public StatCategory category() {
			return category;
		}

		@Override
		public String findEndLine() {
			Matcher matcher = getMatcher(endCategory());
			if (matcher.find()) {
				return matcher.group();
			} else {
				return null;
			}
		}

		@Override
		public String findStartLine() {
			Matcher matcher = getMatcher(startCategory());
			if (matcher.find()) {
				return matcher.group();
			} else {
				return null;
			}
		}

		public List<String> listStats() {
			Matcher startMatcher = getMatcher(startCategory());
			Matcher endMatcher = getMatcher(endCategory());
			List<String> result = new ArrayList<>();
			int lastEnd = -1;
			int lastStart = -1;
			while (startMatcher.find()) {
				int start = startMatcher.start();
				flush(lastStart, lastEnd, start, result);
				lastStart = start;
				while (endMatcher.find()) {
					if (endMatcher.start() > lastStart) {
						lastEnd = endMatcher.end();
						break;
					}
				}
			}
			flush(lastStart, lastEnd, -1, result);
			return result;
		}

		private void flush(int lastStart, int lastEnd, int start,
				List<String> result) {
			if (lastStart == -1 || lastEnd == -1) {
				return;
			}
			if (start == -1 || lastEnd < start) {
				result.add(getLogProvider().getLog().substring(lastStart,
						lastEnd));
			}
		}

		private Matcher getMatcher(Class<? extends StatCategory> category) {
			String regex = Ax.format(
					"[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} .*\\[alc-%s\\].+",
					category.getCanonicalName());
			return Pattern.compile(regex).matcher(getLogProvider().getLog());
		}

		protected Class<? extends StatCategory> endCategory() {
			return end;
		}

		protected Class<? extends StatCategory> startCategory() {
			return start;
		}
	}

	@FunctionalInterface
	public interface LogProvider {
		public String getLog();

		public static class StringLogProvider implements LogProvider {
			private String log;

			public StringLogProvider(String log) {
				this.log = log;
			}

			@Override
			public String getLog() {
				return this.log;
			}

			public void setLog(String log) {
				this.log = log;
			}
		}
	}

	@Registration(StatProvider.class)
	public static abstract class StatProvider {
		private LogProvider logProvider;

		public abstract StatCategory category();

		public abstract String findEndLine();

		public abstract String findStartLine();

		public Stat generate(LogProvider logProvider) {
			this.logProvider = logProvider;
			String startLine = findStartLine();
			String endLine = findEndLine();
			if (startLine == null || endLine == null) {
				return null;
			}
			Stat stat = new Stat();
			stat.provider = this;
			stat.start = parseTime(startLine).getTime();
			stat.end = parseTime(endLine).getTime();
			return stat;
		}

		public LogProvider getLogProvider() {
			return this.logProvider;
		}

		public void setLogProvider(LogProvider logProvider) {
			this.logProvider = logProvider;
		}

		protected int depth() {
			return category().depth();
		}

		protected boolean isParallel() {
			return false;
		}

		protected Date parseTime(String line) {
			String datePart = line.replaceFirst("^(\\S+)\\s+.*$", "$1");
			try {
				return new SimpleDateFormat("HH:mm:ss,SSS").parse(datePart);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public static class StatResults {
		List<Stat> stats;

		private String template = "%-30s %20s\n";

		public void dump() {
			dump(false);
		}

		public void dump(boolean withMissed) {
			System.out.println(dumpString(withMissed));
		}

		public String dumpString(boolean withMissed) {
			Collections.sort(stats);
			StringBuilder sb = new StringBuilder();
			sb.append(String.format(template, "Stat", "Duration"));
			int maxDepth = stats.stream().map(Stat::depth)
					.max(Comparator.naturalOrder()).get();
			for (int depth = maxDepth; depth > 0; depth--) {
				for (Stat stat : stats) {
					if (stat.depth() == depth) {
						for (Stat parent : stats) {
							if (parent.depth() == depth - 1
									&& parent.start <= stat.start
									&& parent.end >= stat.end) {
								parent.childDuration += stat.duration();
							}
						}
					}
				}
			}
			for (Stat stat : stats) {
				String prefix = CommonUtils.padStringLeft("", stat.depth(),
						' ');
				sb.append(String.format(template, prefix + stat.name(),
						stat.duration()));
			}
			if (withMissed) {
				sb.append("\n");
				sb.append(String.format(template, "Stat", "Missed"));
				for (Stat stat : stats) {
					if (stat.duration() == stat.childDuration
							|| stat.childDuration == 0) {
						continue;
					}
					String prefix = CommonUtils.padStringLeft("", stat.depth(),
							' ');
					long diff = stat.duration() - stat.childDuration;
					sb.append(String.format(template, prefix + stat.name(),
							diff + " : " + String.format("%.2f",
									((double) diff) / stat.duration() * 100)));
				}
			}
			return sb.toString();
		}

		public Date getStartTime() {
			return new Date(stats.get(0).start);
		}

		public void save() {
		}
	}

	static class Stat implements Comparable<Stat> {
		public StatProvider provider;

		long start;

		long end;

		long childDuration;

		@Override
		public int compareTo(Stat o) {
			int compareStart = CommonUtils.compareLongs(start, o.start);
			if (compareStart != 0) {
				return compareStart;
			}
			int compareEnd = CommonUtils.compareLongs(end, o.end);
			return -compareEnd;
		}

		public int depth() {
			return provider.depth();
		}

		public long duration() {
			return end - start;
		}

		public String name() {
			return provider.category().name();
		}
	}
}
