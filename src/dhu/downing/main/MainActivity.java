package dhu.downing.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import dhu.downing.heartrate.HeartRate;
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
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.YuvImage;
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
	private int countHeartRate = -30;
	private int current = 0;
	private static final int MAX = 50;
	private double[] rBuffer = new double[MAX];
	private double[] bBuffer = new double[MAX];
	private double[] grayBuffer = new double[MAX];
	private double[] gray = new double[100];
	private boolean flag = true;
	private DrawHeartRateGraphThread myThread;

	private Camera mCamera;
	private CameraPreview mPreview;
	private MediaRecorder mMediaRecorder;
	private boolean isRecording = false;
	private TextView spo2Text;
	private TextView hrText;
	private SpO2Task mTask;
	private HeartRateTask hTask;
	private SurfaceView surface = null;
	private SurfaceHolder holder = null;
	private Paint paint;
	int HEIGHT = 300;
	int WIDTH = 455;
	int X_OFFSET = 5;
	int centerY = HEIGHT / 2;

	// private Point currentPoint = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initPaint();

		mCamera = getCameraInstance();

		Parameters p = mCamera.getParameters();
		// p.setPreviewFpsRange(20, 20);
		p.setPreviewFpsRange(10000, 10000);
		p.setPreviewSize(240, 160);
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
				myThread.isRun = false;
				myThread = null;
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				drawBack(holder);
				myThread = new MainActivity.DrawHeartRateGraphThread(holder);
				myThread.isRun = true;
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
				drawBack(holder);
			}
		});
		spo2Text = (TextView) findViewById(R.id.spo2);
		hrText = (TextView) findViewById(R.id.heartrateText);
	}

	private void initPaint() {
		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeWidth(3);
	}

	private void drawBack(SurfaceHolder holder) {
		Canvas canvas = holder.lockCanvas();
		canvas.drawColor(Color.WHITE);
		Paint p = new Paint();
		p.setColor(Color.BLACK);
		p.setStrokeWidth(2);
		// currentPoint.x = 10;
		// currentPoint.y = 300;
		canvas.drawLine(X_OFFSET, HEIGHT, WIDTH, HEIGHT, p);
		// canvas.drawLine(X_OFFSET, HEIGHT, currentPoint.x, currentPoint.y, p);
		canvas.drawLine(X_OFFSET, 10, X_OFFSET, HEIGHT, p);
		holder.unlockCanvasAndPost(canvas);
		holder.lockCanvas(new Rect(0, 0, 0, 0));
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

		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"SpO2Measurement");
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d(TAG, "failed to create directory");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
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
		param.setPreviewFpsRange(10000, 10000);
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
		
//		Thread thread = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				File file = getOutputMediaFile(MEDIA_TYPE_IMAGE);
//				try {
//					FileOutputStream fos = new FileOutputStream(file);
//					fos.write(data);
//					fos.close();
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		});
//		thread.start();
		
		saveAsPicture(data, camera);
		
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

		if (null != hTask) {
			switch (hTask.getStatus()) {
			case RUNNING:
				return;
			case PENDING:
				hTask.cancel(false);
				break;
			}
		}
		hTask = new HeartRateTask(data, camera);

		try {
			hrText.setText(hTask.execute((Void) null).get());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private void saveAsPicture(byte[] data,Camera camera){
		File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
		if (!pictureFile.exists()) {
			try {
				pictureFile.createNewFile();
				Camera.Parameters parameters = camera.getParameters();
				Size size = parameters.getPreviewSize();
				YuvImage image = new YuvImage(data,
						parameters.getPreviewFormat(), size.width, size.height,
						null);
				FileOutputStream filecon = new FileOutputStream(pictureFile);
				image.compressToJpeg(
						new Rect(0, 0, image.getWidth(), image.getHeight()),
						90, filecon);
			} catch (IOException e) {
				e.printStackTrace();
			}
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
				/*
				 * Log.e(TAG, "width:" + width + " height:" + height); int[]
				 * range = new int[2];
				 * camera.getParameters().getPreviewFpsRange(range); Log.e(TAG,
				 * "min:" + range[0] + " max:" + range[1]); try {
				 * Thread.currentThread().sleep(100000); } catch
				 * (InterruptedException e) { // TODO Auto-generated catch block
				 * e.printStackTrace(); }
				 */
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
					} else {
						bBuf[i / 3] = rgbBuf[i];
					}
				}

				rBuffer[count] = ImageUtil.average(rBuf, width, height);
				Log.e(TAG, "rBuffer[" + count + "]=" + rBuffer[count]);
				bBuffer[count] = ImageUtil.average(gBuf, width, height);
				Log.e(TAG, "bBuffer[" + count + "]=" + bBuffer[count]);
				if (rBuffer[count] < 230) {
					count = -10;
					rBuffer = ImageUtil.leftShift(rBuffer, 50);
					bBuffer = ImageUtil.leftShift(bBuffer, 50);
					text = "未检测到手指覆盖";
				} else {
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
					double spo2 = A - B * average;
					int temp = (int) Math.round(spo2 * 100);
					temp = temp >= 100 ? 99 : temp;
					temp = temp <= 90 ? 91 : temp;
					text = "您的血氧饱和度值为：" + temp + "%";
					// text = "您的血氧饱和度值为：" + average + "%";
					Log.e(TAG, text);
					count = count - 1;
					rBuffer = ImageUtil.leftShift(rBuffer, 1);
					bBuffer = ImageUtil.leftShift(bBuffer, 1);
				}
			}
			return text;
		}

	}

	private class HeartRateTask extends AsyncTask<Void, Void, String> {

		private byte[] mData;
		private Camera camera;

		public HeartRateTask(byte[] mData, Camera c) {
			super();
			this.mData = mData;
			this.camera = c;
		}

		@Override
		protected String doInBackground(Void... params) {
			// TODO Auto-generated method stub
			String result = "未检测到手指覆盖";
			countHeartRate++;
			if (countHeartRate >= 0) {
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
					} else {
						bBuf[i / 3] = rgbBuf[i];
					}
				}
				double red, green, blue;
				red = ImageUtil.average(rBuf, width, height);
				green = ImageUtil.average(gBuf, width, height);
				blue = ImageUtil.average(bBuf, width, height);
				if (red < 230) {
					countHeartRate = -10;
					ImageUtil.leftShift(gray, 100);
					result = "未检测到手指覆盖";
					if(myThread != null){
						myThread.isRun = false;
						myThread = null;
					}
					flag = true;
					return result;
				} else {
					result = "心率正在计算，请稍候...";
				}
				gray[countHeartRate] = 0.2989 * red + 0.5870 * green + 0.1141
						* blue;
				if (countHeartRate == 99) {
					/*
					if (flag) {
						flag = false;
						Thread thread = new Thread(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								String saveData = "";
								for (int i = 0; i <= 99; i++) {
									saveData += (gray[i] + " ");
								}
								File dir = Environment
										.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
								if (!dir.exists()) {
									if (!dir.mkdirs()) {
										Log.d(TAG, "failed to create directory");
										return;
									}
								}
								File file = new File(dir.getPath()
										+ File.separator + "graydata" + ".txt");
								FileOutputStream fos;
								try {
									fos = new FileOutputStream(file);
									fos.write(saveData.getBytes());
									fos.close();
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						});
						thread.start();
					}*/
					if(flag){
						if(myThread == null){
							myThread = new MainActivity.DrawHeartRateGraphThread(holder);
							myThread.isRun = true;
						}
						myThread.setData(gray);
						myThread.start();
						flag = false;
					}
					countHeartRate--;
					int rate = HeartRate.calculation(gray, 10000);
					result = "您的心率为" + rate;
					gray = ImageUtil.leftShift(gray, 1);
				}
			}
			return result;
		}

	}
	
	private class DrawHeartRateGraphThread extends Thread{
		private Boolean isRun;
		private SurfaceHolder holder;
		private double[] data;
		
		
		public Boolean getIsRun() {
			return isRun;
		}

		public void setIsRun(Boolean isRun) {
			this.isRun = isRun;
		}

		public SurfaceHolder getHolder() {
			return holder;
		}

		public void setHolder(SurfaceHolder holder) {
			this.holder = holder;
		}

		public DrawHeartRateGraphThread(SurfaceHolder holder){
			this.holder = holder;
		}
		
		@SuppressLint("NewApi")
		@Override
		public void run() {
			int currentX = 10;
			int oldX = currentX;
			ArrayList<Double> temp = new ArrayList<Double>();
			for(int i=0;i<data.length;i++){
				if(data[i]==0)
					break;
				temp.add(data[i]);
			}
			int length = temp.size();
			if(length <= 1)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else{
				float[] y = new float[length];
				for(int k=0;k<length;k++){
					y[k]=(float) (300-(temp.get(k)-95)*15);
				}
				int j=0;
				while(isRun){
					Canvas c = null;
					synchronized (holder) {
						if(currentX>=WIDTH){
							c = holder.lockCanvas(new Rect(oldX, 10, currentX+11, HEIGHT));
							c.drawColor(Color.WHITE, Mode.ADD);
							holder.unlockCanvasAndPost(c);
							currentX = 10;
							oldX = currentX;
						}
						else{
							c = holder.lockCanvas(new Rect(oldX, 10, currentX+11, HEIGHT));
							c.drawColor(Color.WHITE, Mode.ADD);
							Paint p = new Paint();
							p.setColor(Color.RED);
							p.setStrokeWidth(3);
							if(j==0)
								j++;
							c.drawLine(oldX, y[j-1], currentX+10, y[j], p);
							j++;
							if(j==length)
								j=0;
							oldX = currentX+10;
							currentX += 10;
							holder.unlockCanvasAndPost(c);
						}
					}
				}
			}
		}

		public double[] getData() {
			return data;
		}

		public void setData(double[] data) {
			this.data = data;
		}
		
	}
}