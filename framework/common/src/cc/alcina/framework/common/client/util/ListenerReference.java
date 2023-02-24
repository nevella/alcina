package cc.alcina.framework.common.client.util;

import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.ListenerBinding;

public interface ListenerReference {
	static ListenerBinding
			asBinding(Supplier<ListenerReference> listenerReferenceSupplier) {
		return new ListenerBinding() {
			private ListenerReference reference;

			@Override
			public void bind() {
				reference = listenerReferenceSupplier.get();
			}

			@Override
			public void unbind() {
				reference.remove();
				reference = null;
			}
		};
	}

	void remove();

	void removeOnFire();
}