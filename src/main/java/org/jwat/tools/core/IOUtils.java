package org.jwat.tools.core;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {

	public static void closeIOQuietly(Closeable closable) {
		if (closable != null) {
	        try {
	        	closable.close();
			}
	        catch (IOException e) {
			}
		}
	}

}
