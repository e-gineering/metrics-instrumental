package com.e_gineering.metrics.instrumental;

/**
 * Instrumental Metric types.
 */
public enum MetricType {
	GAUGE("gauge"),
	GAUGE_ABSOLUTE("gauge_absolute"),
	INCREMENT("increment");

	private String protocolKey;

	private MetricType(String protocolKey) {
		this.protocolKey = protocolKey;
	}

	public String getProtocolKey() {
		return this.protocolKey;
	}
}
