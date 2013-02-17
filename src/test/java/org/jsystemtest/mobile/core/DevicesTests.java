package org.jsystemtest.mobile.core;

import junit.framework.Assert;

import org.jsystemtest.mobile.core.device.USBDevice;
import org.junit.Before;
import org.junit.Test;

public class DevicesTests {
	
	private final static String SERIAL= "emulator-5554";
	private AdbController adbController;
	
	@Before
	public void setUp() throws Exception{
		adbController = AdbController.getInstance();
	}
	
	@Test(timeout = 6000)
	public void testConnectionToDevice() throws ConnectionException{
		USBDevice device = adbController.waitForDeviceToConnect(SERIAL);
		Assert.assertEquals(SERIAL, device.getSerialNumber());
	}

	@Test(expected = org.jsystemtest.mobile.core.ConnectionException.class, timeout = 5500)
	public void testConnectionExcpetion() throws ConnectionException{
		adbController.waitForDeviceToConnect("NON EXIST SERIAL");
	}
	
	
	
}
