package cc.alcina.framework.servlet.environment;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.StyleInjector;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.entity.EncryptionUtils;
import cc.alcina.framework.gwt.client.provider.ResourceLoader;
import cc.alcina.framework.gwt.client.provider.ResourceLoader.Priority;
import cc.alcina.framework.gwt.client.provider.ResourceLoader.Type;
import cc.alcina.framework.servlet.component.romcom.Feature_Romcom_Impl;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.LoadResource;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ResourceLoaded;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ResourceRequired;
import cc.alcina.framework.servlet.component.romcom.protocol.StringProtocol.Cache;

@Feature.Ref(Feature_Romcom_Impl._ResourceLoader.class)
class ResourceLoaderRomcom implements ResourceLoader.RomcomImpl {
	Environment environment;

	Cache stringProtocolCache;

	Queue queue;

	static SharedResources sharedResources = new SharedResources();

	static class SharedResources {
		Map<String, String> contentHash = CollectionCreators.Bootstrap
				.createConcurrentStringMap();

		String getHash(String contents) {
			return contentHash.computeIfAbsent(contents,
					EncryptionUtils.get()::SHA1);
		}
	}

	ResourceLoaderRomcom(Environment environment) {
		this.environment = environment;
		stringProtocolCache = MessageTransportLayer.get()
				.getStringProtocolCache();
		queue = new Queue();
	}

	class Queue {
		class Entry {
			String cacheKey;

			String value;

			Type type;

			long timeAdded;

			boolean loadMessageSent;

			boolean resourceMessageSent;

			String valueHash;

			Entry(String cacheKey, String value, String valueHash, Type type) {
				this.cacheKey = cacheKey;
				this.value = value;
				this.valueHash = valueHash;
				this.type = type;
				timeAdded = System.currentTimeMillis();
			}

			ResourceRequired toResourceRequired() {
				ResourceRequired result = new ResourceRequired();
				result.cacheKey = cacheKey;
				result.valueHash = valueHash;
				return result;
			}

			Message.Resource toResourceMessage() {
				Message.Resource result = new Message.Resource();
				result.cacheKey = cacheKey;
				result.valueHash = valueHash;
				result.value = value;
				return result;
			}
		}

		Deque<Entry> entries = new LinkedList<>();

		void schedule(String cacheKey, String value, String valueHash,
				Type type) {
			entries.add(new Entry(cacheKey, value, valueHash, type));
			checkEntries();
		}

		/*
		 * this *could* gate based on client dom events (e.g. don't send until
		 * there's a 500ms pause), but first try without
		 */
		void checkEntries() {
			if (entries.isEmpty()) {
				return;
			}
			Entry first = entries.getFirst();
			if (!first.loadMessageSent) {
				ResourceRequired resourceRequired = first.toResourceRequired();
				first.loadMessageSent = true;
				/*
				 * this two-phase load sequence (ask the client to request the
				 * resource, dispatch async) means a large resource doesn't
				 * block the AwaitResponse pipe
				 */
				environment.access().dispatchToClient(resourceRequired);
			}
		}

		void onLoadResource(LoadResource message) {
			Entry first = entries.getFirst();
			Preconditions.checkState(first.loadMessageSent
					&& Objects.equals(first.cacheKey, message.cacheKey));
			Message.Resource resource = first.toResourceMessage();
			first.resourceMessageSent = true;
			environment.access().dispatchToClient(resource);
		}

		Entry getEntryPostLoad(ResourceLoaded message) {
			Entry first = entries.getFirst();
			Preconditions.checkState(first.resourceMessageSent
					&& Objects.equals(first.cacheKey, message.cacheKey));
			entries.removeFirst();
			return first;
		}
	}

	@Override
	public void injectStyle(String cacheKey, String contents,
			Priority priority) {
		String hash = sharedResources.getHash(contents);
		boolean inject = priority == Priority.immediate
				|| stringProtocolCache.clientContains(cacheKey, contents, hash);
		if (inject) {
			StyleInjector.inject(contents);
		} else {
			queue.schedule(cacheKey, contents, hash, ResourceLoader.Type.style);
		}
	}

	void onLoadResource(LoadResource message) {
		queue.onLoadResource(message);
	}

	void onResourceLoaded(ResourceLoaded message) {
		Queue.Entry loaded = queue.getEntryPostLoad(message);
		switch (loaded.type) {
		case style:
			stringProtocolCache.markClientContains(message.cacheKey,
					loaded.value, loaded.valueHash);
			StyleInjector.inject(loaded.value);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		queue.checkEntries();
	}
}
