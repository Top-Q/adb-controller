package org.jsystemtest.mobile.core.device;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.jsystemtest.mobile.core.AdbController;
import org.jsystemtest.mobile.core.AdbControllerException;
import org.jsystemtest.mobile.core.ConnectionException;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;

public abstract class AbstractAndroidDevice implements IShellOutputReceiver {
	private final static Logger logger = Logger.getLogger(AbstractAndroidDevice.class);

	public enum IntentType {
		DIRECT, BROADCAST
	}

	protected final AndroidDebugBridge adb;
	protected final IDevice device;

	// Variables that we get from the AdbController
	protected final File adbLocation;
	private String shellResponse;

	public AbstractAndroidDevice(AndroidDebugBridge adb, IDevice device) throws Exception {
		super();
		this.adb = adb;
		this.device = device;

		// Get Data from the AdbController
		try {
			adbLocation = AdbController.getInstance().getAdbLocation();
		} catch (Exception e) {
			throw new Exception("Adb location was not set");
		}

	}

	/**
	 * Set port forwarding for the requested device
	 * 
	 * @param deviceSerial
	 * @param localPort
	 * @param remotePort
	 * @throws Exception
	 */
	public void setPortForwarding(int localPort, int remotePort) throws Exception {
		if (device.getState() == IDevice.DeviceState.ONLINE) {
			device.createForward(localPort, remotePort);
		} else {
			Exception e = new Exception("Unable to perform port forwarding - " + device.getSerialNumber()
					+ " is not online");
			logger.error(e);
			throw e;
		}
	}

	/**
	 * Captures device screenshot
	 * 
	 * 
	 * @param screenshotFile
	 *            - File on which to write the screenshot data. If null is
	 *            specified, the content will be written to temporary file.
	 * @return The screenshot file
	 * @throws Exception
	 */
	public File getScreenshot(File screenshotFile) throws Exception {
		logger.info("Screen Shot " + device.getSerialNumber());
		RawImage ri = device.getScreenshot();
		return display(device.getSerialNumber(), ri, screenshotFile);
	}

	private static File display(String device, RawImage rawImage, File screenshotFile) throws Exception {
		BufferedImage image = new BufferedImage(rawImage.width, rawImage.height, BufferedImage.TYPE_INT_RGB);
		// Dimension size = new Dimension(image.getWidth(), image.getHeight());

		int index = 0;
		int indexInc = rawImage.bpp >> 3;
		for (int y = 0; y < rawImage.height; y++) {
			for (int x = 0; x < rawImage.width; x++, index += indexInc) {
				int value = rawImage.getARGB(index);
				image.setRGB(x, y, value);
			}
		}
		if (screenshotFile == null) {
			screenshotFile = File.createTempFile("screenshot", ".png");

		}
		ImageIO.write(image, "png", screenshotFile);
		logger.info("ScreenShot can be found in:" + screenshotFile.getAbsolutePath());
		return screenshotFile;
	}

	/**
	 * Grab file from the device
	 * 
	 * @param deviceName
	 * @param fileLocation
	 *            file location on the device
	 * @param fileName
	 *            file name on the device
	 * @throws Exception
	 */
	public void getFile(String fileLocation, String fileName, String localLocation) throws Exception {
		try {
			File local = new File(localLocation.substring(0, localLocation.lastIndexOf(fileName) - 1));
			if (!local.exists())
				local.mkdirs();
			device.getSyncService().pullFile(fileLocation + "/" + fileName, localLocation,
					SyncService.getNullProgressMonitor());
		} catch (Exception e) {
			logger.error("Exception ", e);
			throw e;
		}
	}


	public abstract void connect() throws ConnectionException;

	public abstract void disconnect();

	public abstract void runTestOnDevice(String pakageName, String testClassName, String testName) throws IOException,
			Exception;

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Serial Number:").append(device.getSerialNumber()).append("\n");
		return sb.toString();
	}

	/**
	 * Push file to the device
	 * 
	 * @param deviceName
	 * @param fileLocation
	 *            file location on the device
	 * @param fileName
	 *            file name on the device
	 * @throws Exception
	 */
	public void pushFileToDevice(String remotefileLocation, String localLocation) throws Exception {
		try {
			device.getSyncService().pushFile(localLocation, remotefileLocation, SyncService.getNullProgressMonitor());
		} catch (Exception e) {
			logger.error("Exception ", e);
			throw e;
		}
	}

	public void sendIntent(IntentType type, String intent, String destination, Map<String, String> extra)
			throws AdbControllerException {
		if (intent == null || intent.isEmpty()) {
			throw new IllegalArgumentException("intent can't be empty");
		}
		if (type == null) {
			type = IntentType.BROADCAST;
		}
		StringBuilder command = new StringBuilder();
		command.append("am ");
		switch (type) {
		case BROADCAST:
			command.append("broadcast ");
			break;
		case DIRECT:
			if (null == destination || destination.isEmpty()) {
				throw new IllegalArgumentException("Destination can't be empty when sending direct intent");
			}
			command.append("start -n").append(destination).append(" ");
			break;

		default:
			throw new IllegalArgumentException("Unsupported intent type " + type.name());
		}
		command.append("-a ").append(intent).append(" ");
		for (String key : extra.keySet()) {
			command.append("-e \"").append(key).append("\" \"").append(extra.get(key)).append("\" ");
		}
		executeShellCommand(command.toString());

	}

	/**
	 * Install APK on device
	 * 
	 * @param apkLocation
	 * @throws InstallException
	 */
	public abstract void installPackage(String apkLocation, boolean reinstall) throws InstallException;

	// Shell receive services
	public void addOutput(byte[] data, int offset, int length) {
		shellResponse = new String(data);
	}

	/**
	 * Called at the end of the process execution (unless the process was
	 * canceled). This allows the receiver to terminate and flush whatever data
	 * was not yet processed.
	 */
	public void flush() {
	}

	public void startActivity(final String packageName, final String activityName) throws AdbControllerException {
		if (packageName == null || packageName.isEmpty() || activityName == null || activityName.isEmpty()) {
			throw new IllegalArgumentException("arguments can't be empty");
		}
		executeShellCommand(String.format("am start -n %s/.%s", packageName, activityName));
	}

	public void killApplication(String packageId) throws AdbControllerException {
		if (null == packageId || packageId.isEmpty()) {
			throw new IllegalArgumentException("Package id can't be null");
		}
		executeShellCommand("am force-stop " + packageId, 5000);
	}

	public String executeShellCommand(String shellCommand) throws AdbControllerException {
		return executeShellCommand(shellCommand, 10000);
	}

	public String executeShellCommand(String shellCommand, int maxTimeToOutputResponse) throws AdbControllerException {
		shellResponse = null;
		try {
			device.executeShellCommand(shellCommand, this, maxTimeToOutputResponse);
		} catch (Exception e) {
			throw new AdbControllerException("Failed to execute shell command: " + shellCommand, e);
		}
		return shellResponse;
	}

	/**
	 * Returns the current device battery level.
	 * 
	 * @return The device battery level.
	 * @throws If
	 *             failed to receive response from device or to parse to
	 *             response.
	 */
	public int getBatteryLevel() throws AdbControllerException {
		String response = executeShellCommand("cat  /sys/class/power_supply/battery/capacity");
		if (response == null || response.isEmpty()) {
			// Maybe the device is disconnected or busy
			throw new AdbControllerException("Failed to query device battery level");
		}
		if (response.contains("No such file or directory")) {
			// This device does not have the capacity file in the place we
			// expected it to be, we will give it another try.
			// The following location is the location of the file in 'Sony
			// Ericsson XPERIA' device
			response = executeShellCommand("cat  /sys/class/power_supply/bq27520/capacity");
		}
		if (response.contains("No such file or directory")) {
			// Still no luck.
			throw new AdbControllerException("Failed to query device battery level: " + response);
		}

		int batteryLevel = 0;
		try {
			batteryLevel = Integer.parseInt(response.trim());
		} catch (Throwable t) {
			throw new AdbControllerException("Failed to parse battery level from shell response " + response);
		}
		return batteryLevel;
	}

	/**
	 * Cancel method to stop the execution of the remote shell command.
	 * 
	 * @return true to cancel the execution of the command.
	 */
	public boolean isCancelled() {
		return false;
	}

	public boolean isOnline() {
		return device.isOnline();
	}

	public boolean isOffline() {
		return device.isOffline();
	}

	public String getSerialNumber() {
		return device.getSerialNumber();
	}
}
