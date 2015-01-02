package cc.alcina.framework.entity.impl.cache;

import org.hibernate.criterion.Criterion;

public class NotHandledException extends Exception {
	public NotHandledException() {
	}

	public NotHandledException(Criterion criterion) {
		super(criterion.getClass().getSimpleName() + ":" + criterion.toString());
	}

	public NotHandledException(String message) {
		super(message);
	}
}
