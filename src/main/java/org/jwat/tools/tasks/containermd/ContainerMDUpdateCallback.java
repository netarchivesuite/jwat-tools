package org.jwat.tools.tasks.containermd;

public interface ContainerMDUpdateCallback {

	public void update(ContainerMDResult result, long consumed);

	public void finalUpdate(ContainerMDResult result, long consumed);

}
