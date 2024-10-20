package cc.alcina.framework.servlet.process.observer.job;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.entity.util.ZipUtil;
import cc.alcina.framework.servlet.component.sequence.Sequence;
import cc.alcina.framework.servlet.process.observer.mvcc.MvccObserver;

/**
 * A detailed history of changes to the observed mvcc object. This
 */
public class JobHistory {
	public static final String LOCAL_PATH = "/tmp/sequence/job";

	public Job job;

	public List<JobObservable> observables = new CopyOnWriteArrayList<>();

	public JobHistory(Job job) {
		this.job = job;
	}

	void add(JobObservable observable) {
		observables.add(observable);
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder();
		format.line("entity: %s", ((Entity) job).toStringId());
		format.indent(1);
		observables.forEach(format::line);
		return format.toString();
	}

	public class JobSequence {
		public boolean includeMvccObservables = true;

		public JobSequence
				withIncludeMvccObservables(boolean includeMvccObservables) {
			this.includeMvccObservables = includeMvccObservables;
			return this;
		}

		public File exportLocal() {
			Stream<? extends IdOrdered> stream = observables.stream();
			if (includeMvccObservables) {
				stream = Stream.concat(stream,
						MvccObserver.getHistory(job.toLocator()).sequence()
								.getEvents().stream());
			}
			List<? extends IdOrdered> events = stream.sorted()
					.collect(Collectors.toList());
			File folder = new File(LOCAL_PATH);
			Sequence.Loader.writeElements(folder, events);
			File datestampedFolder = createLocalDatestampedEventFolder();
			Sequence.Loader.writeElements(datestampedFolder, events);
			Ax.out("wrote job event sequence to %s", datestampedFolder);
			return datestampedFolder;
		}
	}

	static File createLocalDatestampedEventFolder() {
		String persistentExportPath = Configuration.get("persistentExportPath");
		File persistentFolder = new File(persistentExportPath);
		persistentFolder.mkdirs();
		File datestampedFolder = FileUtils.child(persistentFolder,
				Ax.timestampYmd(new Date()));
		return datestampedFolder;
	}

	public static File unzipExportedEvents(InputStream inputStream) {
		File datestampedFolder = createLocalDatestampedEventFolder();
		try {
			new ZipUtil().unzip(datestampedFolder, inputStream);
			return datestampedFolder;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	/**
	 * 
	 * @return A Sequence instance, to export the history in a form to be
	 *         rendered by the Alcina sequence viewer
	 */
	public JobSequence sequence() {
		return new JobSequence();
	}
}
