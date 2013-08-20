package dhu.downing.main;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import dhu.downing.util.ImageUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements PreviewCallback {

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private static final String TAG = "SpO2Measurement";
	private String text = "未检测到手指覆盖";
	private static final double A = 0.9764;
	private static final double B = 0.2543;
	private int count = -30;
	private int current = 0;
	private static final int MAX = 50;
	private double[] rBuffer = new double[MAX];
	private double[] bBuffer = new double[MAX];
	private double[] grayBuffer = new double[MAX];

	private Camera mCamera;
	private CameraPreview mPreview;
	private MediaRecorder mMediaRecorder;
	private boolean isRecording = false;
	private TextView spo2Text;
	private SpO2Task mTask;
	private SurfaceView surface = null;
	private SurfaceHolder holder = null;
	private Paint paint;
	int HEIGHT=300;
	int WIDTH = 500;
	int X_OFFSET=5;
	int centerY=HEIGHT/2;
	//private Point currentPoint = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initPaint();

		mCamera = getCameraInstance();
		Parameters p = mCamera.getParameters();
		//p.setPreviewFpsRange(20, 20);
		p.setPreviewFpsRange(20000,20000);
		mCamera.setParameters(p);
		mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);

		surface = (SurfaceView) findViewById(R.id.heartRate);
		// WIDTH = surface.getWidth() - X_OFFSET -1;
		holder = surface.getHolder();
		
		holder.addCallback(new SurfaceHolder.Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				drawBack(holder);
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width,
					int height) {
				drawBack(holder);
			}
		});
		spo2Text = (TextView) findViewById(R.id.spo2);

	}

	private void initPaint(){
		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeWidth(3);
	}
	
	private void drawBack(SurfaceHolder holder){
    	Canvas canvas = holder.lockCanvas();
    	canvas.drawColor(Color.WHITE);
    	Paint p = new Paint();
    	p.setColor(Color.BLACK);
    	p.setStrokeWidth(2);
    	//currentPoint.x = 10;
    	//currentPoint.y = 300;
    	canvas.drawLine(X_OFFSET, HEIGHT, WIDTH, HEIGHT, p);
    	//canvas.drawLine(X_OFFSET, HEIGHT, currentPoint.x, currentPoint.y, p);
    	canvas.drawLine(X_OFFSET, 10, X_OFFSET, HEIGHT, p);
    	holder.unlockCanvasAndPost(canvas);
    	holder.lockCanvas(new Rect(0,0,0,0));
    	holder.unlockCanvasAndPost(canvas);
    }


	private void createDialog() {
		final Builder builder = new Builder(this);
		builder.setTitle("提示");
		builder.setMessage("是否确定退出？");
		builder.setPositiveButton("退出", new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				MainActivity.this.onBackPressed();
				MainActivity.this.finish();
				System.exit(0);
			}
		});

		builder.setNegativeButton("取消", null);
		builder.create().show();
	}

	@Override
	public void onBackPressed() {
		createDialog();
	}

	private static File getOutputMediaFile(int type) {

		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"SpO2Measurement");

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d(TAG, "failed to create directory");
				return null;
			}
		}

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
		} else if (type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}

	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
		}
		return c; // returns null if camera is unavailable
	}

	private boolean prepareVideoRecorder() {
		releaseCamera();

		mCamera = getCameraInstance();
		Parameters param = mCamera.getParameters();
		param.setFlashMode(Parameters.FLASH_MODE_TORCH);
		param.setPreviewFpsRange(20000, 20000);
		mCamera.setParameters(param);

		mMediaRecorder = new MediaRecorder();

		// mPreview = new CameraPreview(MainActivity.this, mCamera);

		// Step 1: Unlock and set camera to MediaRecorder
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);

		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		// mMediaRecorder.setProfile(CamcorderProfile
		// .get(CamcorderProfile.QUALITY_TIME_LAPSE_HIGH));
		mMediaRecorder.setProfile(CamcorderProfile
				.get(CamcorderProfile.QUALITY_HIGH));

		// mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		// mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		// mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO)
				.toString());

		// Step 5: Set the preview output
		mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
		// mMediaRecorder.setCaptureRate(5);

		// Step 6: Prepare configured MediaRecorder
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.d(TAG,
					"IllegalStateException preparing MediaRecorder: "
							+ e.getMessage());
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaRecorder(); // if you are using MediaRecorder, release it
								// first
		releaseCamera(); // release the camera immediately on pause event
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

	}

	private void releaseMediaRecorder() {
		if (mMediaRecorder != null) {
			mMediaRecorder.reset(); // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			mCamera.lock(); // lock camera for later use
		}
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseMediaRecorder();
		releaseCamera();
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		if (null != mTask) {
			switch (mTask.getStatus()) {
			case RUNNING:
				return;
			case PENDING:
				mTask.cancel(false);
				break;
			}
		}
		mTask = new SpO2Task(data, camera);

		try {
			spo2Text.setText(mTask.execute((Void) null).get());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	private class SpO2Task extends AsyncTask<Void, Void, String> {

		private byte[] mData;
		private Camera camera;

		public SpO2Task(byte[] mData, Camera c) {
			super();
			this.mData = mData;
			this.camera = c;
		}

		@Override
		protected String doInBackground(Void... params) {
			Canvas canvas = null;
			count++;
			Log.e(TAG, count + " ");
			if (count >= 0) {
				// Log.d(TAG, count+" ");
				Size size = camera.getParameters().getPreviewSize();
				int width = size.width;
				int height = size.height;
				int length = width * height * 3;
				byte[] rgbBuf = new byte[length];
				rgbBuf = ImageUtil.decodeYUV420SP(rgbBuf, mData, width, height);
				byte[] rBuf = new byte[width * height];
				byte[] bBuf = new byte[width * height];
				byte[] gBuf = new byte[width * height];
				for (int i = 0; i < length; i++) {
					if (i % 3 == 0) {
						rBuf[i / 3] = rgbBuf[i];
					} else if (i % 3 == 1) {
						gBuf[i / 3] = rgbBuf[i];
					}else{
						bBuf[i / 3] = rgbBuf[i];
					}
				}
				/* 绘制心率曲线部分 代码有问题
				if(current<WIDTH){
					synchronized (holder) {
						double red,green,blue;
						red = ImageUtil.average(rBuf, width, height);
						green = ImageUtil.average(gBuf, width, height);
						blue = ImageUtil.average(bBuf, width, height);
						grayBuffer[count] = 0.2989 * red + 0.5870 * green + 0.1140 * blue;
						int temp = (int) ((grayBuffer[count]/255) *200);
						canvas = holder.lockCanvas(new Rect(current+X_OFFSET,temp-2,current+2+X_OFFSET,temp+2));
						canvas.drawPoint(current+X_OFFSET, temp, paint);
						holder.unlockCanvasAndPost(canvas);
						current++;
					}
				}*/
//				if (count <= MAX - 1 && count % 2 == 0) {
//					rBuffer[count / 2] = ImageUtil.average(rBuf, width, height);
//					Log.e(TAG, "rBuffer[" + count / 2 + "]="
//							+ rBuffer[count / 2]);
//				} else if (count <= MAX - 1 && count % 2 == 1) {
//					bBuffer[count / 2] = ImageUtil.average(gBuf, width, height);
//					Log.e(TAG, "bBuffer[" + count / 2 + "]="
//							+ bBuffer[count / 2]);
//				}
				
				rBuffer[count] = ImageUtil.average(rBuf, width, height);
				Log.e(TAG, "rBuffer[" + count + "]="
						+ rBuffer[count]);
				bBuffer[count] = ImageUtil.average(gBuf, width, height);
				Log.e(TAG, "bBuffer[" + count + "]="
						+ bBuffer[count]);
				if(rBuffer[count]<200){
					count=-10;
					
					text = "未检测到手指覆盖";
				}else{
					text = "血氧饱和度数据正在计算，请稍候...";
				}
				if (count == MAX - 1) {
					double[] result = ImageUtil.dRedDivideDBlue(rBuffer,
							bBuffer);
					double sum = 0;
					for (int i = 0; i < result.length; i++) {
						sum += result[i];
					}
					
					double average = sum / result.length;
					 double spo2 = A-B*average;
					 int temp = (int) Math.round(spo2 * 100);
					 temp = temp>=100?99:temp;
					 temp = temp<=90?91:temp;
					 text = "您的血氧饱和度值为：" + temp + "%";
					//text = "您的血氧饱和度值为：" + average + "%";
					 Log.e(TAG, text);
					count = count-1;
					rBuffer = ImageUtil.leftShift(rBuffer,1);
					bBuffer = ImageUtil.leftShift(bBuffer,1);
				}
			}
			return text;
		}

	}
}