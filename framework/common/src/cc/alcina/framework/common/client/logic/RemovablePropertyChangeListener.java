package cc.alcina.framework.common.client.logic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

public class RemovablePropertyChangeListener implements PropertyChangeListener {
	private SourcesPropertyChangeEvents bound;

	private String propertyName;

	private Consumer<PropertyChangeEvent> handler;

	public RemovablePropertyChangeListener(SourcesPropertyChangeEvents bound,
			String propertyName) {
		this(bound, propertyName, null);
	}

	public RemovablePropertyChangeListener(SourcesPropertyChangeEvents bound,
			String propertyName, Consumer<PropertyChangeEvent> handler) {
		this.bound = bound;
		this.propertyName = propertyName;
		this.handler = handler;
		bind();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// either handler is non-null or this is over-ridden
		Preconditions.checkNotNull(handler);
		handler.accept(evt);
	}

	public void bind() {
		if (propertyName == null) {
			bound.addPropertyChangeListener(this);
		} else {
			bound.addPropertyChangeListener(propertyName, this);
		}
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
}