package cc.alcina.framework.servlet.task;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobScheduler.RetentionPolicy;
import cc.alcina.framework.servlet.job.JobScheduler.Schedule;
import cc.alcina.framework.servlet.schedule.ServerTask;
import cc.alcina.framework.servlet.schedule.StandardSchedules.HourlyScheduleFactory;
import cc.alcina.framework.common.client.logic.reflection.Registration;

public class TaskReapJobs extends ServerTask<TaskReapJobs> {

    @Override
    protected void performAction0(TaskReapJobs task) throws Exception {
        Stream<? extends Job> jobs = JobDomain.get().getAllJobs();
        if (Ax.notBlank(value)) {
            jobs = Stream.of(Job.byId(Long.valueOf(value)));
        }
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger reaped = new AtomicInteger(0);
        AtomicInteger exceptions = new AtomicInteger(0);
        jobs.forEach(job -> {
            boolean delete = false;
            if (!job.provideCanDeserializeTask()) {
                if (job.provideIsNotComplete()) {
                } else {
                    Date date = job.resolveCompletionDate();
                    if (date == null) {
                        // invalid job, clear
                        delete = true;
                    } else {
                        // allow for tmp classes loaded into other vms
                        delete = System.currentTimeMillis() - date.getTime() > TimeConstants.ONE_DAY_MS;
                    }
                }
            } else {
                try {
                    RetentionPolicy policy = Registry.impl(RetentionPolicy.class, job.getTask().getClass());
                    delete = !policy.retain(job);
                } catch (Exception e) {
                    if (exceptions.incrementAndGet() < 20) {
                        e.printStackTrace();
                    }
                }
            }
            if (delete) {
                reaped.incrementAndGet();
                job.delete();
            }
            if (counter.incrementAndGet() % 100000 == 0 || TransformManager.get().getTransforms().size() > 500) {
                logger.info("Reaping jobs: counter {} - transforms {} - jobs {}", counter.get(), TransformManager.get().getTransforms().size(), Domain.size(PersistentImpl.getImplementation(Job.class)));
                Transaction.commit();
                Transaction.endAndBeginNew();
            }
            JobContext.checkCancelled();
        });
        Transaction.commit();
        logger.info("Reaped {} jobs", reaped.get());
    }

    @RegistryLocation(registryPoint = Schedule.class, targetClass = TaskReapJobs.class, implementationType = ImplementationType.FACTORY)
    @Registration(value = { Schedule.class, TaskReapJobs.class }, implementation = Registration.Implementation.FACTORY)
    public static class ScheduleFactory extends HourlyScheduleFactory {
    }
}
