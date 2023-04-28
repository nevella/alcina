package cc.alcina.framework.servlet.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.stat.DevStats;
import cc.alcina.framework.entity.stat.DevStats.KeyedStat;
import cc.alcina.framework.entity.stat.DevStats.LogProvider;
import cc.alcina.framework.entity.stat.DevStats.LogProvider.StringLogProvider;
import cc.alcina.framework.entity.stat.DevStats.StatResults;
import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.job.JobContext;

public class TaskReportDevMetrics extends PerformerTask {
	private Date from = new Date(
			System.currentTimeMillis() - TimeConstants.ONE_DAY_MS);

	private Date to = new Date(System.currentTimeMillis());

	private boolean mostRecent = true;

	private String keyedStatClassName;

	private boolean withMissed;

	public Date getFrom() {
		return this.from;
	}

	public String getKeyedStatClassName() {
		return this.keyedStatClassName;
	}

	public Date getTo() {
		return this.to;
	}

	public boolean isMostRecent() {
		return this.mostRecent;
	}

	public boolean isWithMissed() {
		return this.withMissed;
	}

	public void setFrom(Date from) {
		this.from = from;
	}

	public void setKeyedStatClassName(String keyedStatClassName) {
		this.keyedStatClassName = keyedStatClassName;
	}

	public void setMostRecent(boolean mostRecent) {
		this.mostRecent = mostRecent;
	}

	public void setTo(Date to) {
		this.to = to;
	}

	public void setWithMissed(boolean withMissed) {
		this.withMissed = withMissed;
	}

	@Override
	public void run() throws Exception  {
		DomDocument doc = DomDocument.basicHtmlDoc();
		String css = Io.read().resource("res/TaskReportDevMetrics.css")
				.asString();
		doc.xpath("//head").node().builder().tag("style").text(css).append();
		List<ILogRecord> records = Registry.impl(DevMetricLogSearcher.class)
				.search(this);
		int offset = 0;
		LogProvider provider = new LogProvider() {
			private String log;

			@Override
			public String getLog() {
				if (log == null) {
					log = records.stream().map(ILogRecord::getText)
							.collect(Collectors.joining("\n"));
				}
				return log;
			}
		};
		List<LogProvider> statSequences = new ArrayList<>();
		Class<? extends KeyedStat> clazz = Reflections
				.forName(keyedStatClassName);
		KeyedStat keyedStat = Reflections.newInstance(clazz);
		keyedStat.setLogProvider(provider);
		String loglog = provider.getLog();
		List<String> statSequenceStrings = keyedStat.listStats();
		if (mostRecent) {
			String last = CommonUtils.last(statSequenceStrings);
			if (last != null) {
				statSequences.add(new StringLogProvider(last));
			}
		} else {
			statSequenceStrings.stream().map(StringLogProvider::new)
					.forEach(statSequences::add);
			Collections.reverse(statSequences);
		}
		DomNode docHead = doc.html().body().builder().tag("div")
				.className("head").append();
		docHead.builder().tag("h2").text("Dev metrics").append();
		DomNodeHtmlTableBuilder table = docHead.html().tableBuilder();
		table.row().text("Class").text(keyedStatClassName);
		table.append();
		statSequences.forEach(lp -> {
			StatResults stats = new DevStats().parse(lp);
			String string = stats.dumpString(withMissed);
			Date first = stats.getStartTime();
			DomNode metric = docHead.builder().tag("div").text("metric")
					.append();
			metric.builder().tag("div")
					.text("Timestamp: %s", Ax.timestamp(first)).append();
			metric.builder().tag("pre").text(string).append();
		});
		JobContext.get().getJob().setLargeResult(doc.prettyToString());
		logger.info("Log output to job.largeResult");
	}

	@Registration(DevMetricLogSearcher.class)
	public static abstract class DevMetricLogSearcher {
		public abstract List<ILogRecord>
				search(TaskReportDevMetrics taskReportDevMetrics);
	}
}
