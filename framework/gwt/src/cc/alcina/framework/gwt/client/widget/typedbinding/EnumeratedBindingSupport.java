package cc.alcina.framework.gwt.client.widget.typedbinding;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.util.Multimap;

public class EnumeratedBindingSupport {
    EnumeratedBinding[] bindings;

    private HasEnumeratedBindings source;

    Multimap<String, List<PropertyChangeListener>> listeners = new Multimap<>();

    Map<EnumeratedBinding, PropertyChangeListener> linkedListeners = new LinkedHashMap<>();

    public EnumeratedBindingSupport(HasEnumeratedBindings source,
            Class<? extends EnumeratedBinding> clazz) {
        this.source = source;
        bindings = clazz.getEnumConstants();
    }

    public void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        listeners.add(propertyName, listener);
        Optional<EnumeratedBinding> o_binding = bindingForPath(propertyName);
        if (o_binding.isPresent()) {
            EnumeratedBinding binding = o_binding.get();
            if (!linkedListeners.containsKey(binding)) {
                linkedListeners.put(binding, new LinkedListener(binding));
            }
            PropertyChangeListener pcl = linkedListeners.get(binding);
            SourcesPropertyChangeEvents related = (SourcesPropertyChangeEvents) source
                    .provideRelatedObject(binding.getBoundClass());
            related.addPropertyChangeListener(propertyName, pcl);
        }
    }

    public <T> T get(EnumeratedBinding enumeratedBinding) {
        Object related = source
                .provideRelatedObject(enumeratedBinding.getBoundClass());
        if (related == null) {
            return null;
        }
        return (T) Reflections.propertyAccessor().getPropertyValue(related,
                enumeratedBinding.getBoundPath());
    }

    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        listeners.subtract(propertyName, listener);
        Optional<EnumeratedBinding> o_binding = bindingForPath(propertyName);
        if (o_binding.isPresent()
                && listeners.getAndEnsure(propertyName).isEmpty()) {
            EnumeratedBinding binding = o_binding.get();
            if (linkedListeners.containsKey(binding)) {
                PropertyChangeListener linkedListener = linkedListeners
                        .get(binding);
                SourcesPropertyChangeEvents related = (SourcesPropertyChangeEvents) source
                        .provideRelatedObject(binding.getBoundClass());
                related.removePropertyChangeListener(propertyName,
                        linkedListener);
                linkedListeners.remove(binding);
            }
        }
    }

    public void set(EnumeratedBinding enumeratedBinding, Object value) {
        Object related = source
                .provideRelatedObject(enumeratedBinding.getBoundClass());
        if (related == null) {
            throw new IllegalStateException(
                    "binding should not be exposed for null object");
        }
        Reflections.propertyAccessor().setPropertyValue(related,
                enumeratedBinding.getBoundPath(), value);
    }

    private Optional<EnumeratedBinding> bindingForPath(String propertyName) {
        return Arrays.asList(bindings).stream()
                .filter(eb -> eb.getPath().equals(propertyName)).findFirst();
    }

    class LinkedListener implements PropertyChangeListener {
        private EnumeratedBinding enumeratedBinding;

        public LinkedListener(EnumeratedBinding enumeratedBinding) {
            this.enumeratedBinding = enumeratedBinding;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            PropertyChangeEvent chainedEvent = new PropertyChangeEvent(source,
                    enumeratedBinding.getPath(), evt.getOldValue(),
                    evt.getNewValue());
            for (PropertyChangeListener pcl : listeners
                    .getAndEnsure(enumeratedBinding.getPath())) {
                pcl.propertyChange(chainedEvent);
            }
        }
    }
}
