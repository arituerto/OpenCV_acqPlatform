package arituerto.acqPlatform;

import java.util.List;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;

public class Tutorial3View extends JavaCameraView {

    private static final String TAG = "OCV Acq Platform::Tutorial3View";

    public Tutorial3View(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // SELECT RESOLUTION
    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }
    
    // SELECT AUTOFOCUS MODE
    public List<String> getAutoFocusModes() {
    	return mCamera.getParameters().getSupportedFocusModes();
    }

    public void setAutoFocusMode(String afMode) {
    	Camera.Parameters params = mCamera.getParameters();
    	params.setFocusMode(afMode);
    	mCamera.setParameters(params);
    }
    
    public String getAutoFocusMode() {
    	return mCamera.getParameters().getFocusMode();
    }

}
