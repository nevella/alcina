package cc.alcina.extras.dev.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.stat.DevStats;
import cc.alcina.framework.entity.stat.DevStats.LogProvider;

public class ConsoleStatLogProvider implements LogProvider {
	List<String> stats = Collections.synchronizedList(new ArrayList<>());

	BlockingQueue<String> publishRemote = new LinkedBlockingQueue<>();

	String log;

	private Thread pushRemote = new Thread() {
		@Override
		public void run() {
			try {
				while (true) {
					String message = publishRemote.take();
					CommonPersistenceProvider.get().getCommonPersistence()
							.log(message, LogMessageType.DEV_METRIC.toString());
				}
			} catch (Throwable e) {
				Ax.simpleExceptionOut(e);
			}
		}
	};

	public ConsoleStatLogProvider() {
		pushRemote.setDaemon(true);
		DevStats.topicEmitStat().add((k, v) -> {
			stats.add(v);
			publishRemote.add(v);
		});
	}

	@Override
	public String getLog() {
		if (log == null) {
			log = stats.stream().collect(Collectors.joining("\n"));
		}
		return log;
	}

	public void startRemote() {
		pushRemote.start();
	}
}