package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.Objects;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.google.web.bindery.event.shared.SimpleEventBus;

import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

/**
 * <p>
 * Supports both synchronous (same execution stack) and asynchronous (queued)
 * event dispatch
 *
 * <p>
 * TODO - document a little more extensively - as a rule of thumb, you want to
 * fire 'events' synchronously if they're actually commands ('do this') - and
 * event translation propagation (Model events transformed from DOM events)
 * falls in that category, since it's the same originating user action - so the
 * 'event' is really more a 'tell the appropriate handlers at various model
 * levels to fire in response to DOM event x, translated' rather than 'the user
 * did something other'.
 *
 * <p>
 * On the other hand, signals <em>generated</em> in response to a model event
 * ('x occurred in the application model' - e.g. 'the user's coffee order
 * changed') should be fired asynchronously since they're not a translation,
 * rather an effect.
 *
 * @author nick@alcina.cc
 *
 */
public class VariableDispatchEventBus extends SimpleEventBus {
	private Set<QueuedEvent> distinctQueue = AlcinaCollections.newHashSet();

	public QueuedEvent queued() {
		return new QueuedEvent();
	}

	public class QueuedEvent {
		boolean distinct;

		Topic topic;

		Object message;

		Runnable runnable;

		public void dispatch() {
			Preconditions.checkState(topic != null ^ runnable != null);
			if (distinct) {
				if (!distinctQueue.add(this)) {
					return;
				}
			}
			Scheduler.get().scheduleFinally(this::dispatchSync);
		}

		/**
		 * Sugar for a commonn emission pattern. Note that it is distinct()
		 */
		public void dispatchModelEvent(Node node,
				Class<? extends ModelEvent> clazz) {
			distinct().lambda(() -> NodeEvent.Context.fromNode(node)
					.dispatch(clazz, null)).dispatch();
		}

		public QueuedEvent distinct() {
			distinct = true;
			return this;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof QueuedEvent) {
				QueuedEvent other = (QueuedEvent) obj;
				if (runnable != null) {
					if (other.runnable != null) {
						return runnable.getClass() == other.runnable.getClass();
					} else {
						return false;
					}
				} else {
					return Objects.equals(topic, other.topic)
							&& Objects.equals(message, other.message);
				}
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			if (runnable != null) {
				return runnable.getClass().hashCode();
			} else {
				return Objects.hash(topic, message);
			}
		}

		public <T> QueuedEvent lambda(Runnable runnable) {
			this.runnable = runnable;
			return this;
		}

		public QueuedEvent signal(Topic<Void> topic) {
			this.topic = topic;
			this.message = null;
			return this;
		}

		public <T> QueuedEvent topic(Topic<T> topic, T message) {
			this.topic = topic;
			this.message = message;
			return this;
		}

		void dispatchSync() {
			if (distinct) {
				distinctQueue.remove(this);
			}
			if (runnable != null) {
				runnable.run();
			} else {
				topic.publish(message);
			}
		}
	}
}
