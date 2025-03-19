package cc.alcina.framework.servlet.component.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMatches;
import cc.alcina.framework.common.client.util.StringMatches.PartialSubstring;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestion;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestionEntry;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.AnswerImpl.Invocation;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands.CommandNode;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands.MatchStyle;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorRequest;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.servlet.component.entity.EntityBrowser.Ui;
import cc.alcina.framework.servlet.component.entity.EntityTypesLayer.TypeSelection;
import cc.alcina.framework.servlet.component.entity.RootLayer.DomainGraphSelection;
import cc.alcina.framework.servlet.component.entity.property.PropertyFilterParser;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.TraversalAnswerSupplier;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;

class EntityAnswers extends TraversalAnswerSupplier {
	public EntityAnswers(int fromLayer) {
		super(fromLayer);
	}

	static AppSuggestion createSuggestion(CommandNode node) {
		AppSuggestionEntry suggestion = new AppSuggestionEntry();
		suggestion.modelEvent = (Class<? extends ModelEvent>) node.eventClass;
		suggestion.match = node.toPath();
		suggestion.secondary = node.command.description();
		return suggestion;
	}

	@Override
	public void begin(Invocation invocation) {
		List<AppSuggestion> suggestions = new InvocationHandler(invocation,
				Ui.place(), this.fromLayer).handle();
		AppSuggestorRequest request = new AppSuggestorRequest();
		String query = invocation.ask.getValue();
		request.setQuery(query);
		request.commandContexts.add(TraversalBrowser.CommandContext.class);
		processResults(invocation, suggestions);
	}

	static class InvocationHandler {
		AppSuggestorRequest request = new AppSuggestorRequest();

		Invocation invocation;

		String query;

		List<AppSuggestion> suggestions;

		TraversalPlace fromPlace;

		int fromLayer;

		boolean withCommandSuggestions;

		InvocationHandler(Invocation invocation, TraversalPlace fromPlace,
				int fromLayer) {
			this.invocation = invocation;
			this.fromLayer = fromLayer;
			if (fromLayer == -1) {
				this.withCommandSuggestions = true;
				this.fromPlace = fromPlace;
			} else {
				this.fromPlace = fromPlace.truncateTo(fromLayer);
			}
			query = invocation.ask.getValue();
			request.setQuery(query);
			request.commandContexts.add(TraversalBrowser.CommandContext.class);
			suggestions = new ArrayList<>();
		}

		void proposePlaceContextSuggestions() {
			Selection placeContextSelection = Ui.traversal().getRootSelection();
			TraversalPlace toPlace = fromPlace.copy();
			SelectionPath firstSelectionPath = toPlace.firstSelectionPath();
			if (firstSelectionPath != null) {
				if (fromLayer > 0) {
					// no - we want to change the toPlace
					// firstSelectionPath = firstSelectionPath.copy();
					firstSelectionPath.truncateTo(fromLayer - 1);
				}
				Selection pathSelection = firstSelectionPath.selection();
				if (pathSelection != null) {
					placeContextSelection = pathSelection;
				}
			}
			TypeSuggestor typeSuggestor = Registry.impl(TypeSuggestor.class,
					placeContextSelection.getClass());
			typeSuggestor.handler = this;
			typeSuggestor.query = query;
			typeSuggestor.parts = query.split(" ");
			typeSuggestor.fromPlace = toPlace;
			typeSuggestor.propose(placeContextSelection);
		}

		List<AppSuggestion> handle() {
			if (withCommandSuggestions) {
				proposeSetSuggestions(query, suggestions);
				proposeCommandSuggestions();
			}
			proposePlaceContextSuggestions();
			return suggestions;
		}

		void proposeCommandSuggestions() {
			List<CommandNode> commandNodes = AppSuggestorCommands.get()
					.getCommandNodes(request, MatchStyle.any_substring);
			commandNodes.stream().map(EntityAnswers::createSuggestion)
					.forEach(suggestions::add);
		}
	}

	@Registration.NonGenericSubtypes(TypeSuggestor.class)
	static abstract class TypeSuggestor<S extends Selection>
			implements Registration.AllSubtypes {
		InvocationHandler handler;

		String[] parts;

		TraversalPlace fromPlace;

		String query;

		abstract void propose(S selection);

		<T extends Selection> List<T> matches(List<T> selections, String part) {
			return new StringMatches.PartialSubstring<T>()
					.match(selections, Selection::toFilterString, part).stream()
					.map(PartialSubstring.Match::getValue)
					.collect(Collectors.toList());
		}

		void addSuggestion(String match, List<Selection> selections,
				StandardLayerAttributes.Filter filter) {
			AppSuggestionEntry suggestion = new AppSuggestionEntry();
			suggestion.match = match;
			SelectionTraversal traversal = Ui.traversal();
			int idx = fromPlace.getLayerCount();
			Preconditions.checkArgument(selections.isEmpty() || filter == null);
			TraversalPlace place = fromPlace.copy();
			place = place.appendSelections(selections);
			if (selections.size() > 0) {
				// all appended selections should go at the start of layers
				for (Selection sel : selections) {
					if (idx == 0) {
						idx++;
						continue;
					}
					place.ensureAttributes(idx++)
							.put(new StandardLayerAttributes.SortSelectedFirst(
									true));
				}
			} else {
				place.ensureAttributes(idx).put(filter);
			}
			suggestion.url = place.toTokenString();
			handler.suggestions.add(suggestion);
		}

		static class _Property extends TypeSuggestor<PropertySelection> {
			@Override
			void propose(PropertySelection selection) {
				Class<? extends Entity> entityType = null;
				Property property = selection.get();
				if (property.getType() == Set.class) {
					List<Class> bounds = property.getTypeBounds().bounds;
					if (bounds.size() == 1) {
						Class bound = bounds.get(0);
						if (Reflections.isAssignableFrom(Entity.class, bound)) {
							entityType = bound;
						}
					}
				}
				if (entityType != null) {
					new PropertyFilterParser().proposeFilters(entityType, query)
							.stream()
							.forEach(filter -> addSuggestion(filter.toString(),
									List.of(), filter));
				}
			}
		}

		static class _Entity extends TypeSuggestor<EntitySelection> {
			@Override
			void propose(EntitySelection selection) {
				if (parts.length < 1 || parts.length > 1) {
					return;
				}
				List<PropertySelection> candidates = Reflections
						.at(selection.entityType()).properties().stream()
						.sorted(Comparator.comparing(Property::getName))
						.map(prop -> new PropertySelection(selection, prop))
						.collect(Collectors.toList());
				matches(candidates, parts[0]).forEach(prop -> {
					String valueStr = Ax.ntrim(prop.get().get(selection.get()),
							30);
					String suggestionText = Ax.format("%s - %s",
							prop.get().getName(), valueStr);
					addSuggestion(suggestionText, List.of(prop), null);
				});
			}
		}

		static class _Type extends TypeSuggestor<TypeSelection> {
			@Override
			void propose(TypeSelection selection) {
				new PropertyFilterParser()
						.proposeFilters(selection.get(), query).stream()
						.forEach(filter -> addSuggestion(filter.toString(),
								List.of(), filter));
			}
		}

		static class _Domain extends TypeSuggestor<DomainGraphSelection> {
			long id = 0;

			@Override
			public void propose(DomainGraphSelection selection) {
				if (parts.length < 1 || parts.length > 2) {
					return;
				}
				if (parts.length == 2) {
					if (parts[1].matches("\\d+")) {
						id = Long.parseLong(parts[1]);
					} else {
						return;
					}
				}
				List<TypeSelection> candidates = Ui.traversal()
						.getSelections(TypeSelection.class);
				matches(candidates, parts[0]).forEach(sel -> {
					List<Selection> selections = new ArrayList<>();
					selections.add(sel.parentSelection());
					selections.add(sel);
					String match = sel.get().getSimpleName();
					if (id != 0) {
						Entity entity = Domain.find(sel.get(), id);
						if (entity != null) {
							selections.add(new EntitySelection(sel, entity));
							match = Ax.format("%s - %s - %s",
									sel.get().getSimpleName(), id,
									entity.toString());
						} else {
							match = Ax.format("%s - %s - [no match]",
									sel.get().getSimpleName(), id);
						}
					}
					addSuggestion(match, selections, null);
				});
			}
		}
	}
}