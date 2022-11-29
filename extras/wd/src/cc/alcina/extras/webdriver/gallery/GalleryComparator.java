package cc.alcina.extras.webdriver.gallery;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.extras.webdriver.gallery.GalleryPersister.Image;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils.ThreeWaySetResult;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.entity.util.Shell;

public class GalleryComparator {
	public static final String COMPARATOR_OUT_PATH = "/tmp/GalleryComparator";

	private String testBase;

	private String baselineBase;

	private String variant;

	Logger logger = LoggerFactory.getLogger(getClass());

	public String baselinePath() {
		return Ax.format("%s/%s", baselineBase, variant);
	}

	public boolean compare() {
		File baselineIndex = new File(new File(baselinePath()), "index.json");
		File currentIndex = new File(new File(currentPath()), "index.json");
		GallerySnapshot baselineSnapshot = JacksonUtils
				.deserializeFromFile(baselineIndex, GallerySnapshot.class);
		GallerySnapshot currentSnapshot = JacksonUtils
				.deserializeFromFile(currentIndex, GallerySnapshot.class);
		ThreeWaySetResult<Image> result = HasEquivalenceHelper
				.threeWaySplit(baselineSnapshot.images, currentSnapshot.images);
		logger.info("Comparing snapshots:\n\t{} ->\n\t{}", currentSnapshot,
				baselineSnapshot);
		if (result.firstOnly.size() > 0) {
			logger.warn("baseline only:\n===========");
			result.firstOnly.forEach(Ax::out);
		}
		if (result.secondOnly.size() > 0) {
			logger.warn("current only:\n===========");
			result.firstOnly.forEach(Ax::out);
		}
		boolean allImagesMatched = result.isIntersectionOnly();
		List<ImagePair> distinct = result.intersection.stream()
				.map(image -> new ImagePair(baselineSnapshot.images,
						currentSnapshot.images, image))
				.filter(ImagePair::isDistinctHashes)
				.collect(Collectors.toList());
		if (distinct.isEmpty()) {
			logger.info("All common images [{}] are identical",
					result.intersection.size());
			return allImagesMatched;
		}
		logger.info("Comparing snapshot metadata:\n\t{} ->\n\t{}",
				currentSnapshot.metadata, baselineSnapshot.metadata);
		logger.info("{}/{} non-identical images", distinct.size(),
				result.intersection.size());
		/*
		 * Assumes imagemagick
		 */
		distinct.forEach(ImagePair::compare);
		return false;
	}

	public String currentPath() {
		return Ax.format("%s/%s", testBase, variant);
	}

	public String getBaselineBase() {
		return this.baselineBase;
	}

	public String getTestBase() {
		return this.testBase;
	}

	public String getVariant() {
		return this.variant;
	}

	public void setBaselineBase(String baselineBase) {
		this.baselineBase = baselineBase;
	}

	public void setTestBase(String testBase) {
		this.testBase = testBase;
	}

	public void setVariant(String variant) {
		this.variant = variant;
	}

	class ImagePair {
		Image baseline;

		Image current;

		public ImagePair(List<Image> baselineImages, List<Image> currentImages,
				Image image) {
			baseline = HasEquivalenceHelper.getEquivalent(baselineImages,
					image);
			current = HasEquivalenceHelper.getEquivalent(currentImages, image);
		}

		void compare() {
			File baselineImage = new File(new File(baselinePath()), fileName());
			File currentImage = new File(new File(currentPath()), fileName());
			File folder = new File(COMPARATOR_OUT_PATH);
			SEUtilities.deleteDirectory(folder);
			folder.mkdirs();
			File diff = new File(folder, fileName());
			// final command ensures 0 exit code from exec
			Shell.exec(
					"source ~/.profile && compare '%s' '%s' '%s' || compare --version",
					baselineImage, currentImage, diff);
			logger.warn("{} - diff:\n {} \n", baselineImage.getName(),
					diff.getPath());
		}

		String fileName() {
			return baseline.fileName;
		}

		boolean isDistinctHashes() {
			return !baseline.sha1Hash.equals(current.sha1Hash);
		}
	}
}
