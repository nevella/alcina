package cc.alcina.framework.servlet;

import javax.servlet.http.HttpServletRequest;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.ServletLayerTransforms;

public class Sx {
	public static boolean nonThreadedCommitPoint;

	static boolean testServer;

	public static int commit() {
		int transformCount = ServletLayerTransforms.pushTransformsAsRoot();
		commitPoint(false);
		return transformCount;
	}

	public static int commitIfTransformCount(int n) {
		if (TransformManager.get().getTransforms().size() > n) {
			return commit();
		} else {
			return 0;
		}
	}

	// optimisation - defer push to the end of an rpc call, so as to only do
	// once
	// assumes non-critical deltas
	public static void commitPoint() {
		commitPoint(true);
		// a better/more formal way would be to have some quick write-ahead (say
		// kafka) and recover on restart
	}

	public static boolean isTest() {
		return AppPersistenceBase.isTest();
	}

	public static boolean isTestServer() {
		return testServer || isTest();
	}

	public static String ntrim(String s) {
		return SEUtilities.normalizeWhitespaceAndTrim(s);
	}

	public static void setTestServer(boolean testServer) {
		Sx.testServer = testServer;
	}

	private static void commitPoint(boolean set) {
		HttpServletRequest threadLocalRequest = CommonRemoteServiceServlet
				.getContextThreadLocalRequest();
		if (threadLocalRequest == null) {
			if (AppPersistenceBase.isTest()) {
				Sx.nonThreadedCommitPoint = set;
			}
		} else {
			threadLocalRequest.setAttribute(
					CommonRemoteServiceServlet.PUSH_TRANSFORMS_AT_END_OF_REQUEST,
					set);
		}
	}
}
