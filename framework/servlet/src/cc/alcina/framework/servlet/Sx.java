package cc.alcina.framework.servlet;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.cache.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfoLite;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

public class Sx {
	public static boolean nonThreadedCommitPoint;

	public static void commit() {
		ServletLayerUtils.pushTransformsAsRoot();
		commitPoint(false);
	}

	// optimisation - defer push to the end of an rpc call, so as to only do
	// once
	// assumes non-critical deltas
	public static void commitPoint() {
		commitPoint(true);
		// a better/more formal way would be to have some quick write-ahead (say
		// kafka) and recover on restart
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
					CommonRemoteServiceServlet.PUSH_TRANSFORMS_AT_END_OF_REUQEST,
					set);
		}
	}

	public static boolean isTest() {
		return AppPersistenceBase.isTest();
	}

	static boolean testServer;

	public static boolean isTestServer() {
		return testServer || isTest();
	}

	public static void setTestServer(boolean testServer) {
		Sx.testServer = testServer;
	}
}
