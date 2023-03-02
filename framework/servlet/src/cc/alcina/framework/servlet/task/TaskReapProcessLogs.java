package cc.alcina.framework.servlet.task;

import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.util.ProcessLogFolder;
import cc.alcina.framework.servlet.job.JobScheduler.Schedule;
import cc.alcina.framework.servlet.schedule.ServerTask;
import cc.alcina.framework.servlet.schedule.StandardSchedules.HourlyScheduleFactory;

public class TaskReapProcessLogs extends ServerTask {
	@Override
	public void run() throws Exception {
		List<String> omittedFolders = Arrays
				.asList(Configuration.get("omit").split(","));
		Registry.query(ProcessLogFolder.class).implementations()
				.filter(folder -> {
					if (omittedFolders.contains(folder.getFolder())) {
						logger.info("Omitted (preserve folder contents): {}",
								folder.getFolder());
						return true;
					} else {
						return false;
					}
				}).forEach(ProcessLogFolder::reap);
	}

	@Registration(
		value = { Schedule.class, TaskReapProcessLogs.class },
		implementation = Registration.Implementation.FACTORY)
	public static class ScheduleFactory extends HourlyScheduleFactory {
	}
}
