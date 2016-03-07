package arituerto.acqPlatform;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

public class AcqPlatformActivity extends Activity implements CvCameraViewListener2, OnTouchListener, SensorEventListener {
	private static final String TAG = "OCV Acq Platform:: AcqPlatformActivity";

	private CamControlView mOpenCvCameraView;

	// Menu items for resolution, auto focus and sensors
	private List<Size> mResolutionList;
	private SubMenu mResolutionMenu;
	private MenuItem[] mResolutionMenuItems;
	private SubMenu mAutoFocusModeMenu;
	private List<String> mAutoFocusModeList;
	private MenuItem[] mAutoFocusModeItems;
	private SubMenu mAcqModeMenu;

	// Variables for sensor reading
	private Map<Integer,String> mSensorIntNameList;
	private SensorManager mSensorManager;
	private List<Sensor> mSensorList;
	private Map<Integer,Logger> mSensorLoggers;

	// Logging data
	private boolean mLogging;
	private boolean mAcqModeSequence; // false if Single Image, true if Sequence
	private long refNanoTime;
	private File loggingDir;
	private File imageDir;

	// Is there an openCV?
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
			{
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				mOpenCvCameraView.setOnTouchListener(AcqPlatformActivity.this);
			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}
		}
	};

	public AcqPlatformActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Set layout
		setContentView(R.layout.acqplatform_surface_view);
		mOpenCvCameraView = (CamControlView) findViewById(R.id.acqplatform_activity_java_surface_view);
		mOpenCvCameraView.setVisibility(CamControlView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);

		// Images time reference
		refNanoTime = System.nanoTime();
		
		// Build list of sensors identificators and sensor names
		mSensorIntNameList = new HashMap<Integer,String>();
		mSensorIntNameList.put(Sensor.TYPE_ACCELEROMETER,"ACCELEROMETER");
		mSensorIntNameList.put(Sensor.TYPE_GAME_ROTATION_VECTOR,"GAME_ROTATION_VECTOR");
		mSensorIntNameList.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,"GEOMAGNETIC_ROTATION_VECTOR");
		mSensorIntNameList.put(Sensor.TYPE_GRAVITY,"GRAVITY");
		mSensorIntNameList.put(Sensor.TYPE_GYROSCOPE,"GYROSCOPE");
		mSensorIntNameList.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED,"GYROSCOPE_UNCALIBRATED");
		mSensorIntNameList.put(Sensor.TYPE_LINEAR_ACCELERATION,"LINEAR_ACCELERATION");
		mSensorIntNameList.put(Sensor.TYPE_MAGNETIC_FIELD,"MAGNETIC_FIELD");
		mSensorIntNameList.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,"MAGNETIC_FIELD_UNCALIBRATED");
		mSensorIntNameList.put(Sensor.TYPE_ORIENTATION,"ORIENTATION");
		mSensorIntNameList.put(Sensor.TYPE_ROTATION_VECTOR,"ROTATION_VECTOR");
		mSensorIntNameList.put(Sensor.TYPE_STEP_COUNTER,"STEP_COUNTER");
		mSensorIntNameList.put(Sensor.TYPE_STEP_DETECTOR,"STEP_DETECTOR");

		// Set sensors manager and get available sensors
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mSensorList = getSensorList(mSensorManager);
		mSensorLoggers = new HashMap<Integer,Logger>();

		mLogging = false;
		mAcqModeSequence = true;
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
		// Activate sensor listeners
		ListIterator<Sensor> iter = mSensorList.listIterator();
		while (iter.hasNext()) {
			mSensorManager.registerListener(this,iter.next(),1000);
		}
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
			if (mAcqModeSequence) {
				//Save image!!
				File imgFileName = new File(imageDir.getPath() + "/img_" + System.nanoTime() + "_" + refNanoTime + ".jpg");
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
			} else {
				//Save image!!
				File imgFileName = new File(Environment.getExternalStorageDirectory().getPath() + "/img_" + System.nanoTime() + "_" + refNanoTime + ".jpg");
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
				mLogging = false;
			}
		}
		return inputFrame.rgba();
	}

	// CREATING THE MENU
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");

		mAutoFocusModeMenu = menu.addSubMenu("Auto Focus Modes");
		mAutoFocusModeList = mOpenCvCameraView.getAutoFocusModes();
		mAutoFocusModeItems = new MenuItem[mAutoFocusModeList.size()];
		ListIterator<String> modeItr = mAutoFocusModeList.listIterator();
		int idx = 0;
		while(modeItr.hasNext()) {
			String element = modeItr.next();
			mAutoFocusModeItems[idx] = mAutoFocusModeMenu.add(1, idx, idx,element);
			idx++;
		}

		mResolutionMenu = menu.addSubMenu("Resolution");
		mResolutionList = mOpenCvCameraView.getResolutionList();
		mResolutionMenuItems = new MenuItem[mResolutionList.size()];
		ListIterator<Size> resolutionItr = mResolutionList.listIterator();
		idx = 0;
		while(resolutionItr.hasNext()) {
			Size element = resolutionItr.next();
			mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, idx,
					Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
			idx++;
		}
		
		mAcqModeMenu = menu.addSubMenu("Acquisition Mode");
		mAcqModeMenu.add(3,1,1,"Single Image");
		mAcqModeMenu.add(3,2,2,"Image sequence");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

		if (item.getGroupId() == 1)
		{
			int id = item.getItemId();
			String afMode = mAutoFocusModeList.get(id);
			mOpenCvCameraView.setAutoFocusMode(afMode);
			String caption = "AF MODE: " + mOpenCvCameraView.getAutoFocusMode();
			Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
		} else if (item.getGroupId() == 2) {
			int id = item.getItemId();
			Size resolution = mResolutionList.get(id);
			mOpenCvCameraView.setResolution(resolution);
			resolution = mOpenCvCameraView.getResolution();
			String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
			Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
		} else if (item.getGroupId() == 3) {
			if (item.getItemId() == 1) {
				mAcqModeSequence = false;
				Toast.makeText(this, "Single Image", Toast.LENGTH_SHORT).show();
			} else if (item.getItemId() == 2) {
				mAcqModeSequence = true;
				Toast.makeText(this, "Sequence of Images", Toast.LENGTH_SHORT).show();
			}
		}
		return true;
	}

	// SENSORS
	public List<Sensor> getSensorList(SensorManager manager) {

		List<Sensor> sensorList = new ArrayList<Sensor>();
		// Sensors to log (iterate over mSensorIntNameList
		for (Integer key : mSensorIntNameList.keySet()) {
			if (manager.getDefaultSensor(key) != null) {
				sensorList.add(manager.getDefaultSensor(key));
				Log.i(TAG, "Sensor added: " + mSensorIntNameList.get(key));
			}
		}
		return sensorList;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

//		if (mLogging & mAcqModeSequence) {
//			Integer key = sensor.getType();
//			Logger sensorLogger = mSensorLoggers.get(key);
//			// String eventData = mSensorIntNameList.get(key) + "_ACC," + System.nanoTime();
//			String eventData = "ACC," + System.nanoTime();
//			switch (accuracy) {
//			case (SensorManager.SENSOR_STATUS_ACCURACY_HIGH):
//				eventData += ",4";
//				break;
//			case (SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM):
//				eventData += ",3";
//				break;
//			case (SensorManager.SENSOR_STATUS_ACCURACY_LOW):
//				eventData += ",2";
//				break;
//			case (SensorManager.SENSOR_STATUS_UNRELIABLE):
//				eventData += ",1";
//				break;
//			case (SensorManager.SENSOR_STATUS_NO_CONTACT):
//				eventData += ",0";
//				break;
//			}				
//			try {
//				sensorLogger.log(eventData);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (mLogging & mAcqModeSequence) {
			Integer key = event.sensor.getType();
			Logger sensorLogger = mSensorLoggers.get(key);
			// String eventData = mSensorIntNameList.get(key) + "_VAL," + System.nanoTime() + "," + event.timestamp;
			// String eventData = "VAL," + System.nanoTime() + "," + event.timestamp;
			String eventData = System.nanoTime() + "," + event.timestamp;
			for (float i : event.values){
				eventData += "," + i; 
			}
			try {
				sensorLogger.log(eventData);
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
		if (!mLogging & mAcqModeSequence)
		{

			// Create directory
			loggingDir = new File(Environment.getExternalStorageDirectory().getPath() +
					"/" + currentDateandTime);
			loggingDir.mkdirs();
			imageDir = new File(loggingDir.getPath() + "/images");
			imageDir.mkdirs();

			// Create the loggers
			ListIterator<Sensor> iter = mSensorList.listIterator();
			String loggerFileName = new String();
			while (iter.hasNext()) {
				Integer key = iter.next().getType();
				String sensorName = mSensorIntNameList.get(key);
				loggerFileName = loggingDir.getPath() + "/sensor_" + sensorName + "_log.csv";
				
				String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ns], EVENT_" + mSensorIntNameList.get(key) + "_VALUES";
				try {
					Logger logger = new Logger(loggerFileName);
					mSensorLoggers.put(key,logger);
					try {
						logger.log(csvFormat);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			Toast.makeText(this, "START LOGGING!", Toast.LENGTH_SHORT).show();
			mLogging = true;
		} else if (!mLogging & !mAcqModeSequence)
		{
			Toast.makeText(this, "TAKE PICTURE!", Toast.LENGTH_SHORT).show();
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
			mSensorLoggers.clear();

			Toast.makeText(this, "STOP LOGGING!", Toast.LENGTH_SHORT).show();
			mLogging = false;

		}
		return false;
	}
}
