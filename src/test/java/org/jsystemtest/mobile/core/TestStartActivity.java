package org.jsystemtest.mobile.core;

import org.jsystemtest.mobile.core.device.AbstractAndroidDevice;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestStartActivity {

	private static final String DEVICE_SERIAL = "4df1fdfc1e1b4ffd";
	private AbstractAndroidDevice device;
	private String packageName = "com.example.android.notepad";
	private String activityName = "NotesList";

	@Before
	public void before() throws Exception {
		device = AdbController.getInstance().waitForDeviceToConnect(DEVICE_SERIAL);
	}

	@Test
	@Ignore
	public void testLaunch() throws AdbControllerException {
		device.startActivity(packageName, activityName);
	}

}
