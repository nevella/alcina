package cc.alcina.framework.common.client.job;

/*
 * Marker interface to differentiate tasks from non-root tasks (most tasks will
 * be descendants of PerformerTask, NonRootTask or RemoteAction, this wraps the
 * outliers)
 */
public interface RootTask extends Task {
}
