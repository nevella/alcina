package cc.alcina.framework.gwt.client.dirndl.activity;

import cc.alcina.framework.common.client.domain.search.ModelSearchResults;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;
import java.lang.Boolean;
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
    
    public static class _DirectedActivity implements TypedProperty.Container {
      public TypedProperty<DirectedActivity, BasePlace> place = new TypedProperty<>(DirectedActivity.class, "place");
    }
    
    public static class _DirectedBindableSearchActivity implements TypedProperty.Container {
      public TypedProperty<DirectedBindableSearchActivity, Stream> actions = new TypedProperty<>(DirectedBindableSearchActivity.class, "actions");
      public TypedProperty<DirectedBindableSearchActivity, BindablePlace> place = new TypedProperty<>(DirectedBindableSearchActivity.class, "place");
      public TypedProperty<DirectedBindableSearchActivity, ModelSearchResults> searchResults = new TypedProperty<>(DirectedBindableSearchActivity.class, "searchResults");
    }
    
    public static class _DirectedBindableSearchActivity_DirectedBindableSearchActivity_Entity implements TypedProperty.Container {
      public TypedProperty<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity, Stream> actions = new TypedProperty<>(DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity.class, "actions");
      public TypedProperty<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity, EntityPlace> place = new TypedProperty<>(DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity.class, "place");
      public TypedProperty<DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity, ModelSearchResults> searchResults = new TypedProperty<>(DirectedBindableSearchActivity.DirectedBindableSearchActivity_Entity.class, "searchResults");
    }
    
    public static class _DirectedCategoriesActivity implements TypedProperty.Container {
      public TypedProperty<DirectedCategoriesActivity, CategoryNamePlace> place = new TypedProperty<>(DirectedCategoriesActivity.class, "place");
    }
    
    public static class _DirectedCategoryActivity implements TypedProperty.Container {
      public TypedProperty<DirectedCategoryActivity, CategoryNamePlace> place = new TypedProperty<>(DirectedCategoryActivity.class, "place");
    }
    
    public static class _DirectedEntityActivity implements TypedProperty.Container {
      public TypedProperty<DirectedEntityActivity, Entity> entity = new TypedProperty<>(DirectedEntityActivity.class, "entity");
      public TypedProperty<DirectedEntityActivity, Boolean> entityNotFound = new TypedProperty<>(DirectedEntityActivity.class, "entityNotFound");
      public TypedProperty<DirectedEntityActivity, EntityPlace> place = new TypedProperty<>(DirectedEntityActivity.class, "place");
    }
    
//@formatter:on
}
