package cc.alcina.framework.entity.domaintransform;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.entity.SEUtilities;

public class PersistedTransformUndo {
	private List<DomainTransformEvent> undoTransforms = new ArrayList<DomainTransformEvent>();

	private String log = "";

	String checkMe = "";

	public void generateUndoTransforms(Connection conn,
			List<DomainTransformEvent> events,
			long maxPersistentTransformIdForPriorValue) throws Exception {
		Statement stmt = conn.createStatement();
		SystemoutCounter counter = new SystemoutCounter(1, 10);
		for (DomainTransformEvent dte : events) {
			DomainTransformEvent mostRecent = getMostRecentEvent(dte, stmt,
					maxPersistentTransformIdForPriorValue);
			counter.tick();
			if (mostRecent == null) {
				System.out.format("Unable to find prior transform for:\n%s\n\n",
						dte);
			} else {
				undoTransforms.add(mostRecent);
			}
		}
		counter.end();
		System.out.println(log);
		stmt.close();
	}

	public DomainTransformEvent getMostRecentEvent(DomainTransformEvent evt,
			Statement stmt, long maxPersistentTransformIdForPriorValue)
			throws Exception {
		String tpl = "select * from data_transform_event where id<=%s"
				+ " and objectclassref_id=%s and objectid=%s " + ""
				+ " %s order by id desc limit 1";
		String propNameOrTypeClause = evt.getPropertyName() == null
				? String.format(" and transformtype =%s ",
						evt.getTransformType().ordinal())
				: String.format(" and propertyname='%s' ",
						evt.getPropertyName());
		String sql = String.format(tpl, maxPersistentTransformIdForPriorValue,
				evt.getObjectClassRef().getId(), evt.getObjectId(),
				propNameOrTypeClause);
		ResultSet rs = stmt.executeQuery(sql);
		DomainTransformEvent dte = null;
		boolean exists = rs.next();
		switch (evt.getTransformType()) {
		case CREATE_OBJECT: {
			if (exists) {
				return null;
			} else {
				dte = new DomainTransformEvent();
				dte.setObjectId(evt.getObjectId());
				dte.setObjectClass(evt.getObjectClass());
				dte.setTransformType(TransformType.DELETE_OBJECT);
			}
			break;
		}
		case DELETE_OBJECT: {
			if (exists) {
				// checkMe += evt + "\n";
			}
			dte = new DomainTransformEvent();
			dte.setObjectId(evt.getObjectId());
			dte.setObjectClass(evt.getObjectClass());
			dte.setTransformType(TransformType.CREATE_OBJECT);
			// verry ciruglar
			break;
		}
		case NULL_PROPERTY_REF:
		case CHANGE_PROPERTY_REF:
		case CHANGE_PROPERTY_SIMPLE_VALUE: {
			if (!exists) {
				// ignore default values for the moment -- REVISIT.1
				System.out.format("generating from default - %s.%s ",
						evt.getObjectClass().getSimpleName(),
						evt.getPropertyName());
				switch (evt.getTransformType()) {
				case NULL_PROPERTY_REF:
					break;
				case CHANGE_PROPERTY_REF:
					dte = new DomainTransformEvent();
					dte.setObjectId(evt.getObjectId());
					dte.setObjectClass(evt.getObjectClass());
					dte.setPropertyName(evt.getPropertyName());
					dte.setTransformType(TransformType.NULL_PROPERTY_REF);
					break;
				case CHANGE_PROPERTY_SIMPLE_VALUE:
					dte = new DomainTransformEvent();
					dte.setObjectId(evt.getObjectId());
					dte.setObjectClass(evt.getObjectClass());
					dte.setPropertyName(evt.getPropertyName());
					PropertyDescriptor pd = SEUtilities
							.getPropertyDescriptorByName(evt.getObjectClass(),
									evt.getPropertyName());
					Object instance = evt.getObjectClass().newInstance();
					Object value = pd.getReadMethod().invoke(instance,
							new Object[0]);
					dte.setNewValue(value);
					TransformManager.get().convertToTargetObject(dte);
					dte.setTransformType(value == null
							? TransformType.NULL_PROPERTY_REF
							: TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
					break;
				}
			} else {
				log += String.format(
						"using persisted transform #%s - %s - %s.%s\n",
						rs.getLong("id"), rs.getTimestamp("servercommitdate"),
						evt.getObjectClass().getSimpleName(),
						evt.getPropertyName());
				dte = transformFromRow(rs);
				// checkMe += dte + "\n\n";
			}
			break;
		}
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION:
			// ignore;
			break;
		}
		dte.setCommitType(CommitType.TO_STORAGE);
		return dte;
	}

	public List<DomainTransformEvent> getUndoTransforms() {
		return this.undoTransforms;
	}

	public DomainTransformEvent transformFromRow(ResultSet rs)
			throws Exception {
		DomainTransformEvent dte = new DomainTransformEvent();
		dte.setNewStringValue(rs.getString("newstringvalue"));
		dte.setObjectId(rs.getLong("objectid"));
		dte.setObjectClassRef(ClassRef.forId(rs.getLong("objectclassref_id")));
		dte.setPropertyName(rs.getString("propertyName"));
		dte.setTransformType(
				TransformType.values()[rs.getInt("transformtype")]);
		long vcrid = rs.getLong("valueclassref_id");
		if (vcrid != 0) {
			dte.setValueClassRef(ClassRef.forId(vcrid));
		}
		dte.setUtcDate(new Date());
		if (rs.getLong("valueid") != 0) {
			dte.setValueId(rs.getLong("valueid"));
		}
		return dte;
	}
}
