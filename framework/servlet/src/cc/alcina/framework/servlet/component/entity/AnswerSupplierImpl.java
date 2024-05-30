package cc.alcina.framework.servlet.component.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

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
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.AnswerImpl.Invocation;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands.CommandNode;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands.MatchStyle;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorRequest;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.servlet.component.entity.EntityGraphView.Ui;
import cc.alcina.framework.servlet.component.entity.EntityTypeLayer.EntitySelection;
import cc.alcina.framework.servlet.component.entity.EntityTypesLayer.TypeSelection;
import cc.alcina.framework.servlet.component.entity.QueryLayer.PropertySelection;
import cc.alcina.framework.servlet.component.entity.RootLayer.DomainGraphSelection;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes;
import cc.alcina.framework.servlet.component.traversal.TraversalViewContext;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionPath;

class AnswerSupplierImpl implements AppSuggestor.AnswerSupplier {
	static AppSuggestion createSuggestion(CommandNode node) {
		AppSuggestionEntry suggestion = new AppSuggestionEntry();
		suggestion.modelEvent = (Class<? extends ModelEvent>) node.eventClass;
		suggestion.match = node.toPath();
		suggestion.secondary = node.command.description();
		return suggestion;
	}

	protected AsyncCallback runningCallback = null;

	@Override
	public void begin(Invocation invocation) {
		List<AppSuggestion> suggestions = new InvocationHandler(invocation)
				.handle();
		AppSuggestorRequest request = new AppSuggestorRequest();
		String query = invocation.ask.getValue();
		request.setQuery(query);
		request.commandContexts.add(TraversalViewContext.class);
		processResults(invocation, suggestions);
	}

	static class InvocationHandler {
		@Registration.NonGenericSubtypes(TypeSuggestor.class)
		static abstract class TypeSuggestor<S extends Selection>
				implements Registration.AllSubtypes {
			InvocationHandler handler;

			String[] parts;

			private TraversalPlace place;

			abstract void propose(S selection);

			<T extends Selection> List<T> matches(Class<T> clazz, String part) {
				List<T> selections = Ui.cast().peer.traversal
						.getSelections(clazz);
				return new StringMatches.PartialSubstring<T>()
						.match(selections, Selection::toFilterString, part)
						.stream().map(PartialSubstring.Match::getValue)
						.toList();
			}

			void addSuggestion(List<Selection> selections, String match) {
				AppSuggestionEntry suggestion = new AppSuggestionEntry();
				suggestion.match = match;
				place = Ui.cast().place();
				SelectionTraversal traversal = Ui.cast().peer.traversal;
				place = place.appendSelections(selections);
				// all appended selections should go at the start of layers
				int idx = place.getLayerCount();
				for (Selection sel : selections) {
					place.ensureAttributes(idx++).put(
							new StandardLayerAttributes.SortSelectedFirst());
				}
				suggestion.url = place.toTokenString();
				handler.suggestions.add(suggestion);
			}

			static class _Property extends TypeSuggestor<PropertySelection> {
				@Override
				void propose(PropertySelection selection) {
					// will never have children...well, could list the entity
					// children ... filters?
				}
			}

			static class _Entity extends TypeSuggestor<EntitySelection> {
				@Override
				void propose(EntitySelection selection) {
					Reflections.at(selection.entityType()).properties().stream()
							.sorted(Comparator.comparing(Property::getName))
							.forEach(
									prop -> addSuggestion(
											List.of(new PropertySelection(
													selection, prop)),
											prop.getName()));
				}
			}

			static class _Type extends TypeSuggestor<TypeSelection> {
				@Override
				void propose(TypeSelection selection) {
					// will never have children...well, could list the entity
					// children ... filters?
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
					matches(TypeSelection.class, parts[0]).forEach(sel -> {
						List<Selection> selections = new ArrayList<>();
						selections.add(sel.parentSelection());
						selections.add(sel);
						String match = sel.get().getSimpleName();
						if (id != 0) {
							Entity entity = Domain.find(sel.get(), id);
							if (entity != null) {
								selections.add(
										new EntityTypeLayer.EntitySelection(sel,
												entity));
								match = Ax.format("%s - %s - %s",
										sel.get().getSimpleName(), id,
										entity.toString());
							} else {
								match = Ax.format("%s - %s - [no match]",
										sel.get().getSimpleName(), id);
							}
						}
						addSuggestion(selections, match);
					});
				}
			}
		}

		AppSuggestorRequest request = new AppSuggestorRequest();

		Invocation invocation;

		String query;

		List<AppSuggestion> suggestions;

		InvocationHandler(Invocation invocation) {
			this.invocation = invocation;
			query = invocation.ask.getValue();
			request.setQuery(query);
			request.commandContexts.add(TraversalViewContext.class);
			suggestions = new ArrayList<>();
		}

		void proposePlaceContextSuggestions() {
			TraversalPlace place = Ui.get().place();
			SelectionPath firstSelectionPath = place.firstSelectionPath();
			Selection selection = firstSelectionPath == null
					|| firstSelectionPath.selection() == null
							? Ui.cast().peer.traversal.getRootSelection()
							: firstSelectionPath.selection();
			TypeSuggestor typeSuggestor = Registry.impl(TypeSuggestor.class,
					selection.getClass());
			typeSuggestor.handler = this;
			typeSuggestor.parts = query.split(" ");
			typeSuggestor.propose(selection);
		}

		List<AppSuggestion> handle() {
			proposePlaceContextSuggestions();
			proposeCommandSuggestions();
			return suggestions;
		}

		void proposeCommandSuggestions() {
			List<CommandNode> commandNodes = AppSuggestorCommands.get()
					.getCommandNodes(request, MatchStyle.any_substring);
			commandNodes.stream().map(AnswerSupplierImpl::createSuggestion)
					.forEach(suggestions::add);
		}
	}
}