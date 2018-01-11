package cc.alcina.framework.servlet;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.cache.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfoLite;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

public class Sx {
	public static void commit() {
		ServletLayerUtils.pushTransformsAsRoot();
	}

	// optimisation - defer push to the end of an rpc call, so as to only do
	// once
	// assumes non-critical deltas
	public static void commitPoint() {
		HttpServletRequest threadLocalRequest = CommonRemoteServiceServlet.getCrossServletThreadLocalRequest();
		if(threadLocalRequest==null){
			
		}else{
			threadLocalRequest.setAttribute(CommonRemoteServiceServlet.PUSH_TRANSFORMS_AT_END_OF_REUQEST, true);
		}
		// FIXME - dem3
	}

	

}
