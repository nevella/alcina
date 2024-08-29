package cc.alcina.framework.servlet.story.doc;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.gwt.client.story.Story.Point;
import cc.alcina.framework.gwt.client.story.StoryTeller;
import cc.alcina.framework.gwt.client.story.StoryTeller.AfterPerformAction;
import cc.alcina.framework.gwt.client.story.StoryTeller.BeforeStory;
import cc.alcina.framework.gwt.client.story.StoryTeller.BeforeVisit;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit;
import cc.alcina.framework.gwt.client.story.doc.ScreenshotData;
import cc.alcina.framework.gwt.client.story.doc.StoryDocObservable;
import cc.alcina.framework.gwt.client.story.doc.StoryDocObservable.DocumentPoint;
import cc.alcina.framework.servlet.story.doc.StoryDocPart.RendererConfiguration;

class ObservableRecorder {
	StoryDoc storyDoc;

	Storage storage;

	AtomicInteger observableCounter = new AtomicInteger();

	ObservableRecorder(StoryDoc storyDoc) {
		this.storyDoc = storyDoc;
		this.storage = new Storage();
	}

	class Storage {
		File folder;

		File currentObservableFile;

		Storage() {
			this.folder = new File(Ax.format("%s/%s", storyDoc.part.path,
					Ax.timestampYmd(new Date())));
		}

		void ensureFolder() {
			folder.mkdirs();
		}

		public void storeObservable(String json) {
			currentObservableFile = FileUtils.child(folder,
					Ax.format("%s.json", observableCounter.incrementAndGet()));
			Io.write().string(json).toFile(currentObservableFile);
		}

		public List<StoryDocObservable> getObservables() {
			return Arrays.stream(folder.listFiles())
					.filter(f -> f.getName().endsWith(".json"))
					.<StoryDocObservable> map(f -> Io.read().file(f)
							.asReflectiveSerializedObject())
					.sorted().collect(Collectors.toList());
		}

		public void storeObservable(DocumentPoint observable) {
			String json = ReflectiveSerializer.serialize(observable);
			storeObservable(json);
			Ax.out("stored message :: %s", observable.visit.pathDisplayName());
		}

		public void updateCurrentObservable(byte[] screenshotBytes) {
			DocumentPoint observable = Io.read().file(currentObservableFile)
					.asReflectiveSerializedObject();
			observable.screenshot = screenshotBytes;
			String json = ReflectiveSerializer.serialize(observable);
			Io.write().string(json).toFile(currentObservableFile);
		}
	}

	void observe() {
		ProcessObservers.context().observe(new BeforeStoryObserver());
		ProcessObservers.context().observe(new BeforeVisitObserver());
		ProcessObservers.context().observe(new AfterPerformActionObserver());
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
			if (visit.getDescription() != null) {
				storage.storeObservable(new StoryDocObservable.DocumentPoint(
						visit, visit.getDescription()));
			}
		}
	}

	class AfterPerformActionObserver
			implements ProcessObserver<StoryTeller.AfterPerformAction> {
		@Override
		public void topicPublished(AfterPerformAction message) {
			byte[] screenshotBytes = message.getState()
					.getAttribute(ScreenshotData.class).get();
			if (screenshotBytes != null) {
				storage.updateCurrentObservable(screenshotBytes);
			}
		}
	}

	class AfterStoryObserver
			implements ProcessObserver<StoryTeller.AfterStory> {
		@Override
		public void topicPublished(StoryTeller.AfterStory message) {
			Ax.out("stored observables to %s", storage.folder);
			RendererConfiguration rendererConfiguration = storyDoc.part.rendererConfiguration;
			if (rendererConfiguration != null) {
				List<StoryDocObservable> observables = storage.getObservables();
				observables = observables.stream()
						.filter(obv -> testPoint(obv.pointClassName))
						.collect(Collectors.toList());
				Reflections.newInstance(rendererConfiguration.renderer)
						.render(storyDoc.part, storage.folder, observables);
			}
		}

		boolean testPoint(String pointClassName) {
			RendererConfiguration rendererConfiguration = storyDoc.part.rendererConfiguration;
			Class<? extends Point> pointFilter = rendererConfiguration.pointFilter;
			if (pointFilter != null) {
				return pointClassName.startsWith(pointFilter.getName());
			} else {
				return true;
			}
		}
	}
}
