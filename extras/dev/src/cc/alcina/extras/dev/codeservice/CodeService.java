package cc.alcina.extras.dev.codeservice;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.net.URL;
import java.nio.file.WatchService;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.extras.dev.codeservice.CodeServerCompilationUnits.PackageUnits;
import cc.alcina.extras.dev.codeservice.SourceFolder.SourceFile;
import cc.alcina.extras.dev.codeservice.SourceFolder.SourcePackage;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.util.fs.FsUtils;

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
	public List<String> sourceAndClassPaths;

	public String sourceFilterRegex = ".+\\.java";

	public List<Class<? extends Handler>> handlerTypes;

	WatchService watchService;

	public boolean blockStartThread;

	public CodeService() {
		context = new Context();
		watchService = FsUtils.watchServiceFor(getClass());
	}

	/*
	 * A type that can be queued in the CodeService eventQueue. Note in this
	 * context "Event" is always a "ChangeEvent"
	 */
	abstract class Event {
		protected abstract Object key();

		long time;

		Context context;

		Event() {
			this.context = getContext();
			this.time = System.currentTimeMillis();
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
		if (blockStartThread) {
			for (;;) {
				try {
					Thread.sleep(TimeConstants.ONE_HOUR_MS);
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
			}
		}
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

	void watchFolder(File folder) {
		try {
			FsUtils.toWatchablePath(folder.toPath()).register(watchService,
					ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	/*
	 * Provides services to event handlers. For the moment, it's just a single
	 * instance per CodeService
	 */
	class Context {
		CodeServerCompilationUnits units;

		void watchFolder(File folder) {
			CodeService.this.watchFolder(folder);
		}

		Context() {
			this.units = compilationUnits;
		}

		void submitFileEvent(File file) {
			SourceFolder sourceFolder = compilationUnits
					.getSourceFolderFor(file);
			if (sourceFolder == null) {
				// not canonically owned by the sourcefolder (a symlink) --
				// ignore
				return;
			}
			if (!sourceFolder.test(file)) {
				return;
			}
			if (publishedInitialStats) {
				Ax.out("File event: %s", file);
			}
			queue.add(new FileEvent(sourceFolder.sourceFile(file)));
			if (sourceFolder.testGeneratePackageEvent(file)) {
				queue.add(new PackageEvent(sourceFolder.sourcePackage(file)));
			}
		}

		public boolean isInSourcePath(String name) {
			return compilationUnits.isInSourcePath(name);
		}

		public URL[] getClassPathUrls() {
			List<URL> urls = compilationUnits.sourceFolders.stream()
					.map(sf -> sf.classPathFolderCanonicalPath).map(File::new)
					.map(File::toURI).map(uri -> {
						try {
							return uri.toURL();
						} catch (Exception e) {
							throw WrappedRuntimeException.wrap(e);
						}
					}).collect(Collectors.toList());
			return urls.toArray(new URL[urls.size()]);
		}
	}

	static Class forName(String qualifiedBinaryName) {
		Class clazz = null;
		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		if (contextClassLoader == null) {
			clazz = Reflections.forName(qualifiedBinaryName);
		} else {
			try {
				clazz = contextClassLoader.loadClass(qualifiedBinaryName);
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
		return clazz;
	}

	Context context;

	// just for event construction
	private Context getContext() {
		return context;
	}

	void handleEvent(Event event) {
		this.context = new Context();
		if (publishedInitialStats) {
			long elapsed = System.currentTimeMillis() - event.time;
			if (elapsed < 2000) {
				// allow IDE codegen. Todo - listen on IDE build products, not
				// source changes
				try {
					Thread.sleep(2000 - elapsed);
				} catch (Exception e) {
				}
				Thread.currentThread()
						.setContextClassLoader(new DispClassLoader(context,
								getClass().getClassLoader()));
			}
		}
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
			publishedInitialStats = true;
			FormatBuilder format = new FormatBuilder();
			format.line("\nInitial queue stats: [%s ms]",
					System.currentTimeMillis() - queue.startTime);
			queue.eventHisto.toLinkedHashMap(true).entrySet()
					.forEach(e -> format.line("%s=%s",
							NestedName.get(e.getKey()), e.getValue()));
			format.newLine();
			format.out();
		}
	}
}
