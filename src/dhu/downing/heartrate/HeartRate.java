package dhu.downing.heartrate;

import dhu.downing.util.ImageUtil;
import dhu.downing.wavelet.WaveEnum;
import dhu.downing.wavelet.Wavelet;

public class HeartRate {
	public static int calculation(double[] data,long time){
		Wavelet wavelet = new Wavelet();
		double[] temp = wavelet.waveletDenoise(data,1,WaveEnum.Coif5);
		data = ImageUtil.avgFilter(temp);
		int peakCount = ImageUtil.countPeak(data).length;
		int troughCount = ImageUtil.countTrough(data).length;
		double count = (peakCount+troughCount)/2.0;
		int result = (int) Math.round(60*1000/time*(count));
		return result;
	}
}
