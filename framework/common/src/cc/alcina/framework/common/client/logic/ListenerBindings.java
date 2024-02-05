package cc.alcina.framework.common.client.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.ListenerReference;

public class ListenerBindings implements ListenerBinding {
	private List<ListenerBinding> listenerBindings = new ArrayList<>();

	boolean bound = false;

	public void add(ListenerBinding listenerBinding) {
		listenerBindings.add(listenerBinding);
		if (bound) {
			listenerBinding.bind();
		}
	}

	public void add(Supplier<ListenerReference> listenerReferenceSupplier) {
		add(ListenerReference.asBinding(listenerReferenceSupplier));
	}

	@Override
	public void bind() {
		Preconditions.checkState(!bound);
		listenerBindings.forEach(ListenerBinding::bind);
		bound = true;
	}

	@Override
	public void unbind() {
		Preconditions.checkState(bound);
		listenerBindings.forEach(ListenerBinding::unbind);
		bound = false;
	}
}