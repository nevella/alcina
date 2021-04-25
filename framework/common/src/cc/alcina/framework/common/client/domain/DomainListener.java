package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface DomainListener<E extends Entity> {
	public abstract Class<? extends E> getListenedClass();

	public abstract void insert(E o);

	public boolean isEnabled();

	public abstract void remove(E o);

	public void setEnabled(boolean enabled);

	public static class Delegating<E extends Entity>
			implements DomainListener<E> {
		private DomainListener<E> delegate;

		private Class<E> clazz;

		public DomainListener<E> getDelegate() {
			return this.delegate;
		}

		public void setDelegate(DomainListener<E> delegate) {
			this.delegate = delegate;
		}

		public Class<? extends E> getListenedClass() {
			return clazz;
		}

		public void insert(E o) {
			if (this.delegate == null) {
				return;
			}
			this.delegate.insert(o);
		}

		public boolean isEnabled() {
			return true;
		}

		public void remove(E o) {
			if (this.delegate == null) {
				return;
			}
			this.delegate.remove(o);
		}

		public void setEnabled(boolean enabled) {
			// noop
		}

		public Delegating(Class<E> clazz) {
			this.clazz = clazz;
		}
	}
}
