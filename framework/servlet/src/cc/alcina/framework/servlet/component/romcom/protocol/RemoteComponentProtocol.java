package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.DomEventData;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Pathref;
import com.google.gwt.dom.client.mutations.LocationMutation;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

public class RemoteComponentProtocol {
	@Bean
	public static class InvalidAuthenticationException extends Exception
			implements ProtocolException {
		@AlcinaTransient
		@Override
		public StackTraceElement[] getStackTrace() {
			return super.getStackTrace();
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class InvalidClientException extends Exception
			implements ProtocolException {
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

		@AlcinaTransient
		@Override
		public StackTraceElement[] getStackTrace() {
			return super.getStackTrace();
		}

		@Reflected
		public enum Action {
			REFRESH, EXPIRED
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
		 * Sent by the server to instruct the client to begin the await loop
		 */
		public static class BeginAwaitLoop extends Message {
		}

		/*
		 * Not an album by Beck.
		 */
		public static class DomEventMessage extends Message {
			public DomEventData data;
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

		/*
		 * A dirndl UI example
		 */
		@Bean(PropertySource.FIELDS)
		@Directed
		static class HelloBeans1x5 {
			@Directed
			String world = "World!";
		}

		public static class Invoke extends Message {
			public Pathref path;

			public String methodName;

			public List<?> arguments;

			public int id;

			public List<Class> argumentTypes;

			public String javascript;

			public JsResponseType jsResponseType;

			@Reflected
			public enum JsResponseType {
				_void, string, node_jso
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
		}

		/*
		 * An album by Beck. Amazing.
		 */
		public static class Mutations extends Message {
			public static Mutations ofLocation() {
				Mutations result = new Mutations();
				result.locationMutation = LocationMutation.ofWindow(false);
				return result;
			}

			public List<MutationRecord> domMutations = new ArrayList<>();

			public List<EventSystemMutation> eventMutations = new ArrayList<>();

			public LocationMutation locationMutation;
		}

		/*
		 * Models an exception during message processing;
		 *
		 * Ignores reflective checks because serialization the field
		 * protocolException would normally fail in the general case (here we
		 * guarantee imperatively that the field instance is a subtype of
		 * ProtocolException, anfd thus will succeed)
		 */
		@ReflectiveSerializer.Checks(ignore = true)
		public static class ProcessingException extends Message {
			public Exception protocolException;

			public String exceptionClassName;

			public String exceptionMessage;

			public Class<? extends Exception> exceptionClass() {
				try {
					return Reflections.forName(exceptionClassName);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		}

		/*
		 * Sent by the client on startup, to initialise the server dom
		 */
		public static class Startup extends Message {
			public static Startup forClient() {
				Startup result = new Startup();
				result.maxCharsPerTextNode = LocalDom.maxCharsPerTextNode;
				result.domMutations = LocalDom.pathRefRepresentations()
						.domAsMutations();
				result.locationMutation = LocationMutation.ofWindow(true);
				return result;
			}

			public LocationMutation locationMutation;

			public List<MutationRecord> domMutations = new ArrayList<>();

			public int maxCharsPerTextNode;
		}
	}

	/**
	 * Marker, the exception is safe to send as the payload of a
	 * ProcessingException message
	 *
	 *
	 */
	public interface ProtocolException {
	}

	@Bean(PropertySource.FIELDS)
	public static class Session {
		public String id;

		public String auth;

		public String url;

		public String componentClassName;
	}
}
