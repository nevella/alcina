package cc.alcina.framework.entity.story.doc;

import java.io.File;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Story.Decl.Label;
import cc.alcina.framework.gwt.client.story.StoryTeller;
import cc.alcina.framework.gwt.client.story.StoryTeller.BeforeStory;
import cc.alcina.framework.gwt.client.story.StoryTeller.BeforeVisit;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit;

class ObservableRecorder {
	StoryDoc storyDoc;

	Storage storage;

	AtomicInteger messageCounter = new AtomicInteger();

	ObservableRecorder(StoryDoc storyDoc) {
		this.storyDoc = storyDoc;
		this.storage = new Storage();
	}

	class Storage {
		File folder;

		Storage() {
			this.folder = new File(Ax.format("%s/%s", storyDoc.part.path,
					Ax.timestampYmd(new Date())));
		}

		void ensureFolder() {
			folder.mkdirs();
		}

		public void storeMessage(String json) {
			File child = FileUtils.child(folder,
					Ax.format("%s.json", messageCounter.incrementAndGet()));
			Io.write().string(json).toFile(child);
		}
	}

	void observe() {
		ProcessObservers.context().observe(new BeforeStoryObserver());
		ProcessObservers.context().observe(new BeforeVisitObserver());
		ProcessObservers.context().observe(new DocumentPointObserver());
		ProcessObservers.context().observe(new AfterStoryObserver());
	}

	class BeforeStoryObserver
			implements ProcessObserver<StoryTeller.BeforeStory> {
		@Override
		public void topicPublished(BeforeStory message) {
			storage.ensureFolder();
		}
	}

	class BeforeVisitObserver
			implements ProcessObserver<StoryTeller.BeforeVisit> {
		@Override
		public void topicPublished(BeforeVisit message) {
			Visit visit = message.getVisit();
			Label label = Reflections.at(visit.pointClass())
					.annotation(Decl.Label.class);
			if (label != null) {
				new StoryDocObservable.DocumentPoint(visit, label.value())
						.publish();
			}
		}
	}

	class DocumentPointObserver
			implements ProcessObserver<StoryDocObservable.DocumentPoint> {
		@Override
		public void topicPublished(StoryDocObservable.DocumentPoint message) {
			String json = JacksonUtils.serialize(message);
			storage.storeMessage(json);
			Ax.out("stored message :: %s", message.visit.pathDisplayName());
		}
	}

	class AfterStoryObserver
			implements ProcessObserver<StoryTeller.BeforeStory> {
		@Override
		public void topicPublished(BeforeStory message) {
			// render the doc
		}
	}
}
