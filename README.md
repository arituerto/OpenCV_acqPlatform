# OpenCV_acqPlatform
Simple app to save an timestamped stream of imaged and timestamped IMU sensor reads.

This app is based in the OpenCV4Android example Tutorial 3 - Camera Control.

The application saves time stamped images and timestamped sensor events. The sensors that the application logs (if available in the device) are:
- ACCELEROMETER
- LINEAR_ACCELERATION
- GRAVITY
- GYROSCOPE
- GEOMAGNETIC_ROTATION_VECTOR
- ROTATION_VECTOR
- GAME_ROTATION_VECTOR
- MAGNETIC_FIELD

Images are stored as jpg files and include two timestamps (for data synchronization) the acquisition time and the time when the application was launched. Both numbers are defined in the image name:

**img_[acquisition Time, ns]_[app launching time, ns].[XXX image file extension]**

The app offers an options menu where the focus mode and the image resolution can be configured.

### Issues
- I don't know the reference for the sensors timestamps so the synchronization of images and sensor readings is incorrect.
- The Options Menu is created in the code but this method has been deprecated.
- OpenCV class JavaCameraView works with the android.hardware.Camera that has been also deprecated.

