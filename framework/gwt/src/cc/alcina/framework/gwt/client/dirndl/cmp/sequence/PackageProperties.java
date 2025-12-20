package cc.alcina.framework.gwt.client.dirndl.cmp.sequence;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.DetailArea;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.HighlightModel;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceArea;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceComponent;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSettings;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceTable;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import com.google.gwt.dom.client.StyleElement;
import java.lang.Integer;
import java.lang.Runnable;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _SequenceArea sequenceArea = new _SequenceArea();
    public static _SequenceComponent sequenceComponent = new _SequenceComponent();
    public static _SequenceSettings sequenceSettings = new _SequenceSettings();
    static _SequenceTable sequenceTable = new _SequenceTable();
    
    public static class _SequenceArea implements TypedProperty.Container {
      public TypedProperty<SequenceArea, Model> definitionHeader = new TypedProperty<>(SequenceArea.class, "definitionHeader");
      public TypedProperty<SequenceArea, DetailArea> detailArea = new TypedProperty<>(SequenceArea.class, "detailArea");
      public TypedProperty<SequenceArea, List> filteredSequenceElements = new TypedProperty<>(SequenceArea.class, "filteredSequenceElements");
      public TypedProperty<SequenceArea, HighlightModel> highlightModel = new TypedProperty<>(SequenceArea.class, "highlightModel");
      public TypedProperty<SequenceArea, SequencePlace> lastHighlightTestPlace = new TypedProperty<>(SequenceArea.class, "lastHighlightTestPlace");
      public TypedProperty<SequenceArea, SequencePlace> lastSelectedIndexChangePlace = new TypedProperty<>(SequenceArea.class, "lastSelectedIndexChangePlace");
      public TypedProperty<SequenceArea, SequencePlace> lastSequenceTestPlace = new TypedProperty<>(SequenceArea.class, "lastSequenceTestPlace");
      public TypedProperty<SequenceArea, Timer> observableObservedTimer = new TypedProperty<>(SequenceArea.class, "observableObservedTimer");
      public TypedProperty<SequenceArea, InstanceOracle.Query> oracleQuery = new TypedProperty<>(SequenceArea.class, "oracleQuery");
      public TypedProperty<SequenceArea, Runnable> reloadSequenceLambda = new TypedProperty<>(SequenceArea.class, "reloadSequenceLambda");
      public TypedProperty<SequenceArea, Sequence> sequence = new TypedProperty<>(SequenceArea.class, "sequence");
      public TypedProperty<SequenceArea, SequenceTable> sequenceTable = new TypedProperty<>(SequenceArea.class, "sequenceTable");
      public TypedProperty<SequenceArea, SequenceArea.Service> service = new TypedProperty<>(SequenceArea.class, "service");
      public TypedProperty<SequenceArea, StyleElement> styleElement = new TypedProperty<>(SequenceArea.class, "styleElement");
      public TypedProperty<SequenceArea, Runnable> updateStylesLambda = new TypedProperty<>(SequenceArea.class, "updateStylesLambda");
      public static class InstanceProperties extends InstanceProperty.Container<SequenceArea> {
        public  InstanceProperties(SequenceArea source){super(source);}
        public InstanceProperty<SequenceArea, Model> definitionHeader(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.definitionHeader);}
        public InstanceProperty<SequenceArea, DetailArea> detailArea(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.detailArea);}
        public InstanceProperty<SequenceArea, List> filteredSequenceElements(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.filteredSequenceElements);}
        public InstanceProperty<SequenceArea, HighlightModel> highlightModel(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.highlightModel);}
        public InstanceProperty<SequenceArea, SequencePlace> lastHighlightTestPlace(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.lastHighlightTestPlace);}
        public InstanceProperty<SequenceArea, SequencePlace> lastSelectedIndexChangePlace(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.lastSelectedIndexChangePlace);}
        public InstanceProperty<SequenceArea, SequencePlace> lastSequenceTestPlace(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.lastSequenceTestPlace);}
        public InstanceProperty<SequenceArea, Timer> observableObservedTimer(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.observableObservedTimer);}
        public InstanceProperty<SequenceArea, InstanceOracle.Query> oracleQuery(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.oracleQuery);}
        public InstanceProperty<SequenceArea, Runnable> reloadSequenceLambda(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.reloadSequenceLambda);}
        public InstanceProperty<SequenceArea, Sequence> sequence(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.sequence);}
        public InstanceProperty<SequenceArea, SequenceTable> sequenceTable(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.sequenceTable);}
        public InstanceProperty<SequenceArea, SequenceArea.Service> service(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.service);}
        public InstanceProperty<SequenceArea, StyleElement> styleElement(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.styleElement);}
        public InstanceProperty<SequenceArea, Runnable> updateStylesLambda(){return new InstanceProperty<>(source,PackageProperties.sequenceArea.updateStylesLambda);}
      }
      
      public  InstanceProperties instance(SequenceArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _SequenceComponent implements TypedProperty.Container {
      public TypedProperty<SequenceComponent, Model> header = new TypedProperty<>(SequenceComponent.class, "header");
      public TypedProperty<SequenceComponent, SequenceArea> sequenceArea = new TypedProperty<>(SequenceComponent.class, "sequenceArea");
      public TypedProperty<SequenceComponent, SequenceArea.Service> sequenceAreaService = new TypedProperty<>(SequenceComponent.class, "sequenceAreaService");
      public TypedProperty<SequenceComponent, InstanceProperty> sequencePlaceProperty = new TypedProperty<>(SequenceComponent.class, "sequencePlaceProperty");
      public TypedProperty<SequenceComponent, SequenceSettings> sequenceSettings = new TypedProperty<>(SequenceComponent.class, "sequenceSettings");
      public TypedProperty<SequenceComponent, SequenceComponent.SequenceAreaServiceImpl> serviceImpl = new TypedProperty<>(SequenceComponent.class, "serviceImpl");
      public static class InstanceProperties extends InstanceProperty.Container<SequenceComponent> {
        public  InstanceProperties(SequenceComponent source){super(source);}
        public InstanceProperty<SequenceComponent, Model> header(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.header);}
        public InstanceProperty<SequenceComponent, SequenceArea> sequenceArea(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.sequenceArea);}
        public InstanceProperty<SequenceComponent, SequenceArea.Service> sequenceAreaService(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.sequenceAreaService);}
        public InstanceProperty<SequenceComponent, InstanceProperty> sequencePlaceProperty(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.sequencePlaceProperty);}
        public InstanceProperty<SequenceComponent, SequenceSettings> sequenceSettings(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.sequenceSettings);}
        public InstanceProperty<SequenceComponent, SequenceComponent.SequenceAreaServiceImpl> serviceImpl(){return new InstanceProperty<>(source,PackageProperties.sequenceComponent.serviceImpl);}
      }
      
      public  InstanceProperties instance(SequenceComponent instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _SequenceSettings implements TypedProperty.Container {
      public TypedProperty<SequenceSettings, SequenceSettings.ColumnSet> columnSet = new TypedProperty<>(SequenceSettings.class, "columnSet");
      public TypedProperty<SequenceSettings, SequenceSettings.DetailDisplayMode> detailDisplayMode = new TypedProperty<>(SequenceSettings.class, "detailDisplayMode");
      public TypedProperty<SequenceSettings, Integer> maxElementRows = new TypedProperty<>(SequenceSettings.class, "maxElementRows");
      public TypedProperty<SequenceSettings, String> sequenceKey = new TypedProperty<>(SequenceSettings.class, "sequenceKey");
      public static class InstanceProperties extends InstanceProperty.Container<SequenceSettings> {
        public  InstanceProperties(SequenceSettings source){super(source);}
        public InstanceProperty<SequenceSettings, SequenceSettings.ColumnSet> columnSet(){return new InstanceProperty<>(source,PackageProperties.sequenceSettings.columnSet);}
        public InstanceProperty<SequenceSettings, SequenceSettings.DetailDisplayMode> detailDisplayMode(){return new InstanceProperty<>(source,PackageProperties.sequenceSettings.detailDisplayMode);}
        public InstanceProperty<SequenceSettings, Integer> maxElementRows(){return new InstanceProperty<>(source,PackageProperties.sequenceSettings.maxElementRows);}
        public InstanceProperty<SequenceSettings, String> sequenceKey(){return new InstanceProperty<>(source,PackageProperties.sequenceSettings.sequenceKey);}
      }
      
      public  InstanceProperties instance(SequenceSettings instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _SequenceTable implements TypedProperty.Container {
      TypedProperty<SequenceTable, SequenceSettings.ColumnSet> columnSet = new TypedProperty<>(SequenceTable.class, "columnSet");
      TypedProperty<SequenceTable, List> filteredElements = new TypedProperty<>(SequenceTable.class, "filteredElements");
      TypedProperty<SequenceTable, Heading> header = new TypedProperty<>(SequenceTable.class, "header");
      TypedProperty<SequenceTable, SequenceTable.RowsModelSupport> selectionSupport = new TypedProperty<>(SequenceTable.class, "selectionSupport");
      TypedProperty<SequenceTable, SequenceArea> sequenceArea = new TypedProperty<>(SequenceTable.class, "sequenceArea");
      static class InstanceProperties extends InstanceProperty.Container<SequenceTable> {
         InstanceProperties(SequenceTable source){super(source);}
        InstanceProperty<SequenceTable, SequenceSettings.ColumnSet> columnSet(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.columnSet);}
        InstanceProperty<SequenceTable, List> filteredElements(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.filteredElements);}
        InstanceProperty<SequenceTable, Heading> header(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.header);}
        InstanceProperty<SequenceTable, SequenceTable.RowsModelSupport> selectionSupport(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.selectionSupport);}
        InstanceProperty<SequenceTable, SequenceArea> sequenceArea(){return new InstanceProperty<>(source,PackageProperties.sequenceTable.sequenceArea);}
      }
      
       InstanceProperties instance(SequenceTable instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
