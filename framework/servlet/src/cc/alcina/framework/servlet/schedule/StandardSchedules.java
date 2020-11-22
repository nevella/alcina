package cc.alcina.framework.servlet.schedule;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.servlet.job2.JobScheduler.ExecutorServiceProvider;
import cc.alcina.framework.servlet.job2.JobScheduler.Schedule;

public class StandardSchedules {
	@RegistryLocation(registryPoint = AdHocJobsExecutorServiceProvider.class, implementationType = ImplementationType.SINGLETON)
	public static class AdHocJobsExecutorServiceProvider
			implements ExecutorServiceProvider {
		public static StandardSchedules.AdHocJobsExecutorServiceProvider get() {
			return Registry.impl(
					StandardSchedules.AdHocJobsExecutorServiceProvider.class);
		}

		private ExecutorService service = Executors.newFixedThreadPool(4,
				new NamedThreadFactory("ad-hoc-jobs-pool"));

		@Override
		public ExecutorService getService() {
			return service;
		}
	}

	public static class AppStartupJobSchedule extends Schedule {
		public AppStartupJobSchedule() {
			withClustered(false).withQueueMaxConcurrentJobs(4)
					.withExcutorServiceProvider(
							RecurrentJobsExecutorServiceProvider.get());
		}
	}

	public static class DailySchedule extends Schedule {
		public DailySchedule() {
			withClustered(true).withTimewiseLimited(true)
					.withQueueMaxConcurrentJobs(1).withNext(LocalDateTime.now()
							.truncatedTo(ChronoUnit.DAYS).plusDays(1));
		}
	}

	// FIXME - mvcc.1a - JobScheduler schould schedule these for all vms, not
	// just leader
	public static class HourlyLocalSchedule extends Schedule {
		public HourlyLocalSchedule() {
			withClustered(false).withTimewiseLimited(true)
					.withQueueName(
							"hourly-" + EntityLayerUtils.getLocalHostName())
					.withQueueMaxConcurrentJobs(1).withNext(LocalDateTime.now()
							.truncatedTo(ChronoUnit.HOURS).plusHours(1));
		}
	}

	public static class HourlySchedule extends Schedule {
		public HourlySchedule() {
			withClustered(true).withTimewiseLimited(true)
					.withQueueMaxConcurrentJobs(1).withNext(LocalDateTime.now()
							.truncatedTo(ChronoUnit.HOURS).plusHours(1));
		}
	}

	public static class ImmediateLocalSchedule extends Schedule {
		public ImmediateLocalSchedule() {
			withClustered(false).withQueueMaxConcurrentJobs(1)
					.withNext(LocalDateTime.now());
		}
	}

	public static class MinuteSchedule extends Schedule {
		public MinuteSchedule() {
			withClustered(true).withTimewiseLimited(true)
					.withQueueMaxConcurrentJobs(1).withNext(LocalDateTime.now()
							.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1));
		}
	}

	public static class MonthlySchedule extends Schedule {
		public MonthlySchedule() {
			withClustered(true).withTimewiseLimited(true)
					.withQueueMaxConcurrentJobs(1)
					.withNext(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
							.withDayOfMonth(1).plusMonths(1));
		}
	}

	public static class RecurrentJobsExecutorSchedule extends Schedule {
		public RecurrentJobsExecutorSchedule() {
			withClustered(false).withQueueMaxConcurrentJobs(4)
					.withExcutorServiceProvider(
							RecurrentJobsExecutorServiceProvider.get());
		}
	}

	@RegistryLocation(registryPoint = RecurrentJobsExecutorServiceProvider.class, implementationType = ImplementationType.SINGLETON)
	public static class RecurrentJobsExecutorServiceProvider
			implements ExecutorServiceProvider {
		public static StandardSchedules.RecurrentJobsExecutorServiceProvider
				get() {
			return Registry.impl(
					StandardSchedules.RecurrentJobsExecutorServiceProvider.class);
		}

		private ExecutorService service = Executors.newFixedThreadPool(4,
				new NamedThreadFactory("recurrent-jobs-pool"));

		@Override
		public ExecutorService getService() {
			return service;
		}
	}

	public static class TenMinutesSchedule extends Schedule {
		public TenMinutesSchedule() {
			LocalDateTime now = LocalDateTime.now();
			withClustered(true).withQueueMaxConcurrentJobs(1)
					.withNext(now.truncatedTo(ChronoUnit.MINUTES)
							.withMinute(now.getMinute() / 10 * 10)
							.plusMinutes(10));
		}
	}

	public static class WeeklySchedule extends Schedule {
		public WeeklySchedule() {
			withClustered(true).withTimewiseLimited(true)
					.withQueueMaxConcurrentJobs(1)
					.withNext(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
							.with(DayOfWeek.SUNDAY).plusWeeks(1));
		}
	}
}
