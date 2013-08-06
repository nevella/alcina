package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SyncSet {
	private List<SyncObjectData> objectData = new ArrayList<SyncObjectData>();

	public List<SyncObjectData> getObjectData() {
		return this.objectData;
	}

	public void setObjectData(List<SyncObjectData> objectData) {
		this.objectData = objectData;
	}

	public <T2> void merge(SyncLandscape<T2> landscape) {
		int deltaCount = 0;
		for (SyncObjectData data : objectData) {
			T2 applicable = landscape.findApplicableObject(data);
			if (applicable != null) {
				landscape.applyData(applicable, data);
			}
		}
	}

	public static List<SyncMergeData> merge(SyncSet source, SyncSet target,
			SyncLandscape targetLandscape) {
		List<SyncMergeData> result = new ArrayList<SyncMergeData>();
		Set<SyncObjectData> matchedTargetData = new LinkedHashSet<SyncObjectData>();
		for (SyncObjectData sourceData : source.getObjectData()) {
			Object targetObject = targetLandscape
					.findApplicableObject(sourceData);
			SyncObjectData targetData = targetObject == null ? null
					: targetLandscape.toObjectData(targetObject);
			matchedTargetData.add(targetData);
			SyncMergeData merge = new SyncMergeData(sourceData, targetData,
					targetLandscape);
			result.add(merge);
		}
		for (SyncObjectData targetData : target.getObjectData()) {
			if (!matchedTargetData.contains(targetData)) {
				SyncMergeData merge = new SyncMergeData(null, targetData,
						targetLandscape);
				result.add(merge);
			}
		}
		return result;
	}
}
