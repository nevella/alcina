package cc.alcina.framework.servlet.component.gallery.model.multiplesuggestions;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.gallery.model.multiplesuggestions.MultipleSuggestionsGalleryArea;
import cc.alcina.framework.servlet.component.gallery.model.multiplesuggestions.MultipleSuggestionsGalleryPlace;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _MultipleSuggestionsGalleryArea multipleSuggestionsGalleryArea = new _MultipleSuggestionsGalleryArea();
    public static _MultipleSuggestionsGalleryPlace_Definition multipleSuggestionsGalleryPlace_definition = new _MultipleSuggestionsGalleryPlace_Definition();
    
    static class _MultipleSuggestionsGalleryArea implements TypedProperty.Container {
      TypedProperty<MultipleSuggestionsGalleryArea, MultipleSuggestionsGalleryPlace.Definition> definition = new TypedProperty<>(MultipleSuggestionsGalleryArea.class, "definition");
      TypedProperty<MultipleSuggestionsGalleryArea, MultipleSuggestionsGalleryArea.InfoModel> model = new TypedProperty<>(MultipleSuggestionsGalleryArea.class, "model");
      TypedProperty<MultipleSuggestionsGalleryArea, GalleryPlace> place = new TypedProperty<>(MultipleSuggestionsGalleryArea.class, "place");
      TypedProperty<MultipleSuggestionsGalleryArea, String> self = new TypedProperty<>(MultipleSuggestionsGalleryArea.class, "self");
    }
    
    public static class _MultipleSuggestionsGalleryPlace_Definition implements TypedProperty.Container {
      public TypedProperty<MultipleSuggestionsGalleryPlace.Definition, List> users = new TypedProperty<>(MultipleSuggestionsGalleryPlace.Definition.class, "users");
    }
    
//@formatter:on
}
