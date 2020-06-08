package cc.alcina.framework.servlet;

import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;

public class Sx {
	public static boolean isTest() {
		return AppPersistenceBase.isTest();
	}

	public static boolean isTestServer() {
		return AppPersistenceBase.isTestServer();
	}

	public static void setTestServer(boolean testServer) {
		AppPersistenceBase.setTestServer(testServer);
	}
}
