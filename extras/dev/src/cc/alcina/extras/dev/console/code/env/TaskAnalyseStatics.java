package cc.alcina.extras.dev.console.code.env;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.Csv;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * Analyse a list of classes and report non-trivial statics and/or singletons
 */
public class TaskAnalyseStatics extends PerformerTask.Fields {
	public String classList;

	public String classNameFilter = ".*";

	public transient Result result = new Result();

	transient Set<String> superClassNames = new TreeSet<>();

	@Override
	public void run() throws Exception {
		List<String> seedNames = Arrays.stream(classList.split("\n"))
				.filter(n -> n.matches(classNameFilter))
				.collect(Collectors.toList());
		seedNames.forEach(n -> {
			Class clazz = null;
			try {
				clazz = Reflections.forName(n);
			} catch (Exception e) {
				Ax.out("Exception loading %s :: %s", n,
						CommonUtils.toSimpleExceptionMessage(e));
				return;
			}
			if (clazz.isAnonymousClass() || clazz.isLocalClass()
					|| clazz.isInterface()) {
				return;
			}
			while (clazz != Object.class) {
				superClassNames.add(clazz.getName());
				clazz = clazz.getSuperclass();
			}
		});
		superClassNames.stream().map(ClassStatics::new)
				.forEach(result.list::add);
	}

	public class ClassStatics {
		String className;

		boolean notLoadable;

		boolean registrySingleton;

		List<StaticField> staticFields = new ArrayList<>();

		Class<?> clazz;

		ClassStatics(String className) {
			this.className = className;
			try {
				this.clazz = Reflections.forName(className);
			} catch (Exception e) {
				Ax.out("Exception loading %s :: %s", className,
						CommonUtils.toSimpleExceptionMessage(e));
				notLoadable = true;
				return;
			}
			if (clazz.isAnonymousClass() || clazz.isLocalClass()
					|| clazz.isInterface()) {
				return;
			}
			ClassReflector<?> reflector = Reflections.at(clazz);
			registrySingleton = reflector.has(Registration.Singleton.class);
			Arrays.stream(clazz.getDeclaredFields())
					.filter(f -> Modifier.isStatic(f.getModifiers())
							&& !Modifier.isFinal(f.getModifiers())
							&& !f.getName().startsWith("$SWITCH_TABLE"))
					.map(StaticField::new).forEach(staticFields::add);
		}

		public boolean hasStatics() {
			return registrySingleton || staticFields.size() > 0;
		}

		public class StaticField {
			Field field;

			StaticField(Field field) {
				this.field = field;
			}

			@Override
			public String toString() {
				return Ax.format("%s :: %s", field.getName(),
						NestedName.get(field.getType()));
			}
		}

		public String toDetailString(boolean withName) {
			FormatBuilder format = new FormatBuilder().separator(" - ");
			if (withName) {
				format.append(NestedName.get(clazz));
			}
			if (registrySingleton) {
				format.append("registrySingleton: true");
			}
			if (staticFields.size() > 0) {
				format.format("staticFields: %s", staticFields);
			}
			return format.toString();
		}
	}

	public class Result {
		public List<ClassStatics> list = new ArrayList<>();

		public String toUnresolvedString() {
			FormatBuilder format = new FormatBuilder();
			list.stream().filter(ClassStatics::hasStatics)
					.forEach(cs -> format.line(cs.toDetailString(true)));
			return format.toString();
		}
	}

	public static class PersistentResult {
		public PersistentResult(String csvText) {
			Csv csv = Csv.parseCsv(csvText);
			csv.stream().map(Entry::new).forEach(entries::add);
		}

		public List<Entry> entries = new ArrayList<>();

		transient Map<String, Entry> classNameEntry;

		public PersistentResult(Result result) {
			entries = result.list.stream().filter(ClassStatics::hasStatics)
					.map(Entry::new).collect(Collectors.toList());
		}

		public String toCsvString() {
			Csv csv = Csv.blankWithKeys(Keys.class);
			entries.forEach(e -> e.toRow(csv.addRow()));
			return csv.toCsv();
		}

		enum Keys {
			name, nestedName, descriptor, type, notes
		}

		public enum OkType {
			// has a context provider (uses contextFrame)
			CONTEXT_PROVIDER,
			// unused in multi-threaded env (js/devmode client only)
			UNUSED_MT,
			// correctly behaves as a static (probably multi-client
			// aware/synchronized)
			CORRECT_STATIC,
			// static field type is a stateless type marker
			CORRECT_STATIC_TYPE_MARKER,
			// static field is a switch table
			SWITCH_TABLE,
			// it's a jso, so client only
			JSO,
			// tricky...
			TODO, TO_FRAME,
			// fixed (by changing)
			FIXED, JVM_ONLY
		}

		public static class Entry {
			Entry(Csv.Row row) {
				name = row.get("name");
				nestedName = row.get("nestedName");
				descriptor = row.get("descriptor");
				type = CommonUtils.getEnumValueOrNull(OkType.class,
						row.get("type"));
				notes = row.get("notes");
			}

			void toRow(Csv.Row row) {
				row.set("name", name);
				row.set("nestedName", nestedName);
				row.set("descriptor", descriptor);
				row.set("type", type);
				row.set("notes", notes);
			}

			Entry(ClassStatics statics) {
				name = statics.className;
				nestedName = NestedName.get(statics.clazz);
				descriptor = statics.toDetailString(false);
			}

			String name;

			String nestedName;

			String descriptor;

			OkType type;

			String notes;

			public void mergeAnalysisFrom(Entry fromEntry) {
				type = fromEntry.type;
				notes = fromEntry.notes;
			}
		}

		public void mergeAnalysisFrom(String path) {
			PersistentResult from = new PersistentResult(
					Io.read().path(path).asString());
			ensureLookup();
			from.entries.forEach(fromEntry -> {
				Entry entry = classNameEntry.get(fromEntry.name);
				if (entry != null) {
					entry.mergeAnalysisFrom(fromEntry);
				}
			});
		}

		void ensureLookup() {
			if (classNameEntry == null) {
				classNameEntry = entries.stream()
						.collect(AlcinaCollectors.toKeyMap(e -> e.name));
			}
		}
	}
}
