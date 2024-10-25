package cc.alcina.framework.common.client.process;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.common.client.util.LooseContextInstance.Frame;
import cc.alcina.framework.common.client.util.Multimap;

/*
 * Higher-cost than process observers, since the caller (if passing control)
 * will generally maintain a mutable state that allows ancestor modification.
 *
 * Used for context-based control, as well as observation of a process. See e.g.
 * TreeSync.Preparer
 *
 * Thread-safe - due to the context
 * 
 * ContextObservables will also be emitted as general ProcessObservables (for
 * testing, development essentially - such as attaching an appdebug observer to
 * a particular context observable)
 */
public class ContextObservers {
	static ContextObservers get() {
		return has() ? LooseContext.get(key()) : new ContextObservers();
	}

	public static boolean has() {
		return LooseContext.has(key());
	}

	static String key() {
		return ContextObservers.class.getName();
	}

	public interface Observable extends ProcessObservable {
		default void publish() {
			ProcessObservers.context().publish(this);
		}
	}

	public interface Observer<T extends ProcessObservable>
			extends ProcessObserver<T> {
		@Override
		default void bind() {
			ProcessObservers.context().observe(this);
		}
	}

	private ProcessObservers instance = new ProcessObservers();

	private Multimap<LooseContextInstance.Frame, List<ProcessObserver>> observers = new Multimap<>();

	private ContextObservers() {
	}

	// this is a relatively complex use of context, since listeners can be
	// added to the (one) ContextObservers at various depths. So validate on
	// publish.
	public void observe(ProcessObserver o) {
		// only ensure a ContextObservers exists in the context here
		if (!has()) {
			LooseContext.set(key(), this);
		}
		instance.observe0(o.getObservableClass(), o, true);
		LooseContextInstance.Frame frame = LooseContext.getContext().getFrame();
		observers.add(frame, o);
	}

	public <O extends ProcessObservable> void publish(O observable) {
		if (observers.size() > 0) {
			// validate observer frames
			Iterator<Entry<Frame, List<ProcessObserver>>> itr = observers
					.entrySet().iterator();
			while (itr.hasNext()) {
				Entry<Frame, List<ProcessObserver>> entry = itr.next();
				LooseContextInstance.Frame frame = entry.getKey();
				if (!frame.isActive()) {
					entry.getValue().forEach(o -> instance
							.observe0(o.getObservableClass(), o, false));
					itr.remove();
				}
			}
		}
		instance.publish0((Class<O>) observable.getClass(), () -> observable);
		/*
		 * Also publish to the global process observer system. As per the class
		 * doc, only observe contextobservables at the process level in
		 * development
		 */
		ProcessObservers.publish((Class) observable.getClass(),
				() -> observable);
	}
}