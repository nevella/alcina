package cc.alcina.framework.servlet.servlet.remote;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class RemoteInvocationParameters implements Serializable {
	public String interfaceClassName;

	public String methodName;

	public Object[] args;

	public RemoteInvocationParameters.Api api = Api.EJB_BEAN_PROVIDER;

	public long clientInstanceId;

	public int clientInstanceAuth;

	public boolean asRoot;

	public Map<String, Object> context = new LinkedHashMap<>();

	public enum Api {
		EJB_BEAN_PROVIDER, GWT_REMOTE_SERVICE_IMPL;

		boolean isAllowAsRoot() {
			switch (this) {
			case EJB_BEAN_PROVIDER:
				return true;
			default:
				return false;
			}
		}

		boolean isAllowWithoutClientInstance() {
			switch (this) {
			case EJB_BEAN_PROVIDER:
				return true;
			default:
				return false;
			}
		}

		boolean isLinkToDomain(String methodName) {
			switch (this) {
			case EJB_BEAN_PROVIDER:
				switch (methodName) {
				case "callWithEntityManager":
					return false;
				default:
					return true;
				}
			default:
				return false;
			}
		}
	}
}