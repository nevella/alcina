package cc.alcina.framework.gwt.client.dirndl.layout.doc.example.entity;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;

/**
 * Not 'Product'! This is a very specific business
 */
public class Pastry extends VersionableEntity {
	public static Pastry create() {
		return Domain.create(Pastry.class);
	}

	public static Pastry byId(long id) {
		return Domain.find(Pastry.class, id);
	}

	private String name;

	private double unitPrice;

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		set("name", this.name, name, () -> this.name = name);
	}

	public double getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(double unitPrice) {
		set("unitPrice", this.unitPrice, unitPrice,
				() -> this.unitPrice = unitPrice);
	}
}
