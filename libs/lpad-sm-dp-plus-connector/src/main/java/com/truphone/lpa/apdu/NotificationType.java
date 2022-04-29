package com.truphone.lpa.apdu;

public enum NotificationType {
	ALL(""), INSTALLED("80"), ENABLED("40"), DISABLED("20"), DELETED("10");

	private final String text;

	NotificationType(final String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

}
