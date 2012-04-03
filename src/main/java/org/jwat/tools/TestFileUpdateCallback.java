package org.jwat.tools;

public interface TestFileUpdateCallback {

	public void update(TestFileResult result, long consumed);

	public void finalUpdate(TestFileResult result, long consumed);

}
