package org.jwat.tools.tasks.digest;

import java.util.List;

import org.jwat.tools.JWATTools;

public class DigestOptions {

	public List<String> filesList;

	public boolean bBase16;

	public boolean bBase32;

	public boolean bBase64;

	public DigestAlgo[] digestAlgos;

	@Override
	public String toString() {
		String lineSeparator = System.lineSeparator();
		//int idx;
		//int len;
		StringBuilder sb = new StringBuilder();
		sb.append("FileTools v");
		sb.append(JWATTools.getVersionString());
		if (filesList.size() > 0) {
			sb.append(lineSeparator);
			/*
			if (paths != null) {
				idx = 0;
				len = paths.length;
				sb.append("       Path: " + paths[idx++].getPath());
				sb.append(lineSeparator);
				while (idx < len) {
					sb.append("             " + paths[idx++].getPath());
					sb.append(lineSeparator);
				}
			}
			*/
			sb.append("     base16: " + bBase16);
			sb.append(lineSeparator);
			sb.append("     base32: " + bBase32);
			sb.append(lineSeparator);
			sb.append("     base64: " + bBase64);
			//sb.append(lineSeparator);
			//sb.append("     base64: " + mdAlgo);
		}
		return sb.toString();
	}

}
