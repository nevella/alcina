package cc.alcina.extras.dev.console.test;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainStoreProperty;
import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyLoadType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.cache.DomainStoreDescriptor.TestSupport;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityLazyPropertyTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	@Override
	protected void run0() throws Exception {
		Class<? extends Entity> clazz = Registry.impl(TestSupport.class)
				.getTypeWithLazyProperties();
		List<PropertyDescriptor> withLazy = SEUtilities
				.getSortedPropertyDescriptors(clazz).stream().filter(pd -> {
					DomainStoreProperty domainStoreProperty = pd.getReadMethod()
							.getAnnotation(DomainStoreProperty.class);
					if (domainStoreProperty != null) {
						DomainStorePropertyLoadType loadType = domainStoreProperty
								.loadType();
						if (loadType == DomainStorePropertyLoadType.LAZY) {
							return true;
						}
					}
					return false;
				}).collect(Collectors.toList());
		Domain.stream(clazz).limit(2).forEach(e -> {
			try {
				for (PropertyDescriptor pd : withLazy) {
					Object invoke = pd.getReadMethod().invoke(e, new Object[0]);
					Preconditions.checkNotNull(invoke);
					Ax.out("%s - %s - %s chars", e.toStringEntity(),
							pd.getName(), invoke.toString().length());
				}
			} catch (Exception e2) {
				throw new WrappedRuntimeException(e2);
			}
		});
	}
}
