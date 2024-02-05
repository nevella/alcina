package cc.alcina.framework.common.client.util;

import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.ListenerBinding;

public interface ListenerReference {
	static ListenerBinding
			asBinding(Supplier<ListenerReference> listenerReferenceSupplier) {
		return new ListenerBinding() {
			private ListenerReference reference;

			@Override
			public void bind() {
				reference = listenerReferenceSupplier.get();
				Preconditions.checkState(reference != null);
			}

			@Override
			public void unbind() {
				reference.remove();
				reference = null;
			}
		};
	}

	default ListenerBinding asBinding() {
		return asBinding(() -> this);
	}

	void remove();

	void removeOnFire();
}