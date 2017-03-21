package org.jwat.tools.tasks.test;

import java.util.LinkedList;
import java.util.List;

import org.jwat.common.UriProfile;
import org.jwat.tools.core.ValidatorPlugin;

public class TestOptions {

	public boolean bQueueFirst = false;

	/** Threads to use in thread pool. */
	public int threads = 1;

	public boolean bShowErrors = false;

	public boolean bValidateDigest = true;

	public UriProfile uriProfile = UriProfile.RFC3986;

	public List<ValidatorPlugin> validatorPlugins = new LinkedList<ValidatorPlugin>();

	public boolean bBad = false;

	public Long after = 0L;

	public List<String> filesList;

}
