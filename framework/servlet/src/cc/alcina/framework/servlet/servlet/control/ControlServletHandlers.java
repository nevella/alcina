package cc.alcina.framework.servlet.servlet.control;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.gwittir.renderer.ToLowerCaseTrimmedConverter;

public class ControlServletHandlers {
    final static Logger logger = LoggerFactory
            .getLogger(MethodHandles.lookup().lookupClass());

    public abstract static class ModeDeltaHandler<T> {
        protected AppLifecycleManager appLifecycleManager;

        public ModeDeltaHandler(AppLifecycleManager appLifecycleManager) {
            this.appLifecycleManager = appLifecycleManager;
        }

        public abstract String getModesPropertyName();

        public void handleDelta(T fromState, T toState) {
            Object currentValue = SEUtilities.getPropertyValue(
                    appLifecycleManager.getState().getModes(),
                    getModesPropertyName());
            Object targetValue = SEUtilities.getPropertyValue(
                    appLifecycleManager.getTargetModes(),
                    getModesPropertyName());
            if (fromState == currentValue && toState == targetValue) {
                String message = String.format(
                        "Writer mode delta [%s]: %s -> %s",
                        getModesPropertyName(),
                        new ToLowerCaseTrimmedConverter().convert(currentValue),
                        new ToLowerCaseTrimmedConverter().convert(targetValue));
                appLifecycleManager.debug(message);
                handleDelta0(fromState, toState);
                SEUtilities.setPropertyValue(
                        appLifecycleManager.getState().getModes(),
                        getModesPropertyName(), toState);
            }
        }

        public void handleDeltas(T[] fromStates, T[] toState) {
            for (T from : fromStates) {
                for (T to : toState) {
                    if (from != to) {
                        handleDelta(from, to);
                    }
                }
            }
        }

        public void init() {
        }

        protected abstract void handleDelta0(T fromState, T toState);
    }

    public static class ModeDeltaHandlerRelayMode
            extends ModeDeltaHandler<WriterRelayMode> {
        public ModeDeltaHandlerRelayMode(
                AppLifecycleManager appLifecycleManager) {
            super(appLifecycleManager);
        }

        @Override
        public String getModesPropertyName() {
            return "writerRelayMode";
        }

        @Override
        public void handleDelta0(WriterRelayMode fromState,
                WriterRelayMode toState) {
        }
    }

    public static class ModeDeltaHandlerServiceMode
            extends ModeDeltaHandler<WriterServiceMode> {
        public ModeDeltaHandlerServiceMode(
                AppLifecycleManager appLifecycleManager) {
            super(appLifecycleManager);
        }

        @Override
        public String getModesPropertyName() {
            return "writerServiceMode";
        }

        @Override
        public void handleDelta0(WriterServiceMode fromState,
                WriterServiceMode toState) {
            List<WriterService> services = Registry
                    .singletons(WriterService.class, Void.class);
            for (WriterService service : services) {
                logger.debug(String.format("%s -> %s\n",
                        service.getClass().getSimpleName(),
                        toState == WriterServiceMode.NOT_CONTROLLER ? "shutdown"
                                : "startup"));
                if (toState == WriterServiceMode.NOT_CONTROLLER) {
                    try {
                        service.onApplicationShutdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        service.onApplicationStartup();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                logger.debug(String.format("%s -> %s [Complete]\n",
                        service.getClass().getSimpleName(),
                        toState == WriterServiceMode.NOT_CONTROLLER ? "shutdown"
                                : "startup"));
            }
        }
    }

    public static class ModeDeltaHandlerWriterMode
            extends ModeDeltaHandler<WriterMode> {
        public ModeDeltaHandlerWriterMode(
                AppLifecycleManager appLifecycleManager) {
            super(appLifecycleManager);
        }

        @Override
        public String getModesPropertyName() {
            return "writerMode";
        }

        @Override
        public void handleDelta0(WriterMode fromState, WriterMode toState) {
            updateReadonly(toState);
        }

        @Override
        public void init() {
//            updateReadonly(WriterMode.READ_ONLY);
        }

        private void updateReadonly(WriterMode toState) {
        	//utter crud - set via properties - this needs to happen very early, and can't be changed during webapp lifetime
//            AppPersistenceBase
//                    .setInstanceReadOnly(toState == WriterMode.READ_ONLY);
        }
    }

    public enum ModeEnum {
        WRITER_MODE {
            @Override
            public ModeDeltaHandlerWriterMode getDeltaHandler(
                    AppLifecycleManager appLifecycleManager) {
                return new ModeDeltaHandlerWriterMode(appLifecycleManager);
            }
        },
        WRITER_RELAY_MODE {
            @Override
            public ModeDeltaHandlerRelayMode getDeltaHandler(
                    AppLifecycleManager appLifecycleManager) {
                return new ModeDeltaHandlerRelayMode(appLifecycleManager);
            }
        },
        WRITER_SERVICE_MODE {
            @Override
            public ModeDeltaHandlerServiceMode getDeltaHandler(
                    AppLifecycleManager appLifecycleManager) {
                return new ModeDeltaHandlerServiceMode(appLifecycleManager);
            }
        };
        public abstract ModeDeltaHandler getDeltaHandler(
                AppLifecycleManager appLifecycleManager);
    }
}
