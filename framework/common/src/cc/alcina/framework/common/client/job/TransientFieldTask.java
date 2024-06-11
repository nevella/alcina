package cc.alcina.framework.common.client.job;

/**
 * <p>
 * To elide AlcinaTransient(TransformType.JOB) properties from the _persisted_
 * serialized task - e.g. passwords
 * 
 * <p>
 * The non-persistent versions of these tasks are passed to the performer via
 * TransientFieldTasks
 * 
 * <p>
 * The prologue of the performer should always run something like:
 * 
 * <pre>
 * <code>
 * TransientFieldTasks.Resource resource = new TransientFieldTasks.Resource();
		JobContext.acquireResource(resource);
		ChangePasswordServerAction originalAction = resource.getTask();
		performAction(originalAction.getParameters());
 * </code>
 * </pre>
 */
public interface TransientFieldTask {
}
