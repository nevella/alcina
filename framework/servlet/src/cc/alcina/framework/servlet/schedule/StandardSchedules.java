package cc.alcina.framework.servlet.schedule;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.servlet.job2.JobScheduler.ExecutorServiceProvider;
import cc.alcina.framework.servlet.job2.JobScheduler.Schedule;

public class StandardSchedules {
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

	public static class TenSecondsSchedule extends Schedule {
		public TenSecondsSchedule() {
			withClustered(true).withQueueMaxConcurrentJobs(1)
					.withNext(LocalDateTime.now().plusSeconds(10));
		}
	}
}
