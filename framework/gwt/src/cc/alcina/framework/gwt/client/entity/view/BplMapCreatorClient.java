package cc.alcina.framework.gwt.client.entity.view;

import java.util.Map;

import com.google.gwt.core.shared.GWT;

import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder;
import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder.BplDelegateMapCreatorStd;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@Reflected
@Registration(BaseProjectionLookupBuilder.BplDelegateMapCreator.class)
public class BplMapCreatorClient extends BplDelegateMapCreatorStd {
	@Override
	public Map createDelegateMap(int depthFromRoot, int depth) {
		if (getBuilder().getProjection().getTypes() != null && GWT.isScript()) {
			return JsUniqueMap.create();
		}
		return super.createDelegateMap(depthFromRoot, depth);
	}
}
