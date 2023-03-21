package cc.alcina.extras.dev.console.code;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.Io.ReadOp.MapType;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.CsvCols;
import cc.alcina.framework.entity.util.CsvCols.CsvRow;
import cc.alcina.framework.servlet.schedule.ServerTask;

/**
 * <p>
 * Generates a config tree from a flattened tree csv (for the bundle files) and
 * a sequence of .property files
 *
 * <p>
 * Used to compare old and new property/config implemementations
 *
 * @author nick@alcina.cc
 *
 */
@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class TaskRefactorConfigSets3 extends ServerTask {
	StringMap map = new StringMap();

	String propertiesCsv;

	private List<PropertyRow> propertyRows;

	private List<String> configFilePaths = new ArrayList<>();

	private String output;

	private String keyPackages;

	transient String packageName;

	transient String key;

	transient int propertyRowIdx = 2;

	public List<String> getConfigFilePaths() {
		return this.configFilePaths;
	}

	public String getKeyPackages() {
		return this.keyPackages;
	}

	public String getOutput() {
		return this.output;
	}

	public String getPropertiesCsv() {
		return this.propertiesCsv;
	}

	@Override
	public void run() throws Exception {
		StringMap keysByPackage = new StringMap();
		{
			CsvCols cols = CsvCols.parseCsv(propertiesCsv);
			propertyRows = cols.stream().map(PropertyRow::new)
					.collect(Collectors.toList());
			propertyRows.stream().filter(
					pr -> Ax.notBlank(pr.key) && Ax.isBlank(pr.inputSet))
					.forEach(pr -> map.put(pr.key, normalise(pr.value)));
			propertyRows.stream().filter(
					pr -> Ax.notBlank(pr.key) && Ax.isBlank(pr.inputSet))
					.forEach(pr -> keysByPackage.put(pr.key, pr.packageName));
		}
		configFilePaths.stream().map(Io.read()::path)
				.map(op -> op.asMap(MapType.PROPERTY)).forEach(map::putAll);
		output = map.sorted().toPropertyString();
		keyPackages = keysByPackage.sorted().toPropertyString();
	}

	public void setConfigFilePaths(List<String> configFilePaths) {
		this.configFilePaths = configFilePaths;
	}

	public void setKeyPackages(String keyPackages) {
		this.keyPackages = keyPackages;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public void setPropertiesCsv(String propertiesCsv) {
		this.propertiesCsv = propertiesCsv;
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

	enum Header {
		Package, Key, File, Value, Comment, InputSet, OutputSet;
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
			TaskRefactorConfigSets3 parent = TaskRefactorConfigSets3.this;
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
			// NOOP
		}

		boolean hasInterestingOutputSet() {
			return Ax.notBlank(outputSet) && !outputSet.equals("omit")
					&& !outputSet.equals("app");
		}
	}
}
