package cc.alcina.framework.servlet.component.romcom;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.servlet.component.Feature_RemoteObjectComponent;

@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_RemoteObjectComponent.class)
@Feature.Type.Ref(Feature.Type.Ui_implementation.class)
public interface Feature_Romcom_Impl extends Feature {
	/*
	 * Notes - romcom has to do some fancy layer passing to set the server
	 * cookie post auth change (login/logout)
	 */
	@Feature.Parent(Feature_Romcom_Impl.class)
	public interface _Authentication extends Feature {
	}

	/*
	 * Romcom reuses the client transform manager (and per-client-instance
	 * Domain)
	 */
	@Feature.Parent(Feature_Romcom_Impl.class)
	public interface _Transforms extends Feature {
	}
}
