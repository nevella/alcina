package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;

import com.google.web.bindery.event.shared.HandlerRegistration;

public class DetachList {
	private List<Runnable> detachList = new ArrayList<>();

	public void add(Runnable runnable) {
		detachList.add(runnable);
	}

	public void addRegistration(HandlerRegistration handlerRegistration) {
		detachList.add(() -> handlerRegistration.removeHandler());
	}

	public void detach() {
		detachList.forEach(Runnable::run);
		detachList.clear();
	}
}