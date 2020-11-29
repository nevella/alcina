package cc.alcina.framework.servlet.schedule;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutionConstraints;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutorServiceProvider;
import cc.alcina.framework.servlet.job.JobScheduler.ResubmitPolicy;
import cc.alcina.framework.servlet.job.JobScheduler.Schedule;

public class StandardSchedules {
	public static class AppStartupSchedule extends Schedule {
		public AppStartupSchedule() {
			withVmLocal(true);
		}

		@Override
		public LocalDateTime getNext(boolean applicationStartup) {
			return applicationStartup ? LocalDateTime.now() : null;
		}
	}

	public static class AppStartupScheduleFactory
			implements RegistryFactory<Schedule> {
		@Override
		public Schedule impl() {
			return new StandardSchedules.AppStartupSchedule();
		}
	}

	public static class DailySchedule extends Schedule {
		public DailySchedule() {
			withNext(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
					.plusDays(1));
		}
	}

	public static class DailyScheduleFactory
			implements RegistryFactory<Schedule> {
		@Override
		public Schedule impl() {
			return new StandardSchedules.DailySchedule();
		}
	}

	public static class HourlySchedule extends Schedule {
		public HourlySchedule() {
			withNext(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
					.plusHours(1));
		}

		public HourlySchedule withOffsetMinutes(int offsetMinutes) {
			LocalDateTime next = LocalDateTime.now()
					.truncatedTo(ChronoUnit.HOURS).plusMinutes(offsetMinutes);
			if (LocalDateTime.now().isAfter(next)) {
				next = next.plusHours(1);
			}
			withNext(next);
			return this;
		}
	}

	public static class HourlyScheduleFactory
			implements RegistryFactory<Schedule> {
		@Override
		public Schedule impl() {
			return new StandardSchedules.HourlySchedule();
		}
	}

	public static class MonthlySchedule extends Schedule {
		public MonthlySchedule() {
			withNext(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
					.withDayOfMonth(1).plusMonths(1));
		}
	}

	public static class MonthlyScheduleFactory
			implements RegistryFactory<Schedule> {
		@Override
		public Schedule impl() {
			return new StandardSchedules.MonthlySchedule();
		}
	}

	public static class RecurrentExecutionConstraintsFactory
			implements RegistryFactory<ExecutionConstraints> {
		@Override
		public ExecutionConstraints impl() {
			return new ExecutionConstraints()
					.withDescendantExecutorServiceProvider(
							RecurrentJobsExecutorServiceProvider.get());
		}
	}

	@RegistryLocation(registryPoint = RecurrentJobsExecutorServiceProvider.class, implementationType = ImplementationType.SINGLETON)
	public static class RecurrentJobsExecutorServiceProvider
			implements ExecutorServiceProvider {
		public static RecurrentJobsExecutorServiceProvider get() {
			return Registry.impl(RecurrentJobsExecutorServiceProvider.class);
		}

		private ExecutorService service = Executors.newFixedThreadPool(4,
				new NamedThreadFactory("recurrent-jobs-pool"));

		@Override
		public ExecutorService getService(AllocationQueue queue) {
			return service;
		}
	}

	public static class RecurrentResubmitFactory
			implements RegistryFactory<ResubmitPolicy> {
		@Override
		public ResubmitPolicy impl() {
			return ResubmitPolicy.retryNTimes(2);
		}
	}

	public static class TenMinutesSchedule extends Schedule {
		public TenMinutesSchedule() {
			LocalDateTime now = LocalDateTime.now();
			withNext(now.truncatedTo(ChronoUnit.MINUTES)
					.withMinute(now.getMinute() / 10 * 10).plusMinutes(10));
		}
	}

	public static class TenMinutesScheduleFactory
			implements RegistryFactory<Schedule> {
		@Override
		public Schedule impl() {
			return new StandardSchedules.TenMinutesSchedule();
		}
	}

	public static class WeeklySchedule extends Schedule {
		public WeeklySchedule() {
			withNext(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
					.with(DayOfWeek.SUNDAY).plusWeeks(1));
		}
	}

	public static class WeeklyScheduleFactory
			implements RegistryFactory<Schedule> {
		@Override
		public Schedule impl() {
			return new StandardSchedules.WeeklySchedule();
		}
	}
}
