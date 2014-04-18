package cc.alcina.framework.common.client.actions;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.GwtTransient;

public class ActionResult<T> {
	public ActionLogItem actionLogItem;
	public T resultObject;
}
