package org.jwat.tools.core;

import java.util.ArrayList;
import java.util.List;

public class WildcardMatcher {

	static final int WCT_STRING = 0;
	static final int WCT_HASH = 1;
	static final int WCT_ASTERIX = 2;

	static class WildcardPart {
		int type;
		int num;
		String str;
	}

	protected List<WildcardPart> parts = new ArrayList<WildcardPart>();

	public WildcardMatcher(String patternStr) {
		int idx = 0;
		int state = WCT_STRING;
		StringBuffer tmpStr = new StringBuffer();
		char c;
		WildcardPart part = null;
		while (idx < patternStr.length()) {
			c = patternStr.charAt(idx++);
			switch (state) {
			case WCT_STRING:
				switch (c) {
				case '*':
					if (tmpStr.length() > 0) {
						part = new WildcardPart();
						part.type = WCT_STRING;
						part.str = tmpStr.toString();
						parts.add(part);
						tmpStr.setLength(0);
					}
					part = new WildcardPart();
					part.type = WCT_ASTERIX;
					parts.add(part);
					state = WCT_ASTERIX;
					break;
				case '#':
					if (tmpStr.length() > 0) {
						part = new WildcardPart();
						part.type = WCT_STRING;
						part.str = tmpStr.toString();
						parts.add(part);
						tmpStr.setLength(0);
					}
					part = new WildcardPart();
					part.type = WCT_HASH;
					part.num = 1;
					parts.add(part);
					state = WCT_HASH;
					break;
				default:
					tmpStr.append(c);
				}
				break;
			case WCT_HASH:
				switch (c) {
				case '*':
					part = new WildcardPart();
					part.type = WCT_ASTERIX;
					parts.add(part);
					state = WCT_ASTERIX;
					break;
				case '#':
					++part.num;
					break;
				default:
					tmpStr.append(c);
					state = WCT_STRING;
					break;
				}
				break;
			case WCT_ASTERIX:
				switch (c) {
				case '*':
					break;
				case '#':
					part = new WildcardPart();
					part.type = WCT_HASH;
					part.num = 1;
					parts.add(part);
					state = WCT_HASH;
					break;
				default:
					tmpStr.append(c);
					state = WCT_STRING;
					break;
				}
				break;
			}
		}
		if (tmpStr.length() > 0) {
			part = new WildcardPart();
			part.type = WCT_STRING;
			part.str = tmpStr.toString();
			parts.add(part);
		}
	}

	static class Checkpoint {
		int pIdx = 0;
		int sIdx = 0;
	}

	public boolean match(String inStr) {
		WildcardPart part = null;
		if (parts.size() == 1) {
			part = parts.get(0);
			switch (part.type) {
			case WCT_ASTERIX:
				return true;
			case WCT_HASH:
				return part.num == inStr.length();
			case WCT_STRING:
				return part.str.equals(inStr);
			}
		}
		else {
			int pIdx = 0;
			int sIdx = 0;
			int cIdx;
			int state = 0;
			boolean bLoop = true;
			List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
			Checkpoint checkpoint;
			while (bLoop) {
				if (pIdx < parts.size()) {
					part = parts.get(pIdx);
					switch (state) {
					case 0:
						switch (part.type) {
						case WCT_STRING:
							if (inStr.regionMatches(true, sIdx, part.str, 0, part.str.length())) {
								checkpoint = new Checkpoint();
								checkpoint.pIdx = pIdx;
								checkpoint.sIdx = sIdx;
								checkpoints.add(checkpoint);
								++pIdx;
								sIdx += part.str.length();
							}
							else {
								return false;
							}
							break;
						case WCT_HASH:
							if ((sIdx + part.num) <= inStr.length()) {
								checkpoint = new Checkpoint();
								checkpoint.pIdx = pIdx;
								checkpoint.sIdx = sIdx;
								checkpoints.add(checkpoint);
								++pIdx;
								sIdx += part.num;
							}
							else {
								return false;
							}
							break;
						case WCT_ASTERIX:
							checkpoint = new Checkpoint();
							checkpoint.pIdx = pIdx;
							checkpoint.sIdx = sIdx;
							checkpoints.add(checkpoint);
							++pIdx;
							state = 1;
							break;
						}
						break;
					case 1:
						switch (part.type) {
						case WCT_STRING:
							cIdx = inStr.indexOf(part.str, sIdx);
							if (cIdx != -1) {
								checkpoint = new Checkpoint();
								checkpoint.pIdx = pIdx;
								checkpoint.sIdx = sIdx;
								checkpoints.add(checkpoint);
								++pIdx;
								sIdx = cIdx + part.str.length();
								state = 0;
							}
							else {
								return false;
							}
							break;
						case WCT_HASH:
							if ((sIdx + part.num) <= inStr.length()) {
								checkpoint = new Checkpoint();
								checkpoint.pIdx = pIdx;
								checkpoint.sIdx = sIdx;
								checkpoints.add(checkpoint);
								++pIdx;
								sIdx += part.num;
								state = 0;
							}
							else {
								return false;
							}
							break;
						case WCT_ASTERIX:
							throw new IllegalStateException();
						}
						break;
					}
				}
				if (pIdx == parts.size()) {
					if (sIdx < inStr.length()) {
						boolean bBack = true;
						while (bBack) {
							if (checkpoints.size() > 0) {
								checkpoint = checkpoints.get(pIdx - 1);
								part = parts.get(checkpoint.pIdx);
								if (part.type != WCT_ASTERIX) {
									checkpoints.remove(--pIdx);
								}
								else {
									bBack = false;
								}
							}
							else {
								bBack = false;
							}
						}
						if (pIdx > 0) {
							checkpoint = checkpoints.get(pIdx - 1);
							sIdx = ++checkpoint.sIdx;
							state = 1;
						}
						else {
							return false;
						}
					}
					else {
						bLoop = false;
					}
				}
			}
		}
		return true;
	}

}
