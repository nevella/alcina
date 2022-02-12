package cc.alcina.framework.servlet.cluster.transform;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.cluster.transform.ClusterTransformRequest.State;

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

	private long highestSeekOffset = -1;

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
			logger.info("{}Launch consumer thread :: {}", datestampIfTest(),
					hostName);
			launchConsumerThread(-1);
			logger.info("Started queue :: host {} :: offset {}", hostName,
					currentConsumerThread.currentOffset);
			timeoutChecker = new Timer(true);
			timeoutChecker.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					checkPollTimeout();
				}
			}, pollTimeout, pollTimeout);
			// make sure we have a valid offset before returning (handles
			// network outage/timeout on startup)
			/*
			 */
			MethodContext.instance().withMetricKey("tcl-initial-position")
					.run(() -> refreshCurrentPosition());
		}
	}

	public long getCommitPollReturned() {
		return currentConsumerThread == null ? -1
				: currentConsumerThread.operationEndTime;
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
		if (EntityLayerUtils.isTest()
				&& currentConsumerThread.currentOffset != -1) {
			return;
		}
		currentConsumerThread.checkCurrentPosition();
	}

	public synchronized List<Future<RecordMetadata>>
			sendTransformPublishedMessages(
					DomainTransformRequestPersistent request, State state) {
		return sendTransformPublishedMessages0(request, state);
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

	private String datestampIfTest() {
		return Ax.isTest() ? Ax.timestamp(new Date()) + " " : "";
	}

	private void launchConsumerThread(long offset) {
		ClassLoader classLoader = EntityLayerObjects.get()
				.getServletLayerClassLoader();
		String tName = Ax.format("kafka-consumer-%s-%s",
				getClass().getSimpleName(),
				consumerThreadCounter.incrementAndGet());
		CountDownLatch checkCurrentPositionLatch = null;
		if (currentConsumerThread != null) {
			checkCurrentPositionLatch = currentConsumerThread.checkCurrentPositionLatch;
			logger.info("Restarting consumer with existing position latch");
		}
		currentConsumerThread = new TransformCommitLogThread(classLoader, tName,
				offset);
		currentConsumerThread.checkCurrentPositionLatch = checkCurrentPositionLatch;
		currentConsumerThread.start();
	}

	private List<Future<RecordMetadata>> sendTransformPublishedMessages0(
			DomainTransformRequestPersistent request, State state) {
		if (producer() == null) {
			logger.warn("Mpt sending transform packets - no producer");
			return new ArrayList<>();
		}
		List<byte[]> serialized = serialize(request, state);
		return serialized.stream()
				.map(packet -> producer().send(new ProducerRecord<Void, byte[]>(
						getTopic(), null, packet)))
				.collect(Collectors.toList());
	}

	private List<byte[]> serialize(DomainTransformRequestPersistent request,
			State state) {
		return Registry.impl(ClusterTransformSerializer.class)
				.serialize(request, state);
	}

	protected synchronized void checkPollTimeout() {
		if (currentConsumerThread != null
				&& !currentConsumerThread.cancelled.get()) {
			long pollStartTime = currentConsumerThread.operationStartTime;
			long pollWait = System.currentTimeMillis() - pollStartTime;
			int multiplier = currentConsumerThread.connected() ? 1 : 4;
			if (pollStartTime != 0 && pollWait > pollTimeout * multiplier) {
				if (currentConsumerThread.currentOffset == -1) {
					logger.warn(
							"Restarting {} consumer - not started - ordinal {} ",
							getClass().getSimpleName(),
							consumerThreadCounter.get());
				}
				// can be caused by big gc pauses - but better safe than
				// sorry...
				long seek = currentConsumerThread.currentOffset;
				if (seek == -1) {
					seek = currentConsumerThread.previousConsumerCompletedOffset;
				}
				if (seek == -1) {
					seek = highestSeekOffset;
				}
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

		long operationStartTime;

		long operationEndTime;

		private long pollTimeout;

		private AtomicBoolean cancelled = new AtomicBoolean(false);

		private long currentOffset = -1;

		private long previousConsumerCompletedOffset;

		private KafkaConsumer<Void, byte[]> consumer;

		CountDownLatch checkCurrentPositionLatch;

		private TransformCommitLogThread(ClassLoader cl, String tName,
				long previousConsumerCompletedOffset) {
			this.cl = cl;
			this.tName = tName;
			this.previousConsumerCompletedOffset = previousConsumerCompletedOffset;
		}

		/*
		 * Used to ensure this topic is current
		 */
		public void checkCurrentPosition() {
			checkCurrentPositionLatch = new CountDownLatch(1);
			try {
				if (consumer != null && currentOffset != -1) {
					consumer.wakeup();
				}
				checkCurrentPositionLatch.await();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public boolean connected() {
			return currentOffset != -1;
		}

		@Override
		/*
		 * if operationStartTime !=0, an operation is underway, so this thread
		 * will be shut down if the operation doesn't return in the allocated
		 * time
		 */
		public void run() {
			Thread.currentThread().setContextClassLoader(this.cl);
			Thread.currentThread().setName(this.tName);
			this.pollTimeout = TransformCommitLog.this.pollTimeout / 5;
			while (!stopped.get() && !cancelled.get()) {
				try {
					if (consumer == null) {
						/*
						 * Creates a unique per-vm consumer
						 */
						String groupId = Ax.format("%s-%s-%s-%s", getTopic(),
								hostName, hostUid, consumerThreadCounter.get());
						Properties props = commitLogHost
								.createConsumerProperties(groupId);
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
						logger.info(
								"Started consumer :: thread {} :: groupId :: {} :: topicPartion :: {}",
								tName, groupId, topicPartition);
						if (!EntityLayerUtils.isTest()
								&& !EntityLayerUtils.isTestServer()) {
							/*
							 * We've just created a new consumer group, and
							 * kafka seems to hang more than it should at this
							 * point. So trying a sleep
							 */
							Thread.sleep(1000);
						}
					}
					if (previousConsumerCompletedOffset != -1) {
						performOperation(() -> consumer.seek(topicPartition,
								previousConsumerCompletedOffset + 1));
						previousConsumerCompletedOffset = -1;
					}
					if (checkCurrentPositionLatch != null) {
						logger.info("{}Check current position",
								datestampIfTest());
						if (currentOffset == -1) {
							performOperation(() -> {
								consumer.seekToEnd(Collections
										.singletonList(topicPartition));
							});
						}
						long position = performOperation(
								() -> consumer.position(topicPartition));
						logger.info(
								"{}Current position :: {} - current offset :: {}",
								datestampIfTest(), position, currentOffset);
						if (currentOffset == -1) {
							currentOffset = position - 1;
							highestSeekOffset = Math.max(highestSeekOffset,
									currentOffset);
						}
						if (position == currentOffset + 1) {
							checkCurrentPositionLatch.countDown();
							checkCurrentPositionLatch = null;
						}
					}
					ConsumerRecords<Void, byte[]> records = performOperation(
							() -> consumer
									.poll(Duration.ofMillis(pollTimeout)));
					if (stopped.get() && !cancelled.get()) {
						return;// don't commitsync
					}
					boolean hadRecords = false;
					for (ConsumerRecord<Void, byte[]> record : records) {
						try {
							ClusterTransformRequest request = Registry
									.impl(ClusterTransformSerializer.class)
									.deserialize(record.value(),
											TransformCommitLog.this.getClass());
							if (request == null) {
								logger.info("Received partial packet - {}",
										Registry.impl(
												ClusterTransformSerializer.class)
												.getLastPartialId());
							} else {
								logAcceptRecord(request);
								payloadConsumer.accept(request);
							}
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							hadRecords = true;
							currentOffset = record.offset();
							highestSeekOffset = Math.max(highestSeekOffset,
									currentOffset);
						}
					}
					if (checkCurrentPositionLatch != null) {
						checkCurrentPositionLatch.countDown();
						checkCurrentPositionLatch = null;
					}
					if (!hadRecords) {
						performOperation(() -> consumer.commitSync());
					}
				} catch (Throwable e) {
					if (CommonUtils.hasCauseOfClass(e, WakeupException.class)) {
					} else {
						String message = "BKC transform consumer issue";
						e.printStackTrace();
						Ax.out(message);
						if (!Ax.isTest()) {
							EntityLayerLogging.persistentLog(message,
									"KAFKA_EXCEPTION");
						}
					}
					if (e instanceof WakeupException) {
					} else {
						// restart consumer
						try {
							if (consumer != null) {
								consumer.close();
							}
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

		private <V> V performOperation(Callable<V> callable) {
			try {
				operationStartTime = System.currentTimeMillis();
				operationEndTime = 0;
				V result = callable.call();
				operationEndTime = System.currentTimeMillis();
				return result;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				operationStartTime = 0;
			}
		}

		private void performOperation(ThrowingRunnable runnable) {
			Callable<Object> callable = () -> {
				runnable.run();
				return null;
			};
			performOperation(callable);
		}
	}
}