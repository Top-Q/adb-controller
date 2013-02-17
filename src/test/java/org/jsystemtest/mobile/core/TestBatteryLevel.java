package org.jsystemtest.mobile.core;

import org.jsystemtest.mobile.core.device.AbstractAndroidDevice;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestBatteryLevel {
	private AbstractAndroidDevice device;

	@Before
	public void before() throws Exception {
		device = AdbController.getInstance().waitForDevicesToConnect(1)[0];
	}

	@Test
	@Ignore
	public void testBatteryLevel() throws AdbControllerException {
		int batteryLevel = device.getBatteryLevel();
		System.out.println(batteryLevel);
	}

}
