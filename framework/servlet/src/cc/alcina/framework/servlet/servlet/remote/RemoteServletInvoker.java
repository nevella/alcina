package cc.alcina.framework.servlet.servlet.remote;

import cc.alcina.framework.common.client.job.Task;

/**
 * Used by consoles to call remote alcina servers. Should only be implemented by
 * a console-visible class (not dev server-visible)
 */
public interface RemoteServletInvoker {
	String invokeRemoteTaskReturnResult(Task task);
}
