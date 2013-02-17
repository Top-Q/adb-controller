package org.jsystemtest.mobile.core;

import org.jsystemtest.mobile.core.device.AbstractAndroidDevice;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestGetSerial {
	private AbstractAndroidDevice device;

	@Before
	public void before() throws Exception {
		device = AdbController.getInstance().waitForDevicesToConnect(1)[0];
	}

	@Ignore
	@Test
	public void testGetSerial() {
		System.out.println(device.getSerialNumber());
	}
}
