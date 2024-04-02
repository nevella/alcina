package cc.alcina.framework.gwt.client.dirndl.cmp.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusElement.CloseElement;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusElement.ElementClicked;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule.Message;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import rocket.selection.client.Selection;

@Directed(className = "dl-status")
public class StatusModel extends Model
		implements ElementClicked.Handler, StatusElement.CloseElement.Handler {
	static long delayMillis = 2000;

	static long removingMillis = 300;

	private List<StatusElement> elements = new ArrayList<>();

	private Timer timer;

	public StatusModel() {
	}

	@Directed
	public List<StatusElement> getElements() {
		return this.elements;
	}

	@Override
	public void onCloseElement(CloseElement event) {
		StatusElement model = event.getModel();
		close(model);
	}

	@Override
	public void onElementClicked(ElementClicked event) {
		StatusElement model = event.getModel();
		Selection selection = Selection.getSelection();
		if (!selection.isEmpty()) {
			model.delay = 999999;
		} else {
			if (model.isCloseOnClick()) {
				close(model);
			}
		}
		update();
	}

	public void setElements(List<StatusElement> elements) {
		set("elements", this.elements, elements,
				() -> this.elements = elements);
	}

	private void close(StatusElement model) {
		long removeTime = System.currentTimeMillis()
				+ (model.isRemoving() ? 0 : removingMillis);
		model.setDelay(removeTime - model.time);
		model.setRemoving(true);
	}

	private void ensureTimer() {
		if (timer == null) {
			long next = elements.stream().map(StatusElement::getNext)
					.collect(Collectors.minBy(Comparator.naturalOrder())).get();
			timer = Timer.Provider.get().getTimer(this::update);
			timer.schedule(next);
		}
	}

	void addMessage(Message message) {
		if (message.isNotBlank()) {
			Ax.out(message);
			StatusElement element = new StatusElement();
			element.setText(message.string);
			element.setModel(message.model);
			element.time = System.currentTimeMillis();
			element.delay = message.priority.getDelay();
			if (message.model != null) {
				element.delay = 999999;
			}
			List<StatusElement> update = elements.stream()
					.collect(Collectors.toList());
			update.add(element);
			setElements(update);
			ensureTimer();
		}
	}

	void update() {
		if (timer != null) {
			timer.cancel();
		}
		timer = null;
		elements.forEach(StatusElement::maybeRemoving);
		List<StatusElement> update = elements.stream()
				.filter(StatusElement::notExpired).collect(Collectors.toList());
		setElements(update);
		if (!elements.isEmpty()) {
			ensureTimer();
		}
	}

	public enum Priority {
		INFO, MAJOR, EXCEPTION;

		long getDelay() {
			switch (this) {
			case INFO:
				return 2000;
			case EXCEPTION:
			case MAJOR:
				return 6000;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}
}
