package cc.alcina.framework.entity.gwt.headless;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.i18n.client.BidiPolicy.BidiPolicyImpl;
import com.google.gwt.i18n.client.impl.CldrImpl;
import com.google.gwt.i18n.client.impl.LocaleInfoImpl;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.History.HistoryTokenEncoder;
import com.google.gwt.user.client.HistoryImpl;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.LocationImpl;
import com.google.gwt.user.client.impl.WindowImpl;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

public class GWTBridgeHeadless extends GWTBridge {
	private Set<Class> notImplemented = new LinkedHashSet<>();

	@Override
	public <T> T create(Class<?> classLiteral) {
		Optional<?> optional = Registry.optional(classLiteral);
		if (!optional.isPresent() && notImplemented.add(classLiteral)) {
			Ax.err("No GWT headless implementation: %s", classLiteral);
		}
		return (T) optional.get();
	}

	@Override
	public String getVersion() {
		return null;
	}

	@Override
	public boolean isClient() {
		return false;
	}

	@Override
	public void log(String message, Throwable e) {
		if (message != null) {
			System.out.println(message);
		}
		if (e != null) {
			e.printStackTrace();
		}
	}

	@Registration(BidiPolicyImpl.class)
	public static class BidiPolicyImplHeadless extends BidiPolicyImpl {
		public BidiPolicyImplHeadless() {
			super();
		}
	}

	@Registration(CldrImpl.class)
	public static class CldrImplHeadless extends CldrImpl {
		public CldrImplHeadless() {
			super();
		}
	}

	@Registration(FocusImpl.class)
	public static class FocusImplHeadless extends FocusImpl {
		public FocusImplHeadless() {
			super();
		}
	}

	@Registration(HistoryImpl.class)
	public static class HistoryImplHeadless extends HistoryImpl
			implements TopicListener<String> {
		@Override
		public void attachListener() {
			Window.Location.topicHashChanged().add(this);
		}

		@Override
		public void attachListenerIe8() {
			// NOOP (this modifies the browser listener)
		}

		@Override
		public void attachListenerStd() {
			// NOOP (this modifies the browser listener)
		}

		@Override
		public String decodeFragment(String encodedFragment) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String encodeFragment(String fragment) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String encodeHistoryTokenWithHash(String targetHistoryToken) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void fireHistoryChangedImpl(String token) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getToken() {
			return Window.Location.getHash();
		}

		@Override
		public boolean init() {
			return false;
		}

		@Override
		public void nativeUpdate(String historyToken) {
			newToken(historyToken);
		}

		@Override
		public void newToken(String historyToken) {
			Window.Location.setHash(historyToken);
		}

		@Override
		public void replaceToken(String historyToken) {
			Window.Location.setHash(historyToken);
		}

		@Override
		public void setToken(String token) {
			Window.Location.setHash(token);
		}

		@Override
		public void topicPublished(String message) {
			History.fireCurrentHistoryState();
		}
	}

	/**
	 * This may not be correct - double encoding is always a bit tricky to
	 * reason about. But given any server-side calls are going to be coupled
	 * with client-side calls (with non-trivial implementations), I feel this is
	 * OK
	 * 
	 * 
	 *
	 */
	@Registration(HistoryTokenEncoder.class)
	public static class HistoryTokenEncoderHeadless
			extends HistoryTokenEncoder {
		@Override
		public String decode(String toDecode) {
			return toDecode;
		}

		@Override
		public String encode(String toEncode) {
			return toEncode;
		}
	}

	@Registration(LocaleInfoImpl.class)
	public static class LocaleInfoImplHeadless extends LocaleInfoImpl {
		public LocaleInfoImplHeadless() {
			super();
		}
	}

	@Registration(LocationImpl.class)
	public static class LocationImplHeadless extends LocationImpl {
		private String hash;

		private String queryString;

		private String port;

		private String path;

		private String host;

		private String protocol;

		@Override
		public void assign(String newURL) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getHash() {
			return this.hash;
		}

		@Override
		public String getHost() {
			return this.host;
		}

		@Override
		public String getHostName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getHref() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getOrigin() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getPath() {
			return this.path;
		}

		@Override
		public String getPort() {
			return this.port;
		}

		@Override
		public String getProtocol() {
			return this.protocol;
		}

		@Override
		public String getQueryString() {
			return this.queryString;
		}

		@Override
		public void init(String protocol, String host, String port, String path,
				String queryString) {
			this.protocol = protocol;
			this.host = host;
			this.port = port;
			this.path = path;
			this.queryString = queryString;
		}

		@Override
		public void reload() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void replace(String newURL) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setHash(String hash) {
			String old_hash = this.hash;
			this.hash = hash;
			if (!Objects.equals(old_hash, hash) && old_hash != null) {
				// old_hash == null -> startup, do not publish
				Window.Location.topicHashChanged().publish(hash);
			}
		}

		public void setHost(String host) {
			this.host = host;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public void setPort(String port) {
			this.port = port;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public void setQueryString(String queryString) {
			this.queryString = queryString;
		}
	}

	@Registration(TextBoxImpl.class)
	public static class TextBoxImplHeadless extends TextBoxImpl {
		public TextBoxImplHeadless() {
			super();
		}
	}

	@Registration(WindowImpl.class)
	public static class WindowImplHeadless extends WindowImpl {
		@Override
		public String getHash() {
			return Window.Location.getHash();
		}

		@Override
		public String getQueryString() {
			return Window.Location.getQueryString();
		}

		@Override
		public void initWindowCloseHandler() {
			// FIXME - remocom
		}

		@Override
		public void initWindowResizeHandler() {
			// FIXME - remocom
		}

		@Override
		public void initWindowScrollHandler() {
			// FIXME - remocom
		}
	}

	/*
	 * Default (client) implementation
	 */
	@Registration.Singleton(
		value = CommitToStorageTransformListener.WithFlushedTransforms.class,
		priority = Priority.PREFERRED_LIBRARY)
	public static class WithFlushedTransformsImpl
			extends CommitToStorageTransformListener.WithFlushedTransforms {
		public boolean requireNoTransforms;

		@Override
		public void call(Runnable runnable) {
			// currently r/o
			if (requireNoTransforms) {
				// r/o - otherwise assume the server-hosted client can use
				// transforms, but never commit
				Preconditions.checkState(
						TransformManager.get().getTransforms().isEmpty());
			}
			runnable.run();
		}
	}
}