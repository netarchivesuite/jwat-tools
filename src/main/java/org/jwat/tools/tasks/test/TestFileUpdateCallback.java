package org.jwat.tools.tasks.test;

public interface TestFileUpdateCallback {

	public void update(TestFileResult result, long consumed);

	public void finalUpdate(TestFileResult result, long consumed);

}
