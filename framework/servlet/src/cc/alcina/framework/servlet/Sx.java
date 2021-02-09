package cc.alcina.framework.servlet;

import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;

public class Sx {
	public static boolean isProduction() {
		return !EntityLayerUtils.isTestOrTestServer();
	}

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
