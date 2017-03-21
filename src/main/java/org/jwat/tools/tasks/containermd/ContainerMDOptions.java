package org.jwat.tools.tasks.containermd;

import java.io.File;
import java.util.List;

import org.jwat.common.UriProfile;

public class ContainerMDOptions {

	public boolean bQueueFirst = false;

	public int threads = 1;

	public boolean bQuiet = false;

	public UriProfile uriProfile = UriProfile.RFC3986; // UriProfile.RFC3986_ABS_16BIT_LAX; // UriProfile.RFC3986;

	public File outputDir = new File( System.getProperty("user.dir"));

	public int recordHeaderMaxSize = 1024 * 1024;

	public int payloadHeaderMaxSize = 1024 * 1024;

    public List<String> filesList;

}
