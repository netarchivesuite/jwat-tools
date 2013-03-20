package org.jwat.tools.core;
import java.util.LinkedList;
import java.util.List;

public class ArrayUtils {

	protected static byte[] CASE_SENSITIVE = new byte[256];
	protected static byte[] CASE_INSENSITIVE = new byte[256];

	public static byte[] SKIP_WHITESPACE = new byte[256];
	public static byte[] SKIP_NONWHITESPACE = new byte[256];

	static {
		for (int i=0; i<256; ++i) {
			CASE_SENSITIVE[i] = (byte)i;
			CASE_INSENSITIVE[i] = (byte)Character.toLowerCase(i);
			SKIP_WHITESPACE[i] = 0;
			SKIP_NONWHITESPACE[i] = 1;
		}
		SKIP_WHITESPACE[' '] = 1;
		SKIP_WHITESPACE['\t'] = 1;
		SKIP_WHITESPACE['\r'] = 1;
		SKIP_WHITESPACE['\n'] = 1;
		SKIP_NONWHITESPACE[' '] = 0;
		SKIP_NONWHITESPACE['\t'] = 0;
		SKIP_NONWHITESPACE['\r'] = 0;
		SKIP_NONWHITESPACE['\n'] = 0;
	}

	protected ArrayUtils() {
    }

	public static int skip(byte[] skip, byte[] arr, int fIdx) {
    	int arrLen = arr.length;
		while (fIdx < arrLen && skip[arr[fIdx] & 255] == 1) {
			++fIdx;
		}
		return fIdx;
	}

	public static boolean startsWith(byte[] subArr, byte[] arr) {
    	boolean bRes = false;
    	int lIdx = subArr.length - 1;
    	if (lIdx < arr.length) {
    		if (subArr[0] == arr[0]) {
    			while (lIdx > 0 && subArr[lIdx] == arr[lIdx]) {
    		    	--lIdx;
    			}
    			bRes = (lIdx == 0);
    		}
    	}
    	return bRes;
    }

    public static boolean startsWithIgnoreCase(byte[] subArr, byte[] arr) {
    	boolean bRes = false;
    	int lIdx = subArr.length - 1;
    	if (lIdx < arr.length) {
    		if (CASE_INSENSITIVE[subArr[0]] == CASE_INSENSITIVE[arr[0]]) {
    			while (lIdx > 0 && CASE_INSENSITIVE[subArr[lIdx]] == CASE_INSENSITIVE[arr[lIdx]]) {
    		    	--lIdx;
    			}
    			bRes = (lIdx == 0);
    		}
    	}
    	return bRes;
    }

	public static boolean equalsAt(byte[] subArr, byte[] arr, int fIdx) {
    	boolean bRes = false;
    	int lIdx = subArr.length - 1;
    	int tIdx = fIdx + lIdx;
    	if (tIdx < arr.length) {
    		if (subArr[0] == arr[fIdx]) {
    			while (lIdx > 0 && subArr[lIdx] == arr[tIdx]) {
    		    	--lIdx;
    		    	--tIdx;
    			}
    			bRes = (lIdx == 0);
    		}
    	}
    	return bRes;
    }

    public static boolean equalsAtIgnoreCase(byte[] subArr, byte[] arr, int fIdx) {
    	boolean bRes = false;
    	int lIdx = subArr.length - 1;
    	int tIdx = fIdx + lIdx;
    	if (tIdx < arr.length) {
    		if (CASE_INSENSITIVE[subArr[0] & 255] == CASE_INSENSITIVE[arr[fIdx] & 255]) {
    			while (lIdx > 0 && CASE_INSENSITIVE[subArr[lIdx] & 255] == CASE_INSENSITIVE[arr[tIdx] & 255]) {
    		    	--lIdx;
    		    	--tIdx;
    			}
    			bRes = (lIdx == 0);
    		}
    	}
    	return bRes;
    }

    public static int indexOf(byte[] subArr, byte[] arr, int fIdx) {
    	int csIdx;
    	int caIdx;
    	int idx = -1;
    	int subArrLast = subArr.length - 1;
    	int arrLen = arr.length;
    	int lIdx = fIdx + subArrLast;
    	if (subArrLast > 0) {
        	while (lIdx < arrLen && idx == -1) {
        		if (subArr[0] == arr[fIdx]) {
        			csIdx = subArrLast;
        			caIdx = lIdx;
        			while (csIdx > 0 && subArr[csIdx] == arr[caIdx]) {
        		    	--csIdx;
        		    	--caIdx;
        			}
        			if (csIdx == 0) {
        				idx = fIdx;
        			}
        		}
    			++fIdx;
    			++lIdx;
        	}
    	}
    	else {
    		while (fIdx < arrLen && idx == -1) {
        		if (subArr[0] == arr[fIdx]) {
    				idx = fIdx;
        		}
    			++fIdx;
    		}
    	}
    	return idx;
    }

    public static int indexOfIgnoreCase(byte[] subArr, byte[] arr, int fIdx) {
    	int csIdx;
    	int caIdx;
    	int idx = -1;
    	int subArrLast = subArr.length - 1;
    	int arrLen = arr.length;
    	int lIdx = fIdx + subArrLast;
    	if (subArrLast > 0) {
        	while (lIdx < arrLen && idx == -1) {
        		if (CASE_INSENSITIVE[subArr[0]] == CASE_INSENSITIVE[arr[fIdx]]) {
        			csIdx = subArrLast;
        			caIdx = lIdx;
        			while (csIdx > 0 && CASE_INSENSITIVE[subArr[csIdx]] == CASE_INSENSITIVE[arr[caIdx]]) {
        		    	--csIdx;
        		    	--caIdx;
        			}
        			if (csIdx == 0) {
        				idx = fIdx;
        			}
        		}
    			++fIdx;
    			++lIdx;
        	}
    	}
    	else {
    		while (fIdx < arrLen && idx == -1) {
        		if (CASE_INSENSITIVE[subArr[0]] == CASE_INSENSITIVE[arr[fIdx]]) {
    				idx = fIdx;
        		}
    			++fIdx;
    		}
    	}
    	return idx;
    }

    public static List<byte[]> split(byte[] arr, byte[] subArr, int fIdx, int tIdx) {
    	List<byte[]> list = new LinkedList<byte[]>();
    	byte[] tmpArr;
    	int csIdx;
    	int caIdx;
    	int subArrLen = subArr.length;
    	int subArrLast = subArrLen - 1;
    	if (arr.length < tIdx) {
    		tIdx = arr.length;
    	}
    	if (fIdx > tIdx) {
    		throw new IllegalArgumentException("Reverse interval!");
    	}
    	int lIdx = fIdx + subArrLast;
    	int pIdx = fIdx;
    	if (subArrLast > 0) {
        	while (lIdx < tIdx) {
        		if (subArr[0] == arr[fIdx]) {
        			csIdx = subArrLast;
        			caIdx = lIdx;
        			while (csIdx > 0 && subArr[csIdx] == arr[caIdx]) {
        		    	--csIdx;
        		    	--caIdx;
        			}
        			if (csIdx == 0) {
            			tmpArr = new byte[fIdx - pIdx];
            			System.arraycopy(arr, pIdx, tmpArr, 0, tmpArr.length);
            			list.add(tmpArr);
            			fIdx += subArrLen;
            			lIdx += subArrLen;
            			pIdx = fIdx;
        			}
            		else {
            			++fIdx;
            			++lIdx;
            		}
        		}
        		else {
        			++fIdx;
        			++lIdx;
        		}
        	}
        	if (pIdx < tIdx) {
    			tmpArr = new byte[tIdx - pIdx];
    			System.arraycopy(arr, pIdx, tmpArr, 0, tmpArr.length);
    			list.add(tmpArr);
        	}
    	}
    	else {
    		while (fIdx < tIdx) {
        		if (subArr[0] == arr[fIdx]) {
        			tmpArr = new byte[fIdx - pIdx];
        			System.arraycopy(arr, pIdx, tmpArr, 0, tmpArr.length);
        			list.add(tmpArr);
        			pIdx = ++fIdx;
        		}
        		else {
        			++fIdx;
        		}
    		}
    		if (pIdx < tIdx) {
    			tmpArr = new byte[tIdx - pIdx];
    			System.arraycopy(arr, pIdx, tmpArr, 0, tmpArr.length);
    			list.add(tmpArr);
    		}
    	}
    	return list;
    }

}
