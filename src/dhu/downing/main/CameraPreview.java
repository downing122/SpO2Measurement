package dhu.downing.main;

import java.io.IOException;

import dhu.downing.util.ImageUtil;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {
	private Context c;
//	private static final double A = 0;
//	private static final double B = 0;
	private static final String TAG = "SpO2Measurement";
//	private static int count = -10;
//	private static int MAX = 50;
//	private static double[] rBuffer = new double[MAX/2];
//	private static double[] bBuffer = new double[MAX/2];
	private SurfaceHolder mHolder;
	private Camera mCamera;

	public CameraPreview(Context context, Camera camera) {
		super(context);
		c = context;
		mCamera = camera;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.setDisplayOrientation(90);
			mCamera.startPreview();
			openFlash();
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	public void openFlash() {
		Parameters param = mCamera.getParameters();
		param.setPreviewFpsRange(10000, 10000);
		param.setFlashMode(Parameters.FLASH_MODE_TORCH);
		mCamera.setParameters(param);
	}

	public void closeFlash() {
		Parameters param = mCamera.getParameters();
		param.setFlashMode(Parameters.FLASH_MODE_OFF);
		mCamera.setParameters(param);
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
			openFlash();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.setPreviewCallback((PreviewCallback) c);
			mCamera.startPreview();

		} catch (Exception e) {
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

	//预览帧时检测血氧饱和度
//	@Override
//	public void onPreviewFrame(byte[] data, Camera camera) {
//		count++;
//		if (count >= 0) {
//			Size size = camera.getParameters().getPreviewSize();
//			int width = size.width;
//			int height = size.height;
//			int length = width * height * 3;
//			byte[] rgbBuf = new byte[length];
//			rgbBuf = ImageUtil.decodeYUV420SP(rgbBuf, data, width, height);
//			byte[] rBuf = new byte[width * height];
//			byte[] bBuf = new byte[width * height];
//			for (int i = 0; i < length; i++) {
//				if (i % 3 == 0) {
//					rBuf[i / 3] = rgbBuf[i];
//				} else if (i % 3 == 2) {
//					bBuf[i / 3] = rgbBuf[i];
//				}
//			}
//			if(count%2==0){
//				rBuffer[count/2] = ImageUtil.average(rBuf, width, height);
//			}else{
//				bBuffer[(count/2)-1] = ImageUtil.average(bBuf, width, height);
//			}
//			if(count==MAX-1){
//				double[] result = ImageUtil.dRedDivideDBlue(rBuffer, bBuffer);
//				double sum = 0;
//				for(int i=0;i<result.length;i++){
//					sum += result[i];
//				}
//				double average = sum/result.length;
//				double spo2 = A-B*average;
//				((TextView)findViewById(R.id.spo2)).setText("您的血氧饱和度值为：" + spo2*100 + "%");
//				Toast t = Toast.makeText(c,"您的血氧饱和度值为：" + spo2*100 + "%" , Toast.LENGTH_LONG);
//				t.show();
//				count=MAX-3;
//				rBuffer = ImageUtil.leftShiftOne(rBuffer);
//				bBuffer = ImageUtil.leftShiftOne(bBuffer);
//			}
//		}
//	}
	
	
}
