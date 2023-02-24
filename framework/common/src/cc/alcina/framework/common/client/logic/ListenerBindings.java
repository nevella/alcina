package cc.alcina.framework.common.client.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.ListenerReference;

public class ListenerBindings implements ListenerBinding {
	private List<ListenerBinding> listenerBindings = new ArrayList<>();

	public void add(ListenerBinding listenerBinding) {
		listenerBindings.add(listenerBinding);
	}

	public void add(Supplier<ListenerReference> listenerReferenceSupplier) {
		add(ListenerReference.asBinding(listenerReferenceSupplier));
	}

	@Override
	public void bind() {
		listenerBindings.forEach(ListenerBinding::bind);
	}

	@Override
	public void unbind() {
		listenerBindings.forEach(ListenerBinding::unbind);
	}
}