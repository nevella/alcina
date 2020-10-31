package cc.alcina.framework.gwt.client.dirndl.activity;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

@RegistryLocation(registryPoint = DirectedCategoriesActivity.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
public class DirectedCategoriesActivity<CNP extends CategoryNamePlace>
		extends DirectedActivity<CNP> {
}
