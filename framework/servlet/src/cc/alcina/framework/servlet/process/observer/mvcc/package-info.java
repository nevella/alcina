/**
 * 
 * 
 * <pre>
 * <code>
 - what does it do?
    - (see class javadoc)
- how is it tested?
    - call getHistory with a locator which matches the filter
- how is it tested practically?
    - attach "jobobserver" - start a job - set a breakpoint - call getHistory().dump()

System.out.println(cc.alcina.framework.servlet.process.observer.mvcc.MvccObserver.getHistory(
cc.alcina.framework.servlet.job.JobContext.get().getJob().toLocator()));

- what does it interact with?
    - DomainStore/mvcc and the Job system
- what are the non-intuitives?
    - entities created by other cluster members may also be registered for observation
 * </code>
 * </pre>
 */
@Feature.Ref(Feature_MvccHistory.class)
package cc.alcina.framework.servlet.process.observer.mvcc;

import cc.alcina.framework.common.client.meta.Feature;
