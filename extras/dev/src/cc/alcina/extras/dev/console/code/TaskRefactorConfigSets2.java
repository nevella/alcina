package cc.alcina.extras.dev.console.code;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.CsvCols;
import cc.alcina.framework.entity.util.CsvCols.CsvRow;
import cc.alcina.framework.servlet.schedule.ServerTask;

@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class TaskRefactorConfigSets2 extends ServerTask {
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

	public String getAppSetsTsv() {
		return this.appSetsTsv;
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
		writeOutputSets();
		writePackageBundles();
		writeAppConfigFiles();
	}

	public void setAppSetsTsv(String appSetsTsv) {
		this.appSetsTsv = appSetsTsv;
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
		// TODO Auto-generated method stub
	}

	private void generatePackageBundles() {
		// TODO Auto-generated method stub
	}

	private void parse() {
		{
			CsvCols cols = CsvCols.parseCsv(propertiesCsv);
			propertyRows = cols.stream().map(PropertyRow::new)
					.collect(Collectors.toList());
		}
		{
			CsvCols cols = CsvCols.parseTsv(appSetsTsv);
			appSets = cols.stream().map(AppSets::new)
					.collect(Collectors.toList());
		}
		{
			CsvCols cols = CsvCols.parseTsv(nonStandardSetsTsv);
			nonStandardSets = cols.stream().map(NonStandardSet::new)
					.collect(Collectors.toList());
		}
		{
			CsvCols cols = CsvCols.parseTsv(setPathsTsv);
			setPaths = cols.stream().map(SetPath::new)
					.collect(Collectors.toList());
		}
	}

	private void writeAppConfigFiles() {
		// TODO Auto-generated method stub
	}

	private void writeOutputSets() {
		// TODO Auto-generated method stub
	}

	private void writePackageBundles() {
		// TODO Auto-generated method stub
	}

	class AppSets {
		String app;

		List<String> sets;

		AppSets(CsvRow row) {
			app = row.get("App");
			sets = List.of(row.get("sets").split(" - "));
		}

		boolean contains(String set) {
			return sets.contains(set);
		}
	}

	enum Header {
		Package, Key, File, Value, Comment, InputSet, OutputSet;
	}

	class NonStandardSet {
		String app;

		String set;

		NonStandardSet(CsvRow row) {
			app = row.get("App");
			set = row.get("Non standard");
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

		PropertyRow(CsvRow row) {
			TaskRefactorConfigSets2 parent = TaskRefactorConfigSets2.this;
			this.rowIdx = parent.propertyRowIdx++;
			String packageName = row.get(Header.Package);
			if (Ax.notBlank(packageName)) {
				parent.packageName = packageName;
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

		SetPath(CsvRow row) {
			path = row.get("Path");
			set = row.get("Set");
		}
	}
}
