package cc.alcina.framework.gwt.persistence.client;

import java.io.Serializable;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

@BeanInfo(displayNamePropertyName = "null")
@Introspectable
public class DatabaseStatsInfo implements Serializable {
	private CountingMap<String> transformTexts = new CountingMap<String>();

	private CountingMap<String> transformCounts = new CountingMap<String>();

	private CountingMap<Integer> logSizes = new CountingMap<Integer>();

	private CountingMap<String> deltaCounts = new CountingMap<String>();

	private CountingMap<String> deltaSizes = new CountingMap<String>();

	private long collectionTimeMs = 0;
	
	private int version=0;

	@Override
	public String toString() {
		String out = "\n\nDatabase stats:\n========\n\nTransforms: \n";
		String template = "\t%s : %s  -  %s chars\n";
		for (Entry<String, Integer> entry : transformTexts.entrySet()) {
			out += CommonUtils.formatJ(template,
					CommonUtils.padStringRight(entry.getKey(), 20, ' '),
					transformCounts.get(entry.getKey()), entry.getValue());
		}
		out += CommonUtils.formatJ(template,
				CommonUtils.padStringRight("total", 20, ' '),
				transformCounts.sum(), transformTexts.sum());
		out+=("\n\nObject deltas:\n=========\n");
		for (Entry<String, Integer> entry : deltaSizes.entrySet()) {
			out += CommonUtils.formatJ(template,
					CommonUtils.padStringRight(entry.getKey(), 20, ' '),
					deltaCounts.get(entry.getKey()), entry.getValue());
		}
		out += CommonUtils.formatJ(template,
				CommonUtils.padStringRight("total", 20, ' '),
				deltaCounts.sum(), deltaSizes.sum());
		out += "\nLogs: \n";
		out += CommonUtils.formatJ(template,
				CommonUtils.padStringRight("total", 20, ' '), logSizes.size(),
				logSizes.sum());
		out += CommonUtils.formatJ("\n%s %s\n",
				CommonUtils.padStringRight("Total chars: ", 20, ' '), size());
		out += CommonUtils.formatJ("\n%s %s\n",
				CommonUtils.padStringRight("Est. total bytes: ", 20, ' '),
				estimatedBytes());
		out += CommonUtils
				.formatJ("\n%s %s\n", CommonUtils.padStringRight(
						"% of tablet max (50mb): ", 20, ' '),
						estimatedBytes() / 500000);
		out += CommonUtils.formatJ("\n%s %s\n",
				CommonUtils.padStringRight("Stat time: ", 20, ' '),
				collectionTimeMs);
		return out;
	}

	private int estimatedBytes() {
		return transformCounts.sum() * 100 + transformTexts.sum() * 2
				+ logSizes.size() * 100 + logSizes.sum() * 2
				+ deltaCounts.size() * 300 + deltaSizes.sum() * 2;
	}

	public CountingMap<String> getTransformTexts() {
		return transformTexts;
	}

	public void setTransformTexts(CountingMap<String> transformTexts) {
		this.transformTexts = transformTexts;
	}

	public CountingMap<String> getTransformCounts() {
		return transformCounts;
	}

	public void setTransformCounts(CountingMap<String> transformCounts) {
		this.transformCounts = transformCounts;
	}

	public CountingMap<Integer> getLogSizes() {
		return logSizes;
	}

	public void setLogSizes(CountingMap<Integer> logSizes) {
		this.logSizes = logSizes;
	}

	public int size() {
		return logSizes.sum() + transformTexts.sum() + deltaSizes.sum();
	}

	public boolean greaterSizeThan(DatabaseStatsInfo max) {
		return max == null || estimatedBytes() > max.estimatedBytes();
	}

	public long getCollectionTimeMs() {
		return this.collectionTimeMs;
	}

	public void setCollectionTimeMs(long collectionTimeMs) {
		this.collectionTimeMs = collectionTimeMs;
	}

	public CountingMap<String> getDeltaCounts() {
		return this.deltaCounts;
	}

	public void setDeltaCounts(CountingMap<String> deltaCounts) {
		this.deltaCounts = deltaCounts;
	}

	public CountingMap<String> getDeltaSizes() {
		return this.deltaSizes;
	}

	public void setDeltaSizes(CountingMap<String> deltaSizes) {
		this.deltaSizes = deltaSizes;
	}

	public int getVersion() {
		return this.version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

}