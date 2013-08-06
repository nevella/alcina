package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;

public class SyncInfoSet<T> {
	private List<SyncInfoObjectData> objectData = new ArrayList<SyncInfoObjectData>();

	private List<SyncInfoConversionSpec> conversionSpecs = new ArrayList<SyncInfoConversionSpec>();

	public List<SyncInfoObjectData> getObjectData() {
		return this.objectData;
	}

	public void setObjectData(List<SyncInfoObjectData> objectData) {
		this.objectData = objectData;
	}

	public List<SyncInfoConversionSpec> getConversionSpecs() {
		return this.conversionSpecs;
	}

	public void setConversionSpecs(List<SyncInfoConversionSpec> conversionSpecs) {
		this.conversionSpecs = conversionSpecs;
	}
	public <T2> void merge(SyncInfoLandscape<T2> landscape){
		int deltaCount=0;
		for(SyncInfoObjectData data:objectData){
			T2 applicable = landscape.findApplicableObject(data);
			if(applicable!=null){
				landscape.applyData(applicable, data);
				
			}
		}
	}
}
