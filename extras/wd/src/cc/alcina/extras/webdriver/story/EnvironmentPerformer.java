package cc.alcina.extras.webdriver.story;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Intersection;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;

public class EnvironmentPerformer
		extends WdActionPerformer<Story.Action.Environment> {
	static final Configuration.Key downloadsFolderKey = Configuration
			.key("downloadsFolder");

	static File downloadsFolder = new File(downloadsFolderKey.get());

	static class MarkedDownloads {
		public MarkedDownloads(Set<File> files) {
			this.files = files;
		}

		Set<File> files = new LinkedHashSet<>();

		Set<File> diff(MarkedDownloads other, String pathRegex) {
			return Intersection.of(files, other.files).delta()
					.filter(f -> !f.getName().endsWith("crdownload")
							&& f.getName().matches(pathRegex))
					.collect(AlcinaCollectors.toLinkedHashSet());
		}

		public boolean changedFrom(MarkedDownloads other, String pathRegex) {
			return diff(other, pathRegex).size() > 0;
		}
	}

	public static interface MarkedDownloadsAttr
			extends PerformerAttribute<MarkedDownloads> {
		static MarkedDownloads ensure(Story.Action.Context context) {
			return context
					.ensureAttribute(MarkedDownloadsAttr.class, () -> mark())
					.get();
		}

		static MarkedDownloads mark() {
			Set<File> files = Arrays.stream(downloadsFolder.listFiles())
					.filter(File::isFile)
					.collect(AlcinaCollectors.toLinkedHashSet());
			return new MarkedDownloads(files);
		}
	}

	@Override
	public void perform(Context context, Story.Action.Environment action)
			throws Exception {
		super.perform(context, action);
	}

	public static class MarkDownloads
			implements TypedPerformer<Story.Action.Environment.MarkDownloads> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Environment.MarkDownloads action)
				throws Exception {
			MarkedDownloadsAttr.ensure(wdPerformer.context);
		}
	}

	public static class AwaitNewDownload implements
			TypedPerformer<Story.Action.Environment.AwaitNewDownload> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Environment.AwaitNewDownload action)
				throws Exception {
			long timeout = 20 * TimeConstants.ONE_SECOND_MS;
			long start = System.currentTimeMillis();
			MarkedDownloads initial = MarkedDownloadsAttr
					.ensure(wdPerformer.context);
			MarkedDownloads changed = null;
			while (TimeConstants.within(start, timeout)) {
				MarkedDownloads current = MarkedDownloadsAttr.mark();
				if (current.changedFrom(initial, action.newDownloadPathRegex)) {
					changed = current;
					break;
				}
				Thread.sleep(30);
			}
			Preconditions.checkState(changed != null);
			Ax.out("Downloads changed - %s",
					changed.diff(initial, action.newDownloadPathRegex));
		}
	}
}