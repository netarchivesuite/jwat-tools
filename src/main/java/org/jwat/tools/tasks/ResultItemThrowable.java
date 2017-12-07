package org.jwat.tools.tasks;

public class ResultItemThrowable {

	public Throwable t;

	public Long startOffset;

	public Long offset;

	public ResultItemThrowable() {
	}

	public ResultItemThrowable(Throwable t, Long startOffset, Long offset) {
		this.t = t;
		this.startOffset = startOffset;
		this.offset = offset;
	}

}
