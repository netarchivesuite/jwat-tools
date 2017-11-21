package org.jwat.tools.tasks.extract;

import java.util.List;

public class ExtractOptions {

	public boolean bQueueFirst = false;

	public int threads = 1;

	public boolean bValidateDigest = true;

	public int recordHeaderMaxSize = 8192;

    public int payloadHeaderMaxSize = 32768;

	public String targetUri;

	public List<String> filesList;

}
