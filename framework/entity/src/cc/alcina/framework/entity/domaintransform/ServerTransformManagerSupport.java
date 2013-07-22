package cc.alcina.framework.entity.domaintransform;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.util.CommonUtils;

public class ServerTransformManagerSupport {
	public void removeAssociations(HasIdAndLocalId hili) {
		try {
			PropertyDescriptor[] pds = Introspector
					.getBeanInfo(hili.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (Set.class.isAssignableFrom(pd.getPropertyType())) {
					Association info = pd.getReadMethod().getAnnotation(
							Association.class);
					Set set = (Set) pd.getReadMethod().invoke(hili,
							CommonUtils.EMPTY_OBJECT_ARRAY);
					if (info != null && set != null) {
						for (Object o2 : set) {
							String accessorName = "get"
									+ CommonUtils.capitaliseFirst(info
											.propertyName());
							Object o3 = o2.getClass()
									.getMethod(accessorName, new Class[0])
									.invoke(o2, CommonUtils.EMPTY_OBJECT_ARRAY);
							if (o3 instanceof Set) {
								Set assocSet = (Set) o3;
								assocSet.remove(hili);
							}
							/*
							 * direct references (parent/one-one) are not
							 * removed, throw a referential integrity exception
							 * instead i.e. these *must* be handled explicity in
							 * code three years later, not sure why I opted for
							 * the above but I guess, if it's server layer, the
							 * programmer has more control and there may be a
							 * decent reason hmm - for alcinamemcache, we need
							 * auto-removal of parent/child relations if
							 * transforming from servlet layer. possibly the
							 * "no-auto" was answer.getsurveyresponse
							 * performance in cosa? ..ahah - permissions
							 * reasons. if we null user/group (and are not
							 * root), may cause unwanted permissions probs - so
							 * only do in removeParentAssociations(), which is
							 * effectively root-only
							 */
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void removeParentAssociations(HasIdAndLocalId hili) {
		try {
			PropertyDescriptor[] pds = Introspector
					.getBeanInfo(hili.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (HasIdAndLocalId.class
						.isAssignableFrom(pd.getPropertyType())) {
					HasIdAndLocalId hiliTarget = (HasIdAndLocalId) pd
							.getReadMethod().invoke(hili,
									CommonUtils.EMPTY_OBJECT_ARRAY);
					if (hiliTarget != null) {
						TransformManager.get().registerDomainObject(hiliTarget);
					}
					pd.getWriteMethod().invoke(hili, new Object[] { null });
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
