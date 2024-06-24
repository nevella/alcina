package cc.alcina.extras.dev.codeservice;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.extras.dev.codeservice.CodeServerCompilationUnits.PackageUnits;
import cc.alcina.extras.dev.codeservice.SourceFolder.SourceFile;
import cc.alcina.extras.dev.codeservice.SourceFolder.SourcePackage;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;

/**
 * <h2>Implementation sketch</h2>
 * 
 * <p>
 * 
 * - invoke from a devconsole via:
 * 
 * <pre>
 * <code>
 * 
 	 CodeService codeService = new CodeService();
	// the listpath file might contain - say -
	// /g/alcina/framework/servlet
	codeService.sourceFolderPaths = Io.read()
			.resource("codeserver-paths.local.txt").asList();
	codeService.handlerTypes = List
			.of(PackagePropertiesGenerator.class);
	codeService.start();
 * </pre>
 * <p>
 * The codeservice operates by creating a taskqueue with a set of initial tasks
 * - it's designed to operate with just one task executor thread (to minimise
 * resource use + simplify concurrency)
 * <ul>
 * <li>the instance creates the task queue, and submits an (initial list) of
 * SourceFolderEvent tasks. Those tasks are responsible for managing the
 * scanning of each folder
 * 
 * 
 * 
 * </ul>
 * 
 * 
 */
@Feature.Ref(Feature_CodeService.class)
public class CodeService {
	public List<String> sourceFolderPaths;

	public String sourceFilterRegex = ".+\\.java";

	public List<Class<? extends Handler>> handlerTypes;

	public CodeService() {
		context = new Context();
	}

	/*
	 * A type that can be queued in the CodeService eventQueue. Note in this
	 * context "Event" is always a "ChangeEvent"
	 */
	abstract class Event {
		protected abstract Object key();

		Context context;

		Event() {
			this.context = getContext();
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s", NestedName.get(this), key());
		}
	}

	EventQueue queue;

	CodeServerCompilationUnits compilationUnits;

	public void start() {
		compilationUnits = new CodeServerCompilationUnits(this);
		queue = new EventQueue(this);
		compilationUnits.sourceFolders.stream().map(SourceFolderEvent::new)
				.forEach(queue::add);
		queue.start();
	}

	/*
	 * Models a file that should be processed by the listeners
	 */
	public class FileEvent extends Event {
		public SourceFile file;

		FileEvent(SourceFolder.SourceFile file) {
			this.file = file;
		}

		@Override
		protected Object key() {
			return file;
		}
	}

	/*
	 * Models a package that should be processed by the listeners
	 */
	public class PackageEvent extends Event {
		public SourcePackage sourcePackage;

		public PackageEvent(SourcePackage sourcePackage) {
			this.sourcePackage = sourcePackage;
		}

		@Override
		protected Object key() {
			return sourcePackage;
		}

		public PackageUnits packageUnits() {
			return context.units.getPackageUnits(sourcePackage);
		}
	}

	/*
	 * Models a SourceFolder that should be processed by the listeners
	 */
	public class SourceFolderEvent extends Event {
		public SourceFolder sourceFolder;

		@Override
		protected Object key() {
			return sourceFolder;
		}

		SourceFolderEvent(SourceFolder sourceFolder) {
			this.sourceFolder = sourceFolder;
		}
	}

	/*
	 * A class which receives queue events, possibly of multiple types
	 */
	public interface Handler {
		void handle(Event event);

		static abstract class Abstract implements Handler {
			Logger logger = LoggerFactory.getLogger(getClass());
		}
	}

	/*
	 * Provides services to event handlers. For the moment, it's just a single
	 * instance per CodeService
	 */
	class Context {
		CodeServerCompilationUnits units;

		Context() {
			this.units = compilationUnits;
		}

		void submitFileEvent(SourceFolder sourceFolder, File file) {
			queue.add(new FileEvent(sourceFolder.sourceFile(file)));
			queue.add(new PackageEvent(sourceFolder.sourcePackage(file)));
		}
	}

	Context context;

	// just for event construction
	private Context getContext() {
		return context;
	}

	void handleEvent(Event event) {
		Context context = new Context();
		new SourceFolderScanner().handle(event);
		compilationUnits.handle(event);
		for (Class<? extends Handler> handlerType : handlerTypes) {
			try {
				Reflections.newInstance(handlerType).handle(event);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	boolean publishedInitialStats;

	void onEmptyQueue() {
		if (!publishedInitialStats) {
			FormatBuilder format = new FormatBuilder();
			format.line("Initial queue stats:");
			queue.eventHisto.toLinkedHashMap(true).entrySet()
					.forEach(e -> format.line("%s=%s",
							NestedName.get(e.getKey()), e.getValue()));
			format.out();
		}
	}
}
