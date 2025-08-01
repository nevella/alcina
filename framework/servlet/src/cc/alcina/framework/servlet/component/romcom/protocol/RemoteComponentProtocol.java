package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gwt.dev.protobuf.Message;
import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.WindowState;
import com.google.gwt.dom.client.DomEventData;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.mutations.ElementSelectionRangeRecord;
import com.google.gwt.dom.client.mutations.LocationMutation;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.SelectionRecord;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.servlet.component.romcom.client.common.logic.RemoteComponentSettings;

public class RemoteComponentProtocol {
	@Bean
	public static class InvalidAuthenticationException
			extends ProtocolException {
	}

	@Bean
	public static class ServerProcessingException extends ProtocolException {
		public ServerProcessingException(String message) {
			super(message);
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class InvalidClientException extends ProtocolException {
		@Reflected
		public enum Action {
			REFRESH, EXPIRED
		}

		public Action action;

		public transient String uiType;

		public InvalidClientException() {
		}

		public InvalidClientException(String message, Action action,
				String uiType) {
			super(message);
			this.action = action;
			this.uiType = uiType;
		}
	}

	@Bean(PropertySource.FIELDS)
	public abstract static class Message {
		/*
		 * Sent by the client to allow the server to send it messages
		 */
		public static class AwaitRemote extends Message {
		}

		/*
		 * Sent by the server to instruct the client that server environment
		 * init has completed (so the client startup should begin the await
		 * loop)
		 */
		public static class EnvironmentInitComplete extends Message {
			@Bean(PropertySource.FIELDS)
			public static class EnvironmentSettings {
				public int longRunningMessageTimeMs = 2000;

				public boolean attachRpcDebugMethod = true;
			}

			public EnvironmentSettings environmentSettings = new EnvironmentSettings();
		}

		public interface Handler<M extends Message> {
			default boolean isHandleOutOfBand() {
				return false;
			}
		}

		/*
		 * Not an album by Beck.
		 */
		public static class DomEventMessage extends Message
				implements PrependWindowState {
			public List<DomEventData> events = new ArrayList<>();

			@Override
			protected String provideMessageData() {
				return events.stream().map(e -> e.event.getType()).distinct()
						.collect(Collectors.joining(", "));
			}
		}

		/*
		 * A dirndl UI example
		 */
		@Bean
		@Directed
		public static class HelloBeans1x0 {
			private String world = "World!";

			@Directed
			public String getWorld() {
				return this.world;
			}

			public void setWorld(String world) {
				this.world = world;
			}
		}

		public static class Invoke extends Message {
			@Reflected
			public enum JsResponseType {
				_void, string, node_jso
			}

			public AttachId path;

			public String targetName;

			public String methodName;

			public List<?> arguments;

			public List<?> flags;

			public int id;

			public List<Class> argumentTypes;

			public String javascript;

			public JsResponseType jsResponseType;

			@Override
			protected String provideMessageData() {
				if (Ax.notBlank(javascript)) {
					return "javascript:" + Ax.ntrim(javascript, 50);
				} else {
					return Ax.format("%s::%s", targetName, methodName);
				}
			}
		}

		public static class PersistSettings extends Message {
			public String value;
		}

		public static class ServerDebugProtocolRequest extends Message {
			public String clientState;
		}

		public static class ServerDebugProtocolResponse extends Message {
			public String serverState;
		}

		/**
		 * An odd one, this is processed server-side but is part of the
		 * to-client message sequence
		 */
		public static class SetCookieServerSide extends Message {
			public String name;

			public String value;

			public SetCookieServerSide() {
			}

			public SetCookieServerSide(String name, String value) {
				this.name = name;
				this.value = value;
			}
		}

		// FIXME - doc this annotation
		@ReflectiveSerializer.Checks(ignore = true)
		public static class InvokeResponse extends Message {
			public int id;

			public Object response;

			public ExceptionTransport exception;
		}

		public static class ExceptionTransport extends Bindable.Fields {
			public String className;

			public String nestedClassName;

			public String stackTrace;

			public ExceptionTransport() {
			}

			public ExceptionTransport(Throwable throwable) {
				stackTrace = CommonUtils.getFullExceptionMessage(throwable);
				className = throwable.getClass().getName();
				nestedClassName = NestedName.get(throwable);
			}

			@Override
			public String toString() {
				return Ax.format("%s :: %s", NestedName.get(this),
						nestedClassName);
			}

			public String toExceptionString() {
				return Ax.format("%s :: %s\n\n%s", NestedName.get(this),
						nestedClassName, stackTrace);
			}
		}

		/*
		 * An album by Beck. Amazing.
		 */
		public static class Mutations extends Message
				implements PrependWindowState {
			public static Mutations ofLocation() {
				Mutations result = new Mutations();
				result.locationMutation = LocationMutation.ofWindow(false);
				return result;
			}

			// TODO - romcom/ref.ser, serialized, there should be no classname
			// (but there is)
			public List<MutationRecord> domMutations = new ArrayList<>();

			public List<EventSystemMutation> eventSystemMutations = new ArrayList<>();

			public LocationMutation locationMutation;

			public SelectionRecord selectionMutation;

			@Override
			public String toDebugString() {
				return FormatBuilder.keyValues("dom", domMutations.size(),
						"event", eventSystemMutations.size(), "loc",
						locationMutation);
			}

			public void addDomMutation(MutationRecord mutationRecord) {
				domMutations.add(mutationRecord);
			}

			@Override
			protected String provideMessageData() {
				FormatBuilder format = new FormatBuilder().separator(" - ");
				if (domMutations.size() > 0) {
					format.append(domMutations.stream()
							.map(dm -> dm.target.nodeName).distinct()
							.collect(Collectors.joining(", ")));
				}
				if (domMutations.isEmpty() && eventSystemMutations.size() > 0) {
					format.append(eventSystemMutations.stream()
							.map(esm -> esm.eventTypeName).distinct()
							.collect(Collectors.joining(", ")));
				}
				return format.toString();
			}
		}

		/*
		 * Models an exception during message processing;
		 *
		 */
		@ReflectiveSerializer.Checks(ignore = true)
		public static class ProcessingException extends Message {
			public static ProcessingException wrap(Exception e,
					boolean includeFullTrace) {
				Message.ProcessingException processingException = new Message.ProcessingException();
				processingException.exceptionClassName = e.getClass().getName();
				processingException.exceptionMessage = CommonUtils
						.toSimpleExceptionMessage(e);
				processingException.exceptionTrace = CommonUtils
						.getFullExceptionMessage(e);
				if (e instanceof ProtocolException) {
					processingException.protocolException = (ProtocolException) e;
				}
				return processingException;
			}

			public ProtocolException protocolException;

			public String exceptionClassName;

			public String exceptionMessage;

			public String exceptionTrace;

			public Class<? extends ProtocolException> exceptionClass() {
				try {
					return Reflections.forName(exceptionClassName);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		}

		/**
		 * The client should send a WindowStateUpdate message prior to this
		 * message
		 */
		public interface PrependWindowState {
		}

		public static class WindowStateUpdate extends Message {
			public WindowState windowState;

			public SelectionRecord selectionRecord;

			public ElementSelectionRangeRecord elementSelectionRangeRecord;

			@Override
			protected String provideMessageData() {
				FormatBuilder format = new FormatBuilder().separator(" - ");
				format.format("[node offsets: %s]",
						windowState.nodeUiStates.size());
				if (selectionRecord != null) {
					format.format("[selection: %s]", selectionRecord);
				}
				return format.toString();
			}
		}

		/*
		 * Sent by the client on startup, to initialise the server dom
		 */
		public static class Startup extends Message {
			public static Startup forClient() {
				Startup result = new Startup();
				result.domMutations = LocalDom.attachIdRepresentations()
						.domAsMutations();
				result.locationMutation = LocationMutation.ofWindow(true);
				try {
					result.settings = RemoteComponentSettings.getSettings();
				} catch (Exception e) {
					result.settingsException = CommonUtils
							.getFullExceptionMessage(e);
				}
				return result;
			}

			public WindowState windowState;

			public String settingsException;

			public LocationMutation locationMutation;

			public List<MutationRecord> domMutations = new ArrayList<>();

			public String settings;
		}

		/*
		 * The request/response (server->client) is marked as synchronous, the
		 * server thread will not continue until the (possibly out-of-order)
		 * response is processed
		 */
		public boolean sync;

		/*
		 * Client -> server; incremental.
		 */
		public int messageId;

		public String toDebugString() {
			return toString();
		}

		@Override
		public String toString() {
			String messageData = provideMessageData();
			if (Ax.notBlank(messageData)) {
				messageData = Ax.format(" [%s]", messageData);
			}
			return Ax.format("%s :: %s%s", messageId,
					getClass().getSimpleName(), messageData);
		}

		public boolean canMerge(Message message) {
			return false;
		}

		public void merge(Message message) {
			throw new UnsupportedOperationException();
		}

		/*
		 * for toString() - e.g. for a DomEvent, the event type
		 */
		protected String provideMessageData() {
			return "";
		}
	}

	/**
	 * Marker, the exception is safe to send as the payload of a
	 * ProcessingException message
	 *
	 *
	 */
	@Bean(PropertySource.FIELDS)
	public static abstract class ProtocolException extends Exception {
		public ProtocolException() {
		}

		public ProtocolException(String message) {
			super(message);
		}

		@AlcinaTransient
		@Override
		public StackTraceElement[] getStackTrace() {
			return super.getStackTrace();
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class Session {
		public String id;

		public String auth;

		public String url;

		public String componentClassName;

		// ipv4 address
		@Property.Not
		public transient String remoteAddress;

		@Property.Not
		public transient long startTime;

		public boolean provideIsLocalHost() {
			return Objects.equals(remoteAddress, "127.0.0.1");
		}

		@Override
		public String toString() {
			return Ax.format("session :: id - %s", id);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Session) {
				Session o = (Session) obj;
				return Objects.equals(id, o.id);
			} else {
				return super.equals(obj);
			}
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}
}
