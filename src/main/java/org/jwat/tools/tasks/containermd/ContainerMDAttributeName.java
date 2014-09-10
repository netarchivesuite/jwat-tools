package org.jwat.tools.tasks.containermd;

/** Permitted attributes for the containerMD elements */
enum ContainerMDAttributeName {
	NUMBER("number"), NAME("name"), SIZE("size"), TYPE("type"), METHOD(
			"method"), ORDER("order"), PROTOCOL_NAME("protocolName"), PROTOCOL_VERSION(
			"protocolVersion"), GLOBALSIZE("globalSize");

	public final String key;

	private ContainerMDAttributeName(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return this.key;
	}
}