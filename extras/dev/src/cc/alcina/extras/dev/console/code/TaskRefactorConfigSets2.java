package cc.alcina.extras.dev.console.code;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.EncryptionUtils;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.Csv;
import cc.alcina.framework.entity.util.Csv.Row;
import cc.alcina.framework.servlet.schedule.PerformerTask;

@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class TaskRefactorConfigSets2 extends PerformerTask {
	private String propertiesCsv;

	private String appSetsTsv;

	private String setPathsTsv;

	private String nonStandardSetsTsv;

	transient String packageName;

	transient String key;

	transient int propertyRowIdx = 2;

	private transient List<PropertyRow> propertyRows;

	private List<AppSets> appSets;

	private List<NonStandardSet> nonStandardSets;

	private List<SetPath> setPaths;

	transient Map<String, OutputBundle> outputBundles = new LinkedHashMap<>();

	transient Map<String, OutputSet> outputSets = new LinkedHashMap<>();

	transient int limit = 9999;

	private String collisionsHash;

	public String getAppSetsTsv() {
		return this.appSetsTsv;
	}

	public String getCollisionsHash() {
		return this.collisionsHash;
	}

	public String getNonStandardSetsTsv() {
		return this.nonStandardSetsTsv;
	}

	public String getPropertiesCsv() {
		return this.propertiesCsv;
	}

	public String getSetPathsTsv() {
		return this.setPathsTsv;
	}

	@Override
	public void run() throws Exception {
		parse();
		checkSets();
		generateOutputSets();
		generatePackageBundles();
		checkOutputSetCollisions();
		writeOutputSets();
		writePackageBundles();
		writeAppConfigFiles();
	}

	public void setAppSetsTsv(String appSetsTsv) {
		this.appSetsTsv = appSetsTsv;
	}

	public void setCollisionsHash(String collisionsHash) {
		this.collisionsHash = collisionsHash;
	}

	public void setNonStandardSetsTsv(String nonStandardSetsTsv) {
		this.nonStandardSetsTsv = nonStandardSetsTsv;
	}

	public void setPropertiesCsv(String propertiesCsv) {
		this.propertiesCsv = propertiesCsv;
	}

	public void setSetPathsTsv(String setPathsTsv) {
		this.setPathsTsv = setPathsTsv;
	}

	private void checkOutputSetCollisions() {
		// check - for each app - that the key collisions are all verified
		FormatBuilder collisionBuilder = new FormatBuilder();
		appSets.forEach(appSet -> {
			StringMap cumulative = new StringMap();
			for (String set : appSet.sets) {
				OutputSet outputSet = outputSets.get(set);
				StringMap allProperties = outputSet.allProperties();
				allProperties.forEach((k, v) -> {
					if (cumulative.containsKey(k)) {
						collisionBuilder.line(
								"collision :: %s :: %s :: %s => %s", appSet.app,
								k, cumulative.get(k), v);
					}
					cumulative.put(k, v);
				});
			}
		});
		String collisionsString = collisionBuilder.toString();
		String collisionsHash = EncryptionUtils.get().SHA1(collisionsString);
		if (!Objects.equals(collisionsHash, this.collisionsHash)) {
			Ax.err("Collisions hash: %s", collisionsHash);
			Ax.out(collisionsString);
			throw new IllegalStateException();
		}
	}

	// check all sets are in [appsets, non-app-sets]
	private void checkSets() {
		List<String> outputSetNames = propertyRows.stream()
				.filter(PropertyRow::hasInterestingOutputSet)
				.map(PropertyRow::getOutputSet).distinct()
				.collect(Collectors.toList());
		List<String> unmatched = outputSetNames.stream().filter(
				name -> appSets.stream().noneMatch(set -> set.contains(name))
						&& nonStandardSets.stream()
								.noneMatch(set -> set.set.equals(name)))
				.collect(Collectors.toList());
		if (unmatched.size() > 0) {
			Ax.err(unmatched);
			throw new IllegalStateException();
		}
	}

	private void generateOutputSets() {
		propertyRows.stream().filter(PropertyRow::hasInterestingOutputSet)
				.forEach(row -> outputSets
						.computeIfAbsent(row.outputSet, OutputSet::new)
						.add(row));
	}

	private void generatePackageBundles() {
		propertyRows
				.stream().filter(row -> Ax.isBlank(row.inputSet)
						&& Ax.notBlank(row.key) && Ax.notBlank(row.packageName))
				.forEach(row -> {
					OutputBundle bundle = outputBundles.computeIfAbsent(
							row.packageName, OutputBundle::new);
					bundle.path = row.file;
					bundle.map.put(row.key, normalise(row.value));
				});
	}

	private void parse() {
		{
			Csv cols = Csv.parseCsv(propertiesCsv);
			propertyRows = cols.stream().map(PropertyRow::new)
					.collect(Collectors.toList());
		}
		{
			Csv cols = Csv.parseTsv(appSetsTsv);
			appSets = cols.stream().map(AppSets::new)
					.collect(Collectors.toList());
		}
		{
			Csv cols = Csv.parseTsv(nonStandardSetsTsv);
			nonStandardSets = cols.stream().map(NonStandardSet::new)
					.collect(Collectors.toList());
		}
		{
			Csv cols = Csv.parseTsv(setPathsTsv);
			setPaths = cols.stream().map(SetPath::new)
					.collect(Collectors.toList());
		}
	}

	private void writeAppConfigFiles() {
		appSets.stream().limit(limit).forEach(set -> {
			Io.write().string(set.computeOutputString())
					.toPath(set.computeOutputPath());
		});
	}

	private void writeOutputSets() {
		outputSets.values().stream().limit(limit).forEach(set -> {
			Io.write().string(set.computeOutputString())
					.toPath(set.computeOutputPath());
		});
	}

	private void writePackageBundles() {
		outputBundles.values().stream().limit(limit).forEach(bundle -> {
			Io.write().string(bundle.computeOutputString())
					.toPath(bundle.computeOutputPath());
		});
	}

	String normalise(String string) {
		string = Ax.blankToEmpty(string);
		switch (string) {
		case "TRUE":
			return "true";
		case "FALSE":
			return "false";
		default:
			return string;
		}
	}

	class AppSetMatchResult implements Comparable<AppSetMatchResult> {
		String name;

		int index;

		@Override
		public int compareTo(AppSetMatchResult o) {
			return index - o.index;
		}
	}

	class AppSets {
		String app;

		List<String> sets;

		AppSets(Row row) {
			app = row.get("App");
			sets = List.of(row.get("sets").split(" - "));
		}

		String computeFilename() {
			return Ax.format("app_%s.properties", app);
		}

		String computeOutputPath() {
			String bestSetName = sets.stream().map(name -> match(name))
					.collect(Collectors.maxBy(Comparator.naturalOrder()))
					.get().name;
			SetPath setPathMatch = setPaths.stream()
					.filter(setPath -> Objects.equals(setPath.set, bestSetName))
					.findFirst().get();
			return Ax.format("%s/%s", setPathMatch.path, computeFilename());
		}

		String computeOutputString() {
			FormatBuilder format = new FormatBuilder();
			for (String set : sets) {
				OutputSet outputSet = outputSets.get(set);
				format.line("include.resource=/%s", outputSet
						.computeOutputPath().replaceFirst(".+/src/(.+)", "$1"));
			}
			return format.toString();
		}

		boolean contains(String set) {
			return sets.contains(set);
		}

		AppSetMatchResult match(String name) {
			AppSetMatchResult result = new AppSetMatchResult();
			List<String> matchable = setPaths.stream()
					.map(setPath -> setPath.set).collect(Collectors.toList());
			List<String> matchableThisApp = sets.stream()
					.filter(set -> matchable.contains(set))
					.collect(Collectors.toList());
			result.name = name;
			result.index = matchableThisApp.indexOf(name);
			return result;
		}
	}

	enum Header {
		Package, Key, File, Value, Comment, InputSet, OutputSet;
	}

	class NonStandardSet {
		String app;

		String set;

		NonStandardSet(Row row) {
			app = row.get("App");
			set = row.get("Non standard");
		}
	}

	class OutputBundle {
		String path;

		StringMap map = new StringMap();

		String packageName;

		OutputBundle(String packageName) {
			this.packageName = packageName;
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}

		String computeOutputPath() {
			return path.replace("Bundle.properties",
					"configuration.properties");
		}

		String computeOutputString() {
			return map.toPropertyString();
		}
	}

	class OutputSet {
		String name;

		Map<String, OutputPackage> outputPackages = new LinkedHashMap<>();

		OutputSet(String name) {
			this.name = name;
		}

		public StringMap allProperties() {
			StringMap result = new StringMap();
			outputPackages.values().forEach(pkg -> result.putAll(pkg.map));
			return result;
		}

		public String computeOutputPath() {
			String base = null;
			Optional<NonStandardSet> nonStandardSet = nonStandardSets.stream()
					.filter(set -> set.set.equals(name)).findFirst();
			if (nonStandardSet.isPresent()) {
				base = nonStandardSet.get().app;
			} else {
				base = appSets.stream().map(entry -> entry.match(name))
						.collect(Collectors.maxBy(Comparator.naturalOrder()))
						.get().name;
			}
			if (base.matches("(console|production|dev|server|devserver)")) {
				base = "common";
			}
			String f_base = base;
			String folder = setPaths.stream()
					.filter(set -> f_base.startsWith(set.set))
					.max(Comparator.comparing(set -> set.set.length()))
					.get().path;
			return Ax.format("%s/%s", folder, computeFilename());
		}

		public String computeOutputString() {
			FormatBuilder format = new FormatBuilder();
			outputPackages.values().forEach(pkg -> {
				format.line("#%s", pkg.providePackageName());
				format.append(pkg.map.toPropertyString());
				format.newLine();
				format.newLine();
			});
			return format.toString();
		}

		@Override
		public String toString() {
			return name;
		}

		void add(PropertyRow row) {
			outputPackages.computeIfAbsent(row.packageName, OutputPackage::new)
					.add(row);
		}

		String computeFilename() {
			return Ax.format("configuration_%s.properties", name);
		}

		class OutputPackage {
			String packageName;

			StringMap map = new StringMap();

			OutputPackage(String packageName) {
				this.packageName = packageName;
			}

			public String providePackageName() {
				return Ax.blankTo(packageName, " (no package)");
			}

			@Override
			public String toString() {
				return GraphProjection.fieldwiseToStringOneLine(this);
			}

			void add(PropertyRow row) {
				Preconditions.checkState(!map.containsKey(row.key));
				map.put(row.key, normalise(row.value));
			}
		}
	}

	class PropertyRow {
		String packageName;

		String file;

		String key;

		String value;

		String inputSet;

		String outputSet;

		int rowIdx;

		PropertyRow(Row row) {
			TaskRefactorConfigSets2 parent = TaskRefactorConfigSets2.this;
			this.rowIdx = parent.propertyRowIdx++;
			String packageName = row.get(Header.Package);
			if (Ax.notBlank(packageName)) {
				parent.packageName = packageName;
				parent.key = null;
			}
			String key = row.get(Header.Key);
			if (Ax.notBlank(key)) {
				parent.key = key;
			}
			this.packageName = parent.packageName;
			this.key = parent.key;
			this.file = row.get(Header.File);
			this.value = row.get(Header.Value);
			this.inputSet = row.get(Header.InputSet);
			this.outputSet = row.get(Header.OutputSet);
			checkValid();
		}

		public String getOutputSet() {
			return this.outputSet;
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}

		private void checkValid() {
			if (Ax.notBlank(inputSet) && Ax.isBlank(outputSet)) {
				throw Ax.runtimeException("input/outputset mismatch - row %s",
						rowIdx);
			}
		}

		boolean hasInterestingOutputSet() {
			return Ax.notBlank(outputSet) && !outputSet.equals("omit")
					&& !outputSet.equals("app");
		}
	}

	class SetPath {
		String path;

		String set;

		SetPath(Row row) {
			path = row.get("Path");
			set = row.get("Set");
		}
	}
}
