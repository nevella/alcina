package cc.alcina.framework.gwt.client.dirndl.activity;

import cc.alcina.framework.common.client.domain.search.ModelSearchResults;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;
import java.lang.Boolean;
import java.lang.Class;
import java.util.Map;
import java.util.stream.Stream;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _DirectedActivity directedActivity = new _DirectedActivity();
    public static _DirectedBindableSearchActivity directedBindableSearchActivity = new _DirectedBindableSearchActivity();
    public static _DirectedBindableSearchActivity_DirectedBindableSearchActivity_Entity directedBindableSearchActivity_directedBindableSearchActivity_entity = new _DirectedBindableSearchActivity_DirectedBindableSearchActivity_Entity();
    public static _DirectedCategoriesActivity directedCategoriesActivity = new _DirectedCategoriesActivity();
    public static _DirectedCategoryActivity directedCategoryActivity = new _DirectedCategoryActivity();
    public static _DirectedEntityActivity directedEntityActivity = new _DirectedEntityActivity();
    public static _RootArea rootArea = new _RootArea();
    static _RootArea_ChannelOverlay rootArea_channelOverlay = new _RootArea_ChannelOverlay();
    
    public static class _DirectedActivity implements TypedProperty.Container {
      public TypedProperty<DirectedActivity, BasePlace> place = new TypedProperty<>(DirectedActivity.class, "place");
      public static class InstanceProperties extends InstanceProperty.Container<DirectedActivity> {
        public  InstanceProperties(DirectedActivity source){super(source);}
        public InstanceProperty<DirectedActivity, BasePlace> place(){return new InstanceProperty<>(source,PackageProperties.directedActivity.place);}
      }
      
      public  InstanceProperties instance(DirectedActivity instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _DirectedBindableSearchActivity implements TypedProperty.Container {
      public TypedProperty<DirectedBindableSearchActivity, Stream> actions = new TypedProperty<>(DirectedBindableSearchActivity.class, "actions");
      public TypedProperty<DirectedBindableSearchActivity, BindablePlace> place = new TypedProperty<>(DirectedBindableSearchActivity.class, "place");
      public TypedProperty<DirectedBindableSearchActivity, ModelSearchResults> searchResults = new TypedProperty<>(DirectedBindableSearchActivity.class, "searchResults");
      public static class InstanceProperties extends InstanceProperty.Container<DirectedBindableSearchActivity> {
        public  InstanceProperties(DirectedBindableSearchActivity source){super(source);}
        public InstanceProperty<DirectedBindableSearchActivity, Stream> actions(){return new InstanceProperty<>(source,PackageProperties.directedBindableSearchActivity.actions);}
        public InstanceProperty<DirectedBindableSearchActivity, BindablePlace> place(){return new InstanceProperty<>(source,PackageProperties.directedBindableSearchActivity.place);}
        public InstanceProperty<DirectedBindableSearchActivity, ModelSearchResults> searchResults(){return new InstanceProperty<>(source,PackageProperties.directedBindableSearchActivity.searchResults);}
      }
      
      public  InstanceProperties instance(DirectedBindableSearchActivity instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _DirectedBindableSearchActivity_DirectedBindableSearchActivity_Entity implements TypedProperty.Container {
      public TypedProperty<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity, Stream> actions = new TypedProperty<>(DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity.class, "actions");
      public TypedProperty<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity, EntityPlace> place = new TypedProperty<>(DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity.class, "place");
      public TypedProperty<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity, ModelSearchResults> searchResults = new TypedProperty<>(DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity.class, "searchResults");
      public static class InstanceProperties extends InstanceProperty.Container<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity> {
        public  InstanceProperties(DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity source){super(source);}
        public InstanceProperty<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity, Stream> actions(){return new InstanceProperty<>(source,PackageProperties.directedBindableSearchActivity_directedBindableSearchActivity_entity.actions);}
        public InstanceProperty<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity, EntityPlace> place(){return new InstanceProperty<>(source,PackageProperties.directedBindableSearchActivity_directedBindableSearchActivity_entity.place);}
        public InstanceProperty<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity, ModelSearchResults> searchResults(){return new InstanceProperty<>(source,PackageProperties.directedBindableSearchActivity_directedBindableSearchActivity_entity.searchResults);}
      }
      
      public  InstanceProperties instance(DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _DirectedCategoriesActivity implements TypedProperty.Container {
      public TypedProperty<DirectedCategoriesActivity, CategoryNamePlace> place = new TypedProperty<>(DirectedCategoriesActivity.class, "place");
      public static class InstanceProperties extends InstanceProperty.Container<DirectedCategoriesActivity> {
        public  InstanceProperties(DirectedCategoriesActivity source){super(source);}
        public InstanceProperty<DirectedCategoriesActivity, CategoryNamePlace> place(){return new InstanceProperty<>(source,PackageProperties.directedCategoriesActivity.place);}
      }
      
      public  InstanceProperties instance(DirectedCategoriesActivity instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _DirectedCategoryActivity implements TypedProperty.Container {
      public TypedProperty<DirectedCategoryActivity, CategoryNamePlace> place = new TypedProperty<>(DirectedCategoryActivity.class, "place");
      public static class InstanceProperties extends InstanceProperty.Container<DirectedCategoryActivity> {
        public  InstanceProperties(DirectedCategoryActivity source){super(source);}
        public InstanceProperty<DirectedCategoryActivity, CategoryNamePlace> place(){return new InstanceProperty<>(source,PackageProperties.directedCategoryActivity.place);}
      }
      
      public  InstanceProperties instance(DirectedCategoryActivity instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _DirectedEntityActivity implements TypedProperty.Container {
      public TypedProperty<DirectedEntityActivity, Entity> entity = new TypedProperty<>(DirectedEntityActivity.class, "entity");
      public TypedProperty<DirectedEntityActivity, Boolean> entityNotFound = new TypedProperty<>(DirectedEntityActivity.class, "entityNotFound");
      public TypedProperty<DirectedEntityActivity, EntityPlace> place = new TypedProperty<>(DirectedEntityActivity.class, "place");
      public static class InstanceProperties extends InstanceProperty.Container<DirectedEntityActivity> {
        public  InstanceProperties(DirectedEntityActivity source){super(source);}
        public InstanceProperty<DirectedEntityActivity, Entity> entity(){return new InstanceProperty<>(source,PackageProperties.directedEntityActivity.entity);}
        public InstanceProperty<DirectedEntityActivity, Boolean> entityNotFound(){return new InstanceProperty<>(source,PackageProperties.directedEntityActivity.entityNotFound);}
        public InstanceProperty<DirectedEntityActivity, EntityPlace> place(){return new InstanceProperty<>(source,PackageProperties.directedEntityActivity.place);}
      }
      
      public  InstanceProperties instance(DirectedEntityActivity instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _RootArea implements TypedProperty.Container {
      public TypedProperty<RootArea, Map> channelOverlays = new TypedProperty<>(RootArea.class, "channelOverlays");
      public TypedProperty<RootArea, DirectedActivity> mainActivity = new TypedProperty<>(RootArea.class, "mainActivity");
      public static class InstanceProperties extends InstanceProperty.Container<RootArea> {
        public  InstanceProperties(RootArea source){super(source);}
        public InstanceProperty<RootArea, Map> channelOverlays(){return new InstanceProperty<>(source,PackageProperties.rootArea.channelOverlays);}
        public InstanceProperty<RootArea, DirectedActivity> mainActivity(){return new InstanceProperty<>(source,PackageProperties.rootArea.mainActivity);}
      }
      
      public  InstanceProperties instance(RootArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RootArea_ChannelOverlay implements TypedProperty.Container {
      TypedProperty<RootArea.ChannelOverlay, DirectedActivity> activity = new TypedProperty<>(RootArea.ChannelOverlay.class, "activity");
      TypedProperty<RootArea.ChannelOverlay, Class> channel = new TypedProperty<>(RootArea.ChannelOverlay.class, "channel");
      TypedProperty<RootArea.ChannelOverlay, Overlay> overlay = new TypedProperty<>(RootArea.ChannelOverlay.class, "overlay");
      static class InstanceProperties extends InstanceProperty.Container<RootArea.ChannelOverlay> {
         InstanceProperties(RootArea.ChannelOverlay source){super(source);}
        InstanceProperty<RootArea.ChannelOverlay, DirectedActivity> activity(){return new InstanceProperty<>(source,PackageProperties.rootArea_channelOverlay.activity);}
        InstanceProperty<RootArea.ChannelOverlay, Class> channel(){return new InstanceProperty<>(source,PackageProperties.rootArea_channelOverlay.channel);}
        InstanceProperty<RootArea.ChannelOverlay, Overlay> overlay(){return new InstanceProperty<>(source,PackageProperties.rootArea_channelOverlay.overlay);}
      }
      
       InstanceProperties instance(RootArea.ChannelOverlay instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
