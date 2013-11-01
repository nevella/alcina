package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.entityaccess.TransformPersister;
/**
 * see http://code.google.com/p/alcina/issues/detail?id=14
 * for proposed improvements
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint=TransformPersistenceQueue.class,implementationType=ImplementationType.SINGLETON)
public class NaiveTransformPersistenceQueue implements
		TransformPersistenceQueue {

	public DomainTransformLayerWrapper submit(
			TransformPersistenceToken persistenceToken)  {
		return new TransformPersister().transformExPersistenceContext(persistenceToken);
	}
}
