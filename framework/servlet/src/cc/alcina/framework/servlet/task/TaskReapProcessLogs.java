package cc.alcina.framework.servlet.task;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.ProcessCounter;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.servlet.job.JobScheduler.Schedule;
import cc.alcina.framework.servlet.schedule.ServerTask;
import cc.alcina.framework.servlet.schedule.StandardSchedules.HourlyScheduleFactory;

public class TaskReapProcessLogs extends ServerTask<TaskReapProcessLogs> {
	@Override
	protected void performAction0(TaskReapProcessLogs task) throws Exception {
		Registry.query(ProcessLogFolder.class).implementations()
				.forEach(ProcessLogFolder::reap);
	}

	@Registration(ProcessLogFolder.class)
	public static abstract class ProcessLogFolder {
		Logger logger = LoggerFactory.getLogger(getClass());

		public abstract String getFolder();

		public void reap() {
			if (Arrays
					.asList(ResourceUtilities
							.get(TaskReapProcessLogs.class, "omit").split(","))
					.contains(getFolder())) {
				logger.info("Omitted (preserve folder contents): {}",
						getFolder());
				return;
			}
			File file = DataFolderProvider.get().getChildFile(getFolder());
			file.mkdirs();
			List<File> files = SEUtilities.listFilesRecursive(file.getPath(),
					null, true);
			ProcessCounter counter = new ProcessCounter();
			files.stream().peek(f -> counter.visited()).filter(
					f -> TimeConstants.within(f.lastModified(), maxAge()))
					.forEach(f -> {
						f.delete();
						counter.modified();
					});
			logger.info("Reaped {} - {}", getFolder(), counter);
		}

		protected long maxAge() {
			return TimeConstants.ONE_DAY_MS;
		}
	}

	@Registration(value = { Schedule.class,
			TaskReapProcessLogs.class }, implementation = Registration.Implementation.FACTORY)
	public static class ScheduleFactory extends HourlyScheduleFactory {
	}
}
