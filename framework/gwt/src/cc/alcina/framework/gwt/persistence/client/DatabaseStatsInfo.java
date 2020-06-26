package cc.alcina.framework.gwt.persistence.client;

import java.io.Serializable;
import java.util.Map.Entry;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;

@Bean
@Introspectable
public class DatabaseStatsInfo implements Serializable {
	private CountingMap<String> transformTexts = new CountingMap<String>();

	private CountingMap<String> transformCounts = new CountingMap<String>();

	private CountingMap<Integer> logSizes = new CountingMap<Integer>();

	private CountingMap<String> deltaCounts = new CountingMap<String>();

	private CountingMap<String> deltaSizes = new CountingMap<String>();

	private long collectionTimeMs = 0;

	private int version = 0;

	public long getCollectionTimeMs() {
		return this.collectionTimeMs;
	}

	public CountingMap<String> getDeltaCounts() {
		return this.deltaCounts;
	}

	public CountingMap<String> getDeltaSizes() {
		return this.deltaSizes;
	}

	public CountingMap<Integer> getLogSizes() {
		return logSizes;
	}

	public CountingMap<String> getTransformCounts() {
		return transformCounts;
	}

	public CountingMap<String> getTransformTexts() {
		return transformTexts;
	}

	public int getVersion() {
		return this.version;
	}

	public boolean greaterSizeThan(DatabaseStatsInfo max) {
		return max == null || estimatedBytes() > max.estimatedBytes();
	}

	public void setCollectionTimeMs(long collectionTimeMs) {
		this.collectionTimeMs = collectionTimeMs;
	}

	public void setDeltaCounts(CountingMap<String> deltaCounts) {
		this.deltaCounts = deltaCounts;
	}

	public void setDeltaSizes(CountingMap<String> deltaSizes) {
		this.deltaSizes = deltaSizes;
	}

	public void setLogSizes(CountingMap<Integer> logSizes) {
		this.logSizes = logSizes;
	}

	public void setTransformCounts(CountingMap<String> transformCounts) {
		this.transformCounts = transformCounts;
	}

	public void setTransformTexts(CountingMap<String> transformTexts) {
		this.transformTexts = transformTexts;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public int size() {
		return logSizes.sum() + transformTexts.sum() + deltaSizes.sum();
	}

	@Override
	public String toString() {
		String out = "\n\nDatabase stats:\n========\n\nTransforms: \n";
		String template = "\t%s : %s  -  %s chars\n";
		for (Entry<String, Integer> entry : transformTexts.entrySet()) {
			out += Ax.format(template,
					CommonUtils.padStringRight(entry.getKey(), 20, ' '),
					transformCounts.get(entry.getKey()), entry.getValue());
		}
		out += Ax.format(template, CommonUtils.padStringRight("total", 20, ' '),
				transformCounts.sum(), transformTexts.sum());
		out += ("\n\nObject deltas:\n=========\n");
		for (Entry<String, Integer> entry : deltaSizes.entrySet()) {
			out += Ax.format(template,
					CommonUtils.padStringRight(entry.getKey(), 20, ' '),
					deltaCounts.get(entry.getKey()), entry.getValue());
		}
		out += Ax.format(template, CommonUtils.padStringRight("total", 20, ' '),
				deltaCounts.sum(), deltaSizes.sum());
		out += "\nLogs: \n";
		out += Ax.format(template, CommonUtils.padStringRight("total", 20, ' '),
				logSizes.size(), logSizes.sum());
		out += Ax.format("\n%s %s\n",
				CommonUtils.padStringRight("Total chars: ", 20, ' '), size());
		out += Ax.format("\n%s %s\n",
				CommonUtils.padStringRight("Est. total bytes: ", 20, ' '),
				estimatedBytes());
		out += Ax.format("\n%s %s\n",
				CommonUtils.padStringRight("% of tablet max (50mb): ", 20, ' '),
				estimatedBytes() / 500000);
		out += Ax.format("\n%s %s\n",
				CommonUtils.padStringRight("Stat time: ", 20, ' '),
				collectionTimeMs);
		return out;
	}

	private int estimatedBytes() {
		return transformCounts.sum() * 100 + transformTexts.sum() * 2
				+ logSizes.size() * 100 + logSizes.sum() * 2
				+ deltaCounts.size() * 300 + deltaSizes.sum() * 2;
	}
}