package cc.alcina.framework.common.client.logic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.TopicListener;

public class RemovablePropertyChangeListener implements PropertyChangeListener {
	private SourcesPropertyChangeEvents bound;

	protected String propertyName;

	protected Consumer<PropertyChangeEvent> handler;

	public RemovablePropertyChangeListener(SourcesPropertyChangeEvents bound,
			Object propertyName) {
		this(bound, propertyName, null);
	}

	public RemovablePropertyChangeListener(SourcesPropertyChangeEvents bound,
			Object propertyName, Consumer<PropertyChangeEvent> handler) {
		this.bound = bound;
		this.propertyName = PropertyEnum.asPropertyName(propertyName);
		this.handler = handler;
		bind();
	}

	public void bind() {
		if (propertyName == null) {
			bound.addPropertyChangeListener(this);
		} else {
			bound.addPropertyChangeListener(propertyName, this);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// either handler is non-null or this is over-ridden
		Preconditions.checkNotNull(handler);
		handler.accept(evt);
	}

	public void unbind() {
		if (bound != null) {
			if (propertyName == null) {
				bound.removePropertyChangeListener(this);
			} else {
				bound.removePropertyChangeListener(propertyName, this);
			}
		}
		bound = null;
	}

	public static class Typed<T> extends RemovablePropertyChangeListener
			implements ListenerReference {
		private TopicListener<T> typedHandler;

		public Typed(SourcesPropertyChangeEvents bound, Object propertyName,
				TopicListener<T> typedHandler) {
			super(bound, propertyName);
			this.typedHandler = typedHandler;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			// either typedHandler is non-null or this is over-ridden
			Preconditions.checkNotNull(typedHandler);
			typedHandler.topicPublished((T) evt.getNewValue());
		}

		@Override
		public void remove() {
			unbind();
		}

		@Override
		public void removeOnFire() {
			throw new UnsupportedOperationException();
		}
	}
}