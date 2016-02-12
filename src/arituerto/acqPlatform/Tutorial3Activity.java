package arituerto.acqPlatform;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

public class Tutorial3Activity extends Activity implements CvCameraViewListener2, OnTouchListener, SensorEventListener {
	private static final String TAG = "OCV Acq Platform::Activity";

	private Tutorial3View mOpenCvCameraView;

	// Menu items for resolution, auto focus and sensors
	private List<Size> mResolutionList;
	private SubMenu mResolutionMenu;
	private MenuItem[] mResolutionMenuItems;
	private SubMenu mAutoFocusModeMenu;
	private List<String> mAutoFocusModeList;
	private MenuItem[] mAutoFocusModeItems;


	// Variables for sensor reading
	private SensorManager mSensorManager;
	private List<Sensor> mSensorList;
	private Map<Integer,Logger> mSensorLoggers;

	// Logging data
	private boolean mLogging;
	private long refNanoTime;
	private File loggingDir;

	// Is there an openCV?
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
			{
				Log.i(TAG, "OpenCV loaded successfully");

				mOpenCvCameraView.enableView();

				mOpenCvCameraView.setOnTouchListener(Tutorial3Activity.this);

			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}
		}
	};

	public Tutorial3Activity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Set layout
		setContentView(R.layout.tutorial3_surface_view);
		mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);
		mOpenCvCameraView.setVisibility(Tutorial3View.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();

		// Release all the sensor listeners
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (!OpenCVLoader.initDebug()) {
			Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
		} else {
			Log.d(TAG, "OpenCV library found inside package. Using it!");
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}

		// Images time reference
		refNanoTime = System.nanoTime();

		// Set sensors manager, get available sensors and set the listeners
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mSensorList = getSensorList(mSensorManager);
		ListIterator<Sensor> iter = mSensorList.listIterator();
		while (iter.hasNext()) {
			mSensorManager.registerListener(this, iter.next(), SensorManager.SENSOR_DELAY_FASTEST);
		}

		// Images time reference
		refNanoTime = System.nanoTime();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();

		// Release all the sensor listeners
		mSensorManager.unregisterListener(this);
	}

	// CAMERA
	public void onCameraViewStarted(int width, int height) {
	}

	public void onCameraViewStopped() {
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		if (mLogging) {

			//Save image!!
			File imgFileName = new File(loggingDir.getPath() + "/img_" + System.nanoTime() + "_" + refNanoTime + ".jpg");
			// Convert to Bitmap (android)
			Bitmap rgbaBitmap = Bitmap.createBitmap(inputFrame.rgba().cols(), inputFrame.rgba().rows(), Bitmap.Config.ARGB_8888);;
			Utils.matToBitmap(inputFrame.rgba(),rgbaBitmap);
			// Save
			try {
				FileOutputStream fos = new FileOutputStream(imgFileName);
				rgbaBitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
				fos.flush();
				fos.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return inputFrame.rgba();
	}

	// CREATING THE MENU
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		mAutoFocusModeMenu = menu.addSubMenu("Auto Focus Modes");
		mAutoFocusModeList = mOpenCvCameraView.getAutoFocusModes();
		mAutoFocusModeItems = new MenuItem[mAutoFocusModeList.size()];
		ListIterator<String> modeItr = mAutoFocusModeList.listIterator();
		int idx = 0;
		while(modeItr.hasNext()) {
			String element = modeItr.next();
			mAutoFocusModeItems[idx] = mAutoFocusModeMenu.add(1, idx, Menu.NONE,element);
			idx++;
		}   	

		mResolutionMenu = menu.addSubMenu("Resolution");
		mResolutionList = mOpenCvCameraView.getResolutionList();
		mResolutionMenuItems = new MenuItem[mResolutionList.size()];
		ListIterator<Size> resolutionItr = mResolutionList.listIterator();
		idx = 0;
		while(resolutionItr.hasNext()) {
			Size element = resolutionItr.next();
			mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
					Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
			idx++;
		}

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

		if (item.getGroupId() == 1)
		{
			int id = item.getItemId();
			String afMode = mAutoFocusModeList.get(id);
			Size resolution = mOpenCvCameraView.getResolution();
			mOpenCvCameraView.setAutoFocusMode(afMode);
			mOpenCvCameraView.setResolution(resolution);
			String caption = "AF MODE: " + mOpenCvCameraView.getAutoFocusMode();
			Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
		}
		else if (item.getGroupId() == 2)
		{
			int id = item.getItemId();
			String afMode = mAutoFocusModeList.get(id);
			Size resolution = mResolutionList.get(id);
			mOpenCvCameraView.setResolution(resolution);
			mOpenCvCameraView.setAutoFocusMode(afMode);
			resolution = mOpenCvCameraView.getResolution();
			String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
			Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
		}

		return true;
	}

	// SENSORS
	// Check if sensors are available
	public List<Sensor> getSensorList(SensorManager manager) {

		List<Sensor> mSensorList = new ArrayList<Sensor>();

		// Sensors that I want to log
		if (manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			mSensorList.add(manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		}
		if (manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
			mSensorList.add(manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
		}
		if (manager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
			mSensorList.add(manager.getDefaultSensor(Sensor.TYPE_GRAVITY));
		}
		if (manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
			mSensorList.add(manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
		}
		if (manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null) {
			mSensorList.add(manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR));
		}
		if (manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
			mSensorList.add(manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
		}
		if (manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null) {
			mSensorList.add(manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR));
		}

		return mSensorList;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (mLogging)
		{
			Logger sensorLogger = mSensorLoggers.get(event.sensor.getType());
			String accData = event.timestamp + "," + event.values[0] + "," +
					event.values[1] + "," + event.values[2];
			try {
				sensorLogger.log(accData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// ON TOUCH
	@SuppressLint("SimpleDateFormat")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.i(TAG,"onTouch event");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		String currentDateandTime = sdf.format(new Date());

		// Start to log sensors and images
		if (!mLogging) // START LOGGING! Open imuLogFile and start to write
		{
			// Create directory
			loggingDir = new File(Environment.getExternalStorageDirectory().getPath() +
					"/" + currentDateandTime);
			loggingDir.mkdirs();
			// Create the loggers
			ListIterator<Sensor> iter = mSensorList.listIterator();
			String loggerName = new String();
			while (iter.hasNext()) {
				switch (iter.next().getType()) {
				case (Sensor.TYPE_ACCELEROMETER):
					loggerName = loggingDir.getPath() + "/typeAccelerometer_log.csv";
				try {
					mSensorLoggers.put(Sensor.TYPE_ACCELEROMETER,new Logger(loggerName));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				break;
				case (Sensor.TYPE_LINEAR_ACCELERATION):
					loggerName = loggingDir.getPath() + "/typeLinearAcceleration_log.csv";
				try {
					mSensorLoggers.put(Sensor.TYPE_LINEAR_ACCELERATION,new Logger(loggerName));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				break;
				case (Sensor.TYPE_GRAVITY):
					loggerName = loggingDir.getPath() + "/typeGravity_log.csv";
				try {
					mSensorLoggers.put(Sensor.TYPE_GRAVITY,new Logger(loggerName));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				break;
				case (Sensor.TYPE_GYROSCOPE):
					loggerName = loggingDir.getPath() + "/typeGyroscope_log.csv";
				try {
					mSensorLoggers.put(Sensor.TYPE_GYROSCOPE,new Logger(loggerName));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				break;
				case (Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR):
					loggerName = loggingDir.getPath() + "/typeGeomagneticRotationVector_log.csv";
				try {
					mSensorLoggers.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,new Logger(loggerName));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				break;
				case (Sensor.TYPE_ROTATION_VECTOR):
					loggerName = loggingDir.getPath() + "/typeRotationVector_log.csv";
				try {
					mSensorLoggers.put(Sensor.TYPE_ROTATION_VECTOR,new Logger(loggerName));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				break;
				case (Sensor.TYPE_GAME_ROTATION_VECTOR):
					loggerName = loggingDir.getPath() + "/typeGameRotationVector_log.csv";
				try {
					mSensorLoggers.put(Sensor.TYPE_GAME_ROTATION_VECTOR,new Logger(loggerName));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				break;
				}
			}

			Toast.makeText(this, "START LOGGING!", Toast.LENGTH_SHORT).show();
			mLogging = true;

		} else {
			Iterator<Map.Entry<Integer,Logger>> iter = mSensorLoggers.entrySet().iterator();
			while (iter.hasNext()) {
				try {
					iter.next().getValue().close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			Toast.makeText(this, "STOP LOGGING!", Toast.LENGTH_SHORT).show();
			mLogging = false;

		}
		return false;
	}
}