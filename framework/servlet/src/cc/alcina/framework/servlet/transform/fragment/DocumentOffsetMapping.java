package cc.alcina.framework.servlet.transform.fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location.LocationSnapshot;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Ref;

/*
 * FIXME - contentjrl - migrate to server (task to move this. also doesn't need
 * bean ser, uses kryo)
 */
@Bean(PropertySource.FIELDS)
public final class DocumentOffsetMapping implements ContextObservable {
	@Bean(PropertySource.FIELDS)
	public static final class Entry {
		public static Entry ofOffsetsAndText(int inputIdx, int outputIdx,
				String text) {
			Entry result = new Entry();
			result.text = text;
			result.input = new IntPair(inputIdx, inputIdx + text.length());
			result.output = new IntPair(outputIdx, outputIdx + text.length());
			return result;
		}

		static Entry ofLocationSnapshots(LocationSnapshot source,
				LocationSnapshot transformed) {
			Entry result = new Entry();
			result.input = source.asTextIndexPair();
			result.output = transformed.asTextIndexPair();
			return result;
		}

		public IntPair input;

		public IntPair output;

		public String text;

		int relativeFrom(IntPair outputTest, boolean start) {
			if (start) {
				int test = outputTest.i1;
				if (output.containsExEnd(test)) {
					return input.i1 + (test - output.i1);
				} else {
					return -1;
				}
			} else {
				int test = outputTest.i2;
				// since testing incrementally, the first match will be ok
				// (really it's 'ex start')
				if (output.contains(test)) {
					return input.i1 + (test - output.i1);
				} else {
					return -1;
				}
			}
		}

		@Override
		public String toString() {
			return Ax.format("%s --> %s :: '%s'", input, output, text);
		}
	}

	@Reflected
	public enum Type {
		/*
		 * input to output
		 */
		INPUT_TO_OUTPUT,
		/*
		 * raw document to post-normalisation (tag validation etc)
		 */
		PRE_INPUT_TO_INPUT;
	}

	public Type type;

	public List<Entry> entries = new ArrayList<>();

	public DocumentOffsetMapping withType(Type type) {
		this.type = type;
		return this;
	}

	transient int textLength = 0;

	class ContiguousData {
		ContiguousData(int startOfDiscontiguousSourceIndex) {
			this.startOfDiscontiguousSourceIndex = startOfDiscontiguousSourceIndex;
			int lastContiguous = 0;
			for (Entry entry : entries) {
				if (entry.input.i1 >= startOfDiscontiguousSourceIndex) {
					discontiguousOffsetClosestContiguous.put(entry.input.i1,
							lastContiguous);
				} else {
					lastContiguous = entry.input.i1;
				}
				targetOffsetEntry.put(entry.output.i1, entry);
			}
		}

		int startOfDiscontiguousSourceIndex;

		TreeMap<Integer, Entry> targetOffsetEntry = new TreeMap<>();

		TreeMap<Integer, Integer> discontiguousOffsetClosestContiguous = new TreeMap<>();

		public int getHighestContiguousIndex(IntPair outputPair,
				boolean atStart) {
			int idx = atStart ? outputPair.i1 : outputPair.i2;
			if (idx > startOfDiscontiguousSourceIndex) {
				idx = discontiguousOffsetClosestContiguous.floorEntry(idx)
						.getValue();
			}
			Map.Entry<Integer, Entry> entry = targetOffsetEntry.floorEntry(idx);
			if (entry.getKey() == idx && !atStart && !outputPair.isPoint()) {
				entry = targetOffsetEntry.floorEntry(idx - 1);
			}
			IntPair pair = entry.getValue().input;
			return atStart ? pair.i1 : pair.i2;
		}
	}

	transient ContiguousData contiguousData;

	public void setStartOfDiscontiguousSourceIndex(
			int startOfDiscontiguousSourceIndex) {
		contiguousData = new ContiguousData(startOfDiscontiguousSourceIndex);
	}

	public void add(int inputIdx, int outputIdx, String text) {
		entries.add(Entry.ofOffsetsAndText(inputIdx, outputIdx, text));
		textLength += text.length();
	}

	public Entry getSourceEntry(String textContents) {
		return entries.stream().filter(e -> e.text.contains(textContents))
				.findFirst().orElse(null);
	}

	public Entry getSourceEntry(int startIdx) {
		return entries.stream().filter(e -> e.output.contains(startIdx))
				.findFirst().orElse(null);
	}

	public IntPair getSource(IntPair outputPair) {
		int relativeToStart = getSourceIndex(outputPair, true);
		int relativeToEnd = getSourceIndex(outputPair, false);
		return relativeToStart == -1 || relativeToEnd == -1 ? null
				: new IntPair(relativeToStart, relativeToEnd);
	}

	private int getSourceIndex(IntPair outputPair, boolean atStart) {
		if (contiguousData != null) {
			return contiguousData.getHighestContiguousIndex(outputPair,
					atStart);
		} else {
			return entries.stream()
					.map(m -> m.relativeFrom(outputPair, atStart))
					.filter(i -> i != -1).findFirst().orElse(-1);
		}
	}

	public String toNewlineSeparatedTexts() {
		return entries.stream().map(e -> e.text)
				.collect(Collectors.joining("\n"));
	}

	public String toNewlineSeparatedNumberedTexts() {
		Ref<Integer> acc = Ref.of(0);
		return entries.stream().map(e -> {
			String result = Ax.format("%s: %s", acc.get(), e.text);
			acc.set(acc.get() + e.text.length());
			return result;
		}).collect(Collectors.joining("\n"));
	}

	public String toNewlineSeparatedNumberedOffsetTexts() {
		Ref<Integer> acc = Ref.of(0);
		return entries.stream().map(e -> {
			String result = Ax.format("%s :: %s :: %s: %s", e.input, e.output,
					acc.get(), e.text);
			acc.set(acc.get() + e.text.length());
			return result;
		}).collect(Collectors.joining("\n"));
	}

	public String toNewlineSeparatedNumberedOffsets() {
		return entries.stream()
				.map(e -> Ax.format("%s :: %s ", e.input, e.output))
				.collect(Collectors.joining("\n"));
	}

	public String text() {
		StringBuilder builder = new StringBuilder();
		entries.forEach(e -> builder.append(e.text));
		return builder.toString();
	}

	/*
	 * Builds a mapping from two snapshots of a dom tree
	 */
	public static class SnapshotBuilder {
		DomDocument document;

		public SnapshotBuilder(DomDocument document) {
			this.document = document;
		}

		public void snapshot() {
			snapshots.add(new LocationsSnapshot());
		}

		List<LocationsSnapshot> snapshots = new ArrayList<>();

		class LocationsSnapshot {
			Map<DomNode, LocationSnapshot> nodeLocationSnapshot;

			LocationsSnapshot() {
				nodeLocationSnapshot = document.stream()
						.collect(AlcinaCollectors.toValueMap(
								n -> n.asLocation().toLocationSnapshot()));
			}
		}

		/*
		 * The nodes in the second snapshot may not be contained in the first -
		 * clients of the mapping will need to deal with that
		 */
		public DocumentOffsetMapping createMapping(Type type) {
			DocumentOffsetMapping mapping = new DocumentOffsetMapping()
					.withType(type);
			LocationsSnapshot sourceSnapshot = snapshots.get(0);
			LocationsSnapshot transformedSnapshot = snapshots.get(1);
			sourceSnapshot.nodeLocationSnapshot.entrySet().stream()
					.forEach(e -> {
						LocationSnapshot sourceLocation = e.getValue();
						LocationSnapshot transformedLocation = transformedSnapshot.nodeLocationSnapshot
								.get(e.getKey());
						if (transformedLocation != null
								&& sourceLocation.hasTextLength()
								&& transformedLocation.hasTextLength()) {
							mapping.entries.add(DocumentOffsetMapping.Entry
									.ofLocationSnapshots(sourceLocation,
											transformedLocation));
						}
					});
			return mapping;
		}
	}
}
