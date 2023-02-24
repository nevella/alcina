package cc.alcina.framework.common.client.logic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.TopicListener;

/**
 *
 * @author nick@alcina.cc
 *
 */
public class RemovablePropertyChangeListener
		implements PropertyChangeListener, ListenerReference, ListenerBinding {
	private SourcesPropertyChangeEvents source;

	protected String propertyName;

	protected Consumer<PropertyChangeEvent> handler;

	private boolean bound;

	public RemovablePropertyChangeListener(SourcesPropertyChangeEvents source,
			Object propertyName) {
		this(source, propertyName, null);
	}

	public RemovablePropertyChangeListener(SourcesPropertyChangeEvents source,
			Object propertyName, Consumer<PropertyChangeEvent> handler) {
		this.source = source;
		this.propertyName = PropertyEnum.asPropertyName(propertyName);
		this.handler = handler;
	}

	@Override
	public void bind() {
		Preconditions.checkState(!bound);
		bound = true;
		if (propertyName == null) {
			source.addPropertyChangeListener(this);
		} else {
			source.addPropertyChangeListener(propertyName, this);
		}
	}

	public RemovablePropertyChangeListener changeBoundState(boolean to) {
		Preconditions.checkArgument(bound != to);
		if (to) {
			bind();
		} else {
			unbind();
		}
		return this;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// either handler is non-null or this is over-ridden
		Preconditions.checkNotNull(handler);
		handler.accept(evt);
	}

	@Override
	public void remove() {
		unbind();
	}

	@Override
	public void removeOnFire() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unbind() {
		if (bound) {
			if (propertyName == null) {
				source.removePropertyChangeListener(this);
			} else {
				source.removePropertyChangeListener(propertyName, this);
			}
			bound = false;
			source = null;
		}
	}

	public static class Typed<T> extends RemovablePropertyChangeListener {
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
	}
}