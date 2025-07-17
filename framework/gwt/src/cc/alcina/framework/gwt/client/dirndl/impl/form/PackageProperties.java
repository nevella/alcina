package cc.alcina.framework.gwt.client.dirndl.impl.form;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import com.totsp.gwittir.client.ui.table.Field;
import java.lang.Class;
import java.lang.String;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _FmsTable_FmsTreeTableColumn fmsTable_fmsTreeTableColumn = new _FmsTable_FmsTreeTableColumn();
    
    public static class _FmsTable_FmsTreeTableColumn implements TypedProperty.Container {
      public TypedProperty<FmsTable.FmsTreeTableColumn, String> caption = new TypedProperty<>(FmsTable.FmsTreeTableColumn.class, "caption");
      public TypedProperty<FmsTable.FmsTreeTableColumn, TableModel.TableColumn.ColumnFilter> columnFilter = new TypedProperty<>(FmsTable.FmsTreeTableColumn.class, "columnFilter");
      public TypedProperty<FmsTable.FmsTreeTableColumn, Field> field = new TypedProperty<>(FmsTable.FmsTreeTableColumn.class, "field");
      public TypedProperty<FmsTable.FmsTreeTableColumn, TableModel.SortDirection> sortDirection = new TypedProperty<>(FmsTable.FmsTreeTableColumn.class, "sortDirection");
      public TypedProperty<FmsTable.FmsTreeTableColumn, String> title = new TypedProperty<>(FmsTable.FmsTreeTableColumn.class, "title");
      public TypedProperty<FmsTable.FmsTreeTableColumn, Class> valueClass = new TypedProperty<>(FmsTable.FmsTreeTableColumn.class, "valueClass");
      public static class InstanceProperties extends InstanceProperty.Container<FmsTable.FmsTreeTableColumn> {
        public  InstanceProperties(FmsTable.FmsTreeTableColumn source){super(source);}
        public InstanceProperty<FmsTable.FmsTreeTableColumn, String> caption(){return new InstanceProperty<>(source,PackageProperties.fmsTable_fmsTreeTableColumn.caption);}
        public InstanceProperty<FmsTable.FmsTreeTableColumn, TableModel.TableColumn.ColumnFilter> columnFilter(){return new InstanceProperty<>(source,PackageProperties.fmsTable_fmsTreeTableColumn.columnFilter);}
        public InstanceProperty<FmsTable.FmsTreeTableColumn, Field> field(){return new InstanceProperty<>(source,PackageProperties.fmsTable_fmsTreeTableColumn.field);}
        public InstanceProperty<FmsTable.FmsTreeTableColumn, TableModel.SortDirection> sortDirection(){return new InstanceProperty<>(source,PackageProperties.fmsTable_fmsTreeTableColumn.sortDirection);}
        public InstanceProperty<FmsTable.FmsTreeTableColumn, String> title(){return new InstanceProperty<>(source,PackageProperties.fmsTable_fmsTreeTableColumn.title);}
        public InstanceProperty<FmsTable.FmsTreeTableColumn, Class> valueClass(){return new InstanceProperty<>(source,PackageProperties.fmsTable_fmsTreeTableColumn.valueClass);}
      }
      
      public  InstanceProperties instance(FmsTable.FmsTreeTableColumn instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
