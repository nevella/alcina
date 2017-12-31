package cc.alcina.framework.servlet.servlet.dev;

import java.io.Serializable;

public class DevRemoterParams implements Serializable {
	public String interfaceClassName;

	public String methodName;

	public Object[] args;

	public String username;

	public boolean cleanEntities;

	public DevRemoterParams.DevRemoterApi api = DevRemoterApi.EJB_BEAN_PROVIDER;

	public enum DevRemoterApi {
		EJB_BEAN_PROVIDER, GWT_REMOTE_SERVICE_IMPL
	}
}