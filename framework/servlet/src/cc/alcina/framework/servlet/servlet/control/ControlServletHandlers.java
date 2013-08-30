package cc.alcina.framework.servlet.servlet.control;

import java.util.List;

import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvents;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.gwt.client.gwittir.renderer.ToLowerCaseConverter;

public class ControlServletHandlers {
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

	public abstract static class ModeDeltaHandler<T> {
		protected AppLifecycleManager appLifecycleManager;

		public ModeDeltaHandler(AppLifecycleManager appLifecycleManager) {
			this.appLifecycleManager = appLifecycleManager;
		}

		public abstract String getModesPropertyName();

		public void handleDeltas(T[] fromStates, T[] toState) {
			for (T from : fromStates) {
				for (T to : toState) {
					if (from != to || checkNonDeltas()) {
						handleDelta(from, to);
					}
				}
			}
		}

		protected boolean checkNonDeltas() {
			return false;
		}

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
						new ToLowerCaseConverter().convert(currentValue),
						new ToLowerCaseConverter().convert(targetValue));
				appLifecycleManager.log(message);
				handleDelta0(fromState, toState);
				SEUtilities.setPropertyValue(appLifecycleManager.getState()
						.getModes(), getModesPropertyName(), toState);
			}
		}

		protected abstract void handleDelta0(T fromState, T toState);
	}

	public static class ModeDeltaHandlerWriterMode extends
			ModeDeltaHandler<WriterMode> {
		public ModeDeltaHandlerWriterMode(
				AppLifecycleManager appLifecycleManager) {
			super(appLifecycleManager);
		}

		@Override
		public void handleDelta0(WriterMode fromState, WriterMode toState) {
			if(toState==WriterMode.CLUSTER_WRITER){
				Registry.impl(DomainTransformPersistenceEvents.class).startSequentialEventChecks();
			}
			AppPersistenceBase
					.setInstanceReadOnly(toState == WriterMode.READ_ONLY);
		}

		@Override
		// check at init - probably fixme with some type of null->active check
		protected boolean checkNonDeltas() {
			return true;
		}

		@Override
		public String getModesPropertyName() {
			return "writerMode";
		}
	}

	public static class ModeDeltaHandlerRelayMode extends
			ModeDeltaHandler<WriterRelayMode> {
		public ModeDeltaHandlerRelayMode(AppLifecycleManager appLifecycleManager) {
			super(appLifecycleManager);
		}

		@Override
		public void handleDelta0(WriterRelayMode fromState,
				WriterRelayMode toState) {
		}

		@Override
		public String getModesPropertyName() {
			return "writerRelayMode";
		}
	}

	public static class ModeDeltaHandlerServiceMode extends
			ModeDeltaHandler<WriterServiceMode> {
		public ModeDeltaHandlerServiceMode(
				AppLifecycleManager appLifecycleManager) {
			super(appLifecycleManager);
		}

		@Override
		public void handleDelta0(WriterServiceMode fromState,
				WriterServiceMode toState) {
			List<WriterService> services = Registry.singletons(
					WriterService.class, Void.class);
			TaggedLogger logger = Registry.impl(TaggedLoggers.class).getLogger(
					ControlServlet.class);
			for (WriterService service : services) {
				logger.log(String
						.format("%s -> %s\n",
								service.getClass().getSimpleName(),
								toState == WriterServiceMode.NOT_CONTROLLER ? "shutdown"
										: "startup"));
				if (toState == WriterServiceMode.NOT_CONTROLLER) {
					try {
						service.shutdown();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						service.startup();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				logger.log(String
						.format("%s -> %s [Complete]\n",
								service.getClass().getSimpleName(),
								toState == WriterServiceMode.NOT_CONTROLLER ? "shutdown"
										: "startup"));
			}
		}

		@Override
		public String getModesPropertyName() {
			return "writerServiceMode";
		}
	}
}
