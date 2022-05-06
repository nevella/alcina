package cc.alcina.framework.entity.impl.jboss;

import java.io.Serializable;

import org.hibernate.Transaction;
import org.hibernate.type.Type;

import cc.alcina.framework.entity.persistence.transform.TransformPersisterInPersistenceContext.ThreadData;

public class ExceptionDebugInterceptor extends org.hibernate.EmptyInterceptor {
	public ExceptionDebugInterceptor() {
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		ThreadData.get().afterTransactionCompletion();
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		ThreadData.get().onFlushDirty(entity, id, currentState, previousState,
				propertyNames, types);
		return false;
	}
}
