package cc.alcina.framework.servlet.cluster.transform;

import java.util.Collections;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.logic.EntityLayerObjects;

public class TransformCommitLog {
	private Producer<Void, byte[]> producer;

	private AtomicBoolean initialised = new AtomicBoolean();

	private AtomicBoolean stopped = new AtomicBoolean(false);

	private Timer timeoutChecker;

	private String hostName;

	private Consumer<ClusterTransformRequest> payloadConsumer;

	private AtomicInteger consumerThreadCounter = new AtomicInteger(0);

	private TransformCommitLogThread currentConsumerThread;

	private long pollTimeout;

	Logger logger = LoggerFactory.getLogger(getClass());

	private TopicPartition topicPartition;

	private long hostUid;

	private TransformCommitLogHost commitLogHost;

	public TransformCommitLog() {
	}

	public void consumer(TransformCommitLogHost commitLogHost,
			Consumer<ClusterTransformRequest> payloadConsumer, String hostName,
			long hostUid) {
		this.commitLogHost = commitLogHost;
		this.payloadConsumer = payloadConsumer;
		this.hostName = hostName;
		this.hostUid = hostUid;
		this.pollTimeout = commitLogHost.getPollTimeout();
		if (!initialised.getAndSet(true)) {
			topicPartition = new TopicPartition(getTopic(), 0);
			launchConsumerThread(-1);
			timeoutChecker = new Timer(true);
			timeoutChecker.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					checkPollTimeout();
				}
			}, pollTimeout, pollTimeout);
		}
	}

	public long getCommitPollReturned() {
		return currentConsumerThread == null ? -1
				: currentConsumerThread.pollEndTime;
	}

	public long getCommitPosition() {
		return currentConsumerThread == null ? -1
				: currentConsumerThread.currentOffset;
	}

	public Producer<Void, byte[]> producer() {
		if (producer == null) {
			Properties props = commitLogHost
					.createProducerProperties(getClass());
			props.put("key.serializer",
					"org.apache.kafka.common.serialization.ByteArraySerializer");
			props.put("value.serializer",
					"org.apache.kafka.common.serialization.ByteArraySerializer");
			producer = new KafkaProducer<>(props);
		}
		return producer;
	}

	/**
	 * Will throw if currentconsumer thread cancelled etc (anything that needs
	 * to ensure position should generally throw that exception and retry later)
	 */
	public void refreshCurrentPosition() {
		currentConsumerThread.checkCurrentPosition();
	}

	public synchronized Future<RecordMetadata> sendTransformPublishedMessage(
			DomainTransformRequestPersistent request) {
		return producer().send(new ProducerRecord<Void, byte[]>(getTopic(),
				null, serialize(request)));
	}

	public void shutdown() {
		stopped.set(true);
		if (producer != null) {
			producer.close();
		}
		if (timeoutChecker != null) {
			timeoutChecker.cancel();
		}
	}

	private void launchConsumerThread(long offset) {
		ClassLoader classLoader = EntityLayerObjects.get()
				.getServletLayerClassLoader();
		String tName = Ax.format("kafka-consumer-%s-%s",
				getClass().getSimpleName(),
				consumerThreadCounter.incrementAndGet());
		currentConsumerThread = new TransformCommitLogThread(classLoader, tName,
				offset);
		currentConsumerThread.start();
	}

	private byte[] serialize(DomainTransformRequestPersistent request) {
		return Registry.impl(ClusterTransformSerializer.class)
				.serialize(request);
	}

	protected synchronized void checkPollTimeout() {
		if (currentConsumerThread != null
				&& !currentConsumerThread.cancelled.get()) {
			long pollStartTime = currentConsumerThread.pollStartTime;
			long pollWait = System.currentTimeMillis() - pollStartTime;
			int multiplier = currentConsumerThread.connected() ? 1 : 4;
			if (pollStartTime != 0 && pollWait > pollTimeout * multiplier) {
				// can be caused by big gc pauses - but better safe than
				// sorry...
				long seek = currentConsumerThread.currentOffset + 1;
				logger.warn(
						"Restarting {} consumer - poll timeout: {} - max {} - ordinal {} - seekTo {}",
						getClass().getSimpleName(), pollWait, pollTimeout,
						consumerThreadCounter.get(), seek);
				currentConsumerThread.cancelled.set(true);
				launchConsumerThread(seek);
			}
		}
	}

	protected String getTopic() {
		return commitLogHost.getTopic();
	}

	protected void logAcceptRecord(ClusterTransformRequest value) {
	}

	private final class TransformCommitLogThread extends Thread {
		private final ClassLoader cl;

		private final String tName;

		long pollStartTime;

		long pollEndTime;

		private long pollTimeout;

		private AtomicBoolean cancelled = new AtomicBoolean(false);

		private long currentOffset = -1;

		private long seekToOffset;

		private KafkaConsumer<Void, byte[]> consumer;

		private CountDownLatch checkCurrentPositionLatch;

		private TransformCommitLogThread(ClassLoader cl, String tName,
				long seekToOffset) {
			this.cl = cl;
			this.tName = tName;
			this.seekToOffset = seekToOffset;
		}

		/*
		 * Used to ensure this topic is current
		 */
		public void checkCurrentPosition() {
			checkCurrentPositionLatch = new CountDownLatch(1);
			consumer.wakeup();
			try {
				checkCurrentPositionLatch.await();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public boolean connected() {
			return currentOffset != -1;
		}

		@Override
		public void run() {
			Thread.currentThread().setContextClassLoader(this.cl);
			Thread.currentThread().setName(this.tName);
			this.pollTimeout = TransformCommitLog.this.pollTimeout / 5;
			while (!stopped.get() && !cancelled.get()) {
				try {
					if (consumer == null) {
						Properties props = commitLogHost
								.createConsumerProperties(Ax.format(
										"%s-%s-%s-%s", getTopic(), hostName,
										hostUid, consumerThreadCounter.get()));
						props.put("key.deserializer",
								"org.apache.kafka.common.serialization.ByteArrayDeserializer");
						props.put("value.deserializer",
								"org.apache.kafka.common.serialization.ByteArrayDeserializer");
						// defense against a really mean gc
						// actually - no - if it fails, let it rejoin - just use
						// default
						// props.put("max.poll.interval.ms", "180000");
						consumer = new KafkaConsumer<>(props);
						consumer.assign(
								Collections.singletonList(topicPartition));
					}
					if (seekToOffset != -1) {
						consumer.seek(topicPartition, seekToOffset + 1);
						seekToOffset = -1;
					}
					if (checkCurrentPositionLatch != null) {
						long position = consumer.position(topicPartition);
						if (position == currentOffset + 1) {
							checkCurrentPositionLatch.countDown();
							checkCurrentPositionLatch = null;
						}
					}
					pollStartTime = System.currentTimeMillis();
					ConsumerRecords<Void, byte[]> records = consumer
							.poll(pollTimeout);
					pollStartTime = 0;
					pollEndTime = System.currentTimeMillis();
					if (stopped.get() && !cancelled.get()) {
						return;// don't commitsync
					}
					for (ConsumerRecord<Void, byte[]> record : records) {
						try {
							ClusterTransformRequest request = Registry
									.impl(ClusterTransformSerializer.class)
									.deserialize(record.value());
							logAcceptRecord(request);
							payloadConsumer.accept(request);
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							currentOffset = record.offset();
						}
					}
					if (checkCurrentPositionLatch != null) {
						checkCurrentPositionLatch.countDown();
						checkCurrentPositionLatch = null;
					}
					// poll's not the correct term here - but effect is the same
					// - possible blocking on cluster rebalance, so need to
					// bracket in a watcher
					pollStartTime = System.currentTimeMillis();
					consumer.commitSync();
					pollStartTime = 0;
					pollEndTime = System.currentTimeMillis();
				} catch (Throwable e) {
					if (e instanceof WakeupException) {
					} else {
						String message = "BKC transform consumer issue";
						e.printStackTrace();
						Ax.out(message);
						if (!Ax.isTest()) {
							EntityLayerLogging.persistentLog(message,
									"KAFKA_EXCEPTION");
						}
					}
					// restart consumer
					try {
						if (consumer != null) {
							consumer.close();
						}
					} catch (Throwable e1) {
						e1.printStackTrace();
					}
					if (e instanceof WakeupException) {
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					consumer = null;
				}
			}
			logger.warn("Exiting consumer thread {} -  {}",
					getClass().getSimpleName(), getName());
		}
	}
}