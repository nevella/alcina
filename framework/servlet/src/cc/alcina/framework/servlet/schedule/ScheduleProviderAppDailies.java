package cc.alcina.framework.servlet.schedule;

import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.servlet.job2.JobScheduler.Schedule;
import cc.alcina.framework.servlet.job2.JobScheduler.ScheduleProvider;

@RegistryLocation(registryPoint = ScheduleProvider.class, targetClass = AppDailies.class, implementationType = ImplementationType.INSTANCE)
public class ScheduleProviderAppDailies implements ScheduleProvider {
	@Override
	public Schedule getSchedule(Task task, boolean onAppplicationStart) {
		return new StandardSchedules.DailySchedule();
	}
}
