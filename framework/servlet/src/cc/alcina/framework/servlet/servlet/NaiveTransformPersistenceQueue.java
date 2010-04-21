package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.entityaccess.TransformPersister;
import cc.alcina.framework.servlet.ServletLayerLocator;
/**
 * see http://code.google.com/p/alcina/issues/detail?id=14
 * for proposed improvements
 * @author nick@alcina.cc
 *
 */
public class NaiveTransformPersistenceQueue implements
		TransformPersistenceQueue {

	public DomainTransformLayerWrapper submit(
			TransformPersistenceToken persistenceToken)  {
		return new TransformPersister().transformExPersistenceContext(persistenceToken);
	}
}
