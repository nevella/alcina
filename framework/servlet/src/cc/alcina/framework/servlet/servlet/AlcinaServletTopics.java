package cc.alcina.framework.servlet.servlet;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.entity.util.LengthConstrainedStringWriter;
import cc.alcina.framework.entity.util.LengthConstrainedStringWriter.OverflowException;
import cc.alcina.framework.servlet.LifecycleService;

public class AlcinaServletTopics {
	public static Topic<LengthConstrainedStringWriter.OverflowException> serializationOverflow = Topic
			.local();

	@Registration.Singleton
	public static class Handlers extends LifecycleService {
		private boolean serializationOverflowPersisted;

		Logger logger = LoggerFactory.getLogger(getClass());

		@Override
		public void onApplicationStartup() {
			serializationOverflow.add((k, v) -> onSerializationOverflow(v));
		}

		private synchronized void onSerializationOverflow(OverflowException v) {
			if (!serializationOverflowPersisted) {
				serializationOverflowPersisted = true;
				File file = DataFolderProvider.get().getChildFile(
						SEUtilities.getNestedSimpleName(getClass())
								+ ".serializationOverflow.txt");
				ResourceUtilities.write(v.preOverflowResult, file);
				logger.info("Wrote overflow to {}", file);
			}
		}
	}
}
