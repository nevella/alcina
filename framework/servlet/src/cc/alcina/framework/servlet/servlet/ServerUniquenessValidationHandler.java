package cc.alcina.framework.servlet.servlet;

import java.util.Objects;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;

@Registration({ ServerValidatorHandler.class, ServerUniquenessValidator.class })
public class ServerUniquenessValidationHandler
		implements ServerValidatorHandler<ServerUniquenessValidator> {
	@Override
	public void handle(ServerUniquenessValidator uniquenessValidator) {
		int ctr = 0;
		String value = Ax.blankToEmpty(uniquenessValidator.getValue());
		uniquenessValidator.setSuggestedValue(value);
		while (true) {
			Property property = Reflections
					.at(uniquenessValidator.getObjectClass())
					.property(uniquenessValidator.getPropertyName());
			String test = value;
			Entity entity = Domain.stream(uniquenessValidator.getObjectClass())
					.filter(e -> !Objects.equals(e.getId(),
							uniquenessValidator.getOkId()))
					.filter(e -> {
						String entityValue = (String) property.get(e);
						return uniquenessValidator.isCaseInsensitive()
								? test.equalsIgnoreCase(entityValue)
								: test.equals(entityValue);
					}).findFirst().orElse(null);
			if (entity == null) {
				if (ctr != 0) {
					uniquenessValidator.setSuggestedValue(value);
					uniquenessValidator.setMessage(
							"Item exists. Suggested value: " + value);
				}
				break;
			}
			// no suggestions, just error
			if (uniquenessValidator.getValueTemplate() == null) {
				uniquenessValidator.setMessage("Item exists");
				break;
			}
			ctr++;
			value = String.format(uniquenessValidator.getValueTemplate(),
					uniquenessValidator.getValue() == null ? ""
							: uniquenessValidator.getValue(),
					ctr);
		}
	}
}
