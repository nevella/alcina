package cc.alcina.framework.gwt.client.dirndl.activity;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

@RegistryLocation(registryPoint = DirectedCategoryActivity.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
@Registration(DirectedCategoryActivity.class)
public class DirectedCategoryActivity<CNP extends CategoryNamePlace>
		extends DirectedActivity<CNP> {
}
