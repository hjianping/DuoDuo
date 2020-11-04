package org.qiunet.profile.printer;

/***
 *
 *
 * @author qiunet
 * 2020-11-04 16:08
 */
class TextTableColumnValue {

	private String value;

	private int length;

	TextTableColumnValue(String value, int length) {
		this.value = value;
		this.length = length;
	}

	public String getValue() {
		return value;
	}

	public int getLength() {
		return length;
	}
}
