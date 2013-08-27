package dhu.downing.heartrate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

import dhu.downing.util.ImageUtil;
import dhu.downing.wavelet.WaveEnum;
import dhu.downing.wavelet.Wavelet;

public class HeartRate {
	public static int calculation(double[] data,long time){
		Wavelet wavelet = new Wavelet();
		double[] temp = wavelet.waveletDenoise(data,1,WaveEnum.Coif5);
		final double[] data1 = ImageUtil.avgFilter(temp);
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				String saveData = "";
				for (int i = 0; i <= 99; i++) {
					saveData += (data1[i] + " ");
				}
				File dir = Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				if (!dir.exists()) {
					if (!dir.mkdirs()) {
						Log.d("Spo2", "failed to create directory");
						return;
					}
				}
				File file = new File(dir.getPath()
						+ File.separator + "graydata" + ".txt");
				FileOutputStream fos;
				try {
					fos = new FileOutputStream(file,true);
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
		int peakCount = ImageUtil.countPeak(data1).length;
		int troughCount = ImageUtil.countTrough(data1).length;
		double count = (peakCount+troughCount)/2.0;
		int result = (int) Math.round(60*1000/time*(count));
		return result;
	}
}
