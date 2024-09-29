package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.sequence.DetailArea;
import cc.alcina.framework.servlet.component.sequence.Header;
import cc.alcina.framework.servlet.component.sequence.HighlightModel;
import cc.alcina.framework.servlet.component.sequence.Page;
import cc.alcina.framework.servlet.component.sequence.Sequence;
import cc.alcina.framework.servlet.component.sequence.SequenceArea;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser;
import cc.alcina.framework.servlet.component.sequence.SequenceSettings;
import cc.alcina.framework.servlet.component.sequence.place.SequencePlace;
import com.google.gwt.dom.client.StyleElement;
import java.lang.Class;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _Dotburger_Menu dotburger_menu = new _Dotburger_Menu();
    static _Header header = new _Header();
    static _Header_Left header_left = new _Header_Left();
    static _Page page = new _Page();
    static _SequenceBrowser_Ui sequenceBrowser_ui = new _SequenceBrowser_Ui();
    public static _SequenceSettings sequenceSettings = new _SequenceSettings();
    
    static class _Dotburger_Menu implements TypedProperty.Container {
      TypedProperty<Dotburger.Menu, SequenceSettings.PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(Dotburger.Menu.class, "propertyDisplayMode");
      TypedProperty<Dotburger.Menu, Heading> section2 = new TypedProperty<>(Dotburger.Menu.class, "section2");
    }
    
    static class _Header implements TypedProperty.Container {
      TypedProperty<Header, Header.Left> left = new TypedProperty<>(Header.class, "left");
      TypedProperty<Header, Header.Mid> mid = new TypedProperty<>(Header.class, "mid");
      TypedProperty<Header, Header.Right> right = new TypedProperty<>(Header.class, "right");
    }
    
    static class _Header_Left implements TypedProperty.Container {
      TypedProperty<Header.Left, String> filter = new TypedProperty<>(Header.Left.class, "filter");
      TypedProperty<Header.Left, String> highlight = new TypedProperty<>(Header.Left.class, "highlight");
      TypedProperty<Header.Left, String> name = new TypedProperty<>(Header.Left.class, "name");
    }
    
    static class _Page implements TypedProperty.Container {
      TypedProperty<Page, DetailArea> detailArea = new TypedProperty<>(Page.class, "detailArea");
      TypedProperty<Page, List> filteredSequenceElements = new TypedProperty<>(Page.class, "filteredSequenceElements");
      TypedProperty<Page, Header> header = new TypedProperty<>(Page.class, "header");
      TypedProperty<Page, HighlightModel> highlightModel = new TypedProperty<>(Page.class, "highlightModel");
      TypedProperty<Page, SequencePlace> lastFilterTestPlace = new TypedProperty<>(Page.class, "lastFilterTestPlace");
      TypedProperty<Page, SequencePlace> lastHighlightTestPlace = new TypedProperty<>(Page.class, "lastHighlightTestPlace");
      TypedProperty<Page, SequencePlace> lastSelectedIndexChangePlace = new TypedProperty<>(Page.class, "lastSelectedIndexChangePlace");
      TypedProperty<Page, Sequence> sequence = new TypedProperty<>(Page.class, "sequence");
      TypedProperty<Page, SequenceArea> sequenceArea = new TypedProperty<>(Page.class, "sequenceArea");
      TypedProperty<Page, StyleElement> styleElement = new TypedProperty<>(Page.class, "styleElement");
      TypedProperty<Page, SequenceBrowser.Ui> ui = new TypedProperty<>(Page.class, "ui");
    }
    
    static class _SequenceBrowser_Ui implements TypedProperty.Container {
      TypedProperty<SequenceBrowser.Ui, Class> appCommandContext = new TypedProperty<>(SequenceBrowser.Ui.class, "appCommandContext");
      TypedProperty<SequenceBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(SequenceBrowser.Ui.class, "commandContextProvider");
      TypedProperty<SequenceBrowser.Ui, String> mainCaption = new TypedProperty<>(SequenceBrowser.Ui.class, "mainCaption");
      TypedProperty<SequenceBrowser.Ui, Page> page = new TypedProperty<>(SequenceBrowser.Ui.class, "page");
      TypedProperty<SequenceBrowser.Ui, SequencePlace> place = new TypedProperty<>(SequenceBrowser.Ui.class, "place");
      TypedProperty<SequenceBrowser.Ui, SequenceSettings> settings = new TypedProperty<>(SequenceBrowser.Ui.class, "settings");
    }
    
    public static class _SequenceSettings implements TypedProperty.Container {
      public TypedProperty<SequenceSettings, SequenceSettings.PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(SequenceSettings.class, "propertyDisplayMode");
      public TypedProperty<SequenceSettings, String> sequenceKey = new TypedProperty<>(SequenceSettings.class, "sequenceKey");
    }
    
//@formatter:on
}
