/**
 * <h2>Process metrics to assist optimisation (works for client-server too)</h2>
 *
 * <p>
 * This package provides support for structured metrics - generation, collation
 * and reporting
 *
 * <p>
 * An example:
 *
 * <h3>Construct a metric structure</h3>
 *
 * <code><pre>
 *
 *public class StatCategory_DomainStore extends StatCategory {
  public StatCategory_DomainStore() {
    super(StatCategory_Console.class, "domain-store");
  }

  public static class GetDescriptor extends StatCategory {
    public GetDescriptor() {
      super(StatCategory_DomainStore.class, "get-descriptor");
    }
  }
  ...
 * </code>
 * </pre>
 *
 * <h3>Make some stats (start, end events):</h3>
 *
 *
 * <p>
 * Note, there might be some easier way to do this - but not a huge amount
 * easier
 *
 * <code><pre>
 public static class ConsoleStat_StatCategory_DomainStore extends KeyedStat {
  public ConsoleStat_StatCategory_DomainStore() {
    super(StatCategory_DomainStore.Start.class,
        StatCategory_DomainStore.class);
  }

  public static class GetDescriptor extends KeyedStat {
    public GetDescriptor() {
      super(StatCategory_DomainStore.Start.class,
          StatCategory_DomainStore.GetDescriptor.class);
    }
  }
  ...
 * </code>
 * </pre>
 *
 * <h3>Emit some events:</h3>
 *
 * <code><pre>
 * new StatCategory_DomainStore().emit();

  ...

 new StatCategory_DomainStore.GetDescriptor().emit();
 *   </code>
 * </pre>
 *
 * <h3>Collate and output:</h3>
 * <p>
 * See {@link TaskReportDevMetrics}
 *
 *
 * <code><pre>
 *  TaskReportDevMetrics task = new TaskReportDevMetrics();
  task.setMostRecent(true);
  task.setFrom(SEUtilities.toOldDate(LocalDate.now().minusDays(5)));
  task.setKeyedStatClassName(ConsoleStatAll.class.getName());
  task.setWithMissed(true);
  Job performed = task.perform();
  performed.domain().ensurePopulated();
  ResourceUtilities.logToFile(performed.getLargeResult().toString());
</code>
 * </pre>
 *
 * j
 *
 */
package cc.alcina.framework.entity.stat;
