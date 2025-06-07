package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.util.Date;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _Job job = new _Job();
    
    public static class _Job implements TypedProperty.Container {
      public TypedProperty<Job, String> cause = new TypedProperty<>(Job.class, "cause");
      public TypedProperty<Job, Double> completion = new TypedProperty<>(Job.class, "completion");
      public TypedProperty<Job, String> consistencyPriority = new TypedProperty<>(Job.class, "consistencyPriority");
      public TypedProperty<Job, Date> creationDate = new TypedProperty<>(Job.class, "creationDate");
      public TypedProperty<Job, ClientInstance> creator = new TypedProperty<>(Job.class, "creator");
      public TypedProperty<Job, Date> endTime = new TypedProperty<>(Job.class, "endTime");
      public TypedProperty<Job, Set> fromRelations = new TypedProperty<>(Job.class, "fromRelations");
      public TypedProperty<Job, Long> id = new TypedProperty<>(Job.class, "id");
      public TypedProperty<Job, Object> largeResult = new TypedProperty<>(Job.class, "largeResult");
      public TypedProperty<Job, String> largeResultSerialized = new TypedProperty<>(Job.class, "largeResultSerialized");
      public TypedProperty<Job, Date> lastModificationDate = new TypedProperty<>(Job.class, "lastModificationDate");
      public TypedProperty<Job, Long> localId = new TypedProperty<>(Job.class, "localId");
      public TypedProperty<Job, String> log = new TypedProperty<>(Job.class, "log");
      public TypedProperty<Job, ClientInstance> performer = new TypedProperty<>(Job.class, "performer");
      public TypedProperty<Job, Integer> performerVersionNumber = new TypedProperty<>(Job.class, "performerVersionNumber");
      public TypedProperty<Job, Job.ProcessState> processState = new TypedProperty<>(Job.class, "processState");
      public TypedProperty<Job, String> processStateSerialized = new TypedProperty<>(Job.class, "processStateSerialized");
      public TypedProperty<Job, Object> result = new TypedProperty<>(Job.class, "result");
      public TypedProperty<Job, String> resultMessage = new TypedProperty<>(Job.class, "resultMessage");
      public TypedProperty<Job, String> resultSerialized = new TypedProperty<>(Job.class, "resultSerialized");
      public TypedProperty<Job, JobResultType> resultType = new TypedProperty<>(Job.class, "resultType");
      public TypedProperty<Job, Date> runAt = new TypedProperty<>(Job.class, "runAt");
      public TypedProperty<Job, Date> startTime = new TypedProperty<>(Job.class, "startTime");
      public TypedProperty<Job, JobState> state = new TypedProperty<>(Job.class, "state");
      public TypedProperty<Job, Set> stateMessages = new TypedProperty<>(Job.class, "stateMessages");
      public TypedProperty<Job, String> statusMessage = new TypedProperty<>(Job.class, "statusMessage");
      public TypedProperty<Job, Task> task = new TypedProperty<>(Job.class, "task");
      public TypedProperty<Job, String> taskClassName = new TypedProperty<>(Job.class, "taskClassName");
      public TypedProperty<Job, String> taskSerialized = new TypedProperty<>(Job.class, "taskSerialized");
      public TypedProperty<Job, String> taskSignature = new TypedProperty<>(Job.class, "taskSignature");
      public TypedProperty<Job, Set> toRelations = new TypedProperty<>(Job.class, "toRelations");
      public TypedProperty<Job, String> uuid = new TypedProperty<>(Job.class, "uuid");
      public TypedProperty<Job, Integer> versionNumber = new TypedProperty<>(Job.class, "versionNumber");
    }
    
//@formatter:on
}
