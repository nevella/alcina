package cc.alcina.framework.entity.entityaccess.metric;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class StartupStats {
	static Topic<String> topicEmitStat = Topic.local();

	public static Topic<String> topicEmitStat() {
		return topicEmitStat;
	}

	public StatResults parse(LogProvider logProvider) {
		StatResults result = new StatResults();
		result.stats = Registry.impls(StatProvider.class).stream()
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

		private Matcher getMatcher(Class<? extends StatCategory> category) {
			String regex = Ax.format(
					"[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} .*\\[alc-%s\\].+",
					category.getSimpleName());
			return Pattern.compile(regex).matcher(logProvider.getLog());
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
	}

	@RegistryLocation(registryPoint = StatProvider.class)
	public static abstract class StatProvider {
		protected LogProvider logProvider;

		public abstract StatCategory category();

		public abstract String findEndLine();

		public abstract String findStartLine();

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

		Stat generate(LogProvider logProvider) {
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
	}

	public static class StatResults {
		List<Stat> stats;

		private String template = "%-40s %10s\n";

		public void dump() {
			Collections.sort(stats);
			System.out.format(template, "Stat", "Duration");
			for (Stat stat : stats) {
				String prefix = CommonUtils.padStringLeft("", stat.depth(),
						' ');
				System.out.format(template, prefix + stat.name(),
						stat.duration());
			}
		}

		public void save() {
		}
	}

	static class Stat implements Comparable<Stat> {
		public StatProvider provider;

		long start;

		long end;

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
