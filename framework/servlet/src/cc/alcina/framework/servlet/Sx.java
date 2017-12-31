package cc.alcina.framework.servlet;

import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.cache.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfoLite;

public class Sx {
	public static void commit() {
		ServletLayerUtils.pushTransformsAsRoot();
	}

	// optimisation - defer push to the end of an rpc call, so as to only do
	// once
	// assumes non-critical deltas
	public static void commitPoint() {
		// FIXME - dem3
	}

	

	public static void commitOffThread() {
		commit();
	}
}
