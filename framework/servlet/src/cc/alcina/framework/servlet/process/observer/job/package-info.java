/**
 * 
 * 
 * <pre>
 * <code>
 - what does it do?
    - (see JobObserver javadoc)
- how is it tested?
    - call getHistory with a locator which matches the filter
- how is it tested practically?
    - attach "JobObserver" - start a job - set a breakpoint - call getHistory().dump()

(attach JobObserver) ::
JobObserver.observe(new ObservableJobFilter.All());
MvccObserver.observe(new JobMvccObserver());

-- or - configuration --

JobScheduler.observeJobEvents=true

-- log the history --
System.out.println(cc.alcina.framework.servlet.process.observer.job.JobObserver.getHistory(
cc.alcina.framework.servlet.job.JobContext.get().getJob().toLocator()));

-- write the history to the local fs --
cc.alcina.framework.servlet.process.observer.job.JobObserver.getHistory(
cc.alcina.framework.servlet.job.JobContext.get().getJob().toLocator()).sequence().exportLocal();

- what does it interact with?
    - DomainStore/mvcc and the Job system
- what are the non-intuitives?
    - entities created by other cluster members may also be registered for observation
 * </code>
 * </pre>
 */
@Feature.Ref(Feature_JobHistory.class)
package cc.alcina.framework.servlet.process.observer.job;

import cc.alcina.framework.common.client.meta.Feature;
