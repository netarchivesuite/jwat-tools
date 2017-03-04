package org.jwat.tools.tasks.compress;

import java.io.File;
import java.util.Map;

public class CompressResult {

	protected File srcFile;

	protected File dstFile;

	protected File idxFile;

	protected Map<String, Long> schemesMap;

	protected boolean bCompleted = false;

	protected boolean bVerified = false;

	protected byte[] md5DigestBytesOrg;

	protected byte[] md5DigestBytesVerify;

	protected byte[] md5compDigestBytesVerify;

	protected Throwable t;

}
