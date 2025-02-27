package cc.alcina.framework.servlet.task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * Simplistic url verifier
 */
public class TaskVerifyUrls extends PerformerTask.Remote {
	public List<String> urls;

	public List<Result> results;

	public static class Result {
		public Result(String url) {
			this.url = url;
		}

		public String url;

		public boolean ok;

		public String exceptionMessage;

		public void load() {
			try {
				Io.read().url(url).asString();
				ok = true;
			} catch (Exception e) {
				exceptionMessage = CommonUtils.toSimpleExceptionMessage(e);
			}
		}
	}

	@Override
	public void run() {
		results = new ArrayList<>();
		SystemoutCounter counter = SystemoutCounter.standardJobCounter(urls,
				this);
		for (String url : urls) {
			Result result = new Result(url);
			results.add(result);
			result.load();
			counter.tick();
		}
	}

	public String asInvalidUrlStringList() {
		return results.stream().filter(r -> !r.ok).map(r -> r.url)
				.collect(Collectors.joining("\n"));
	}
}
