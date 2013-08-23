package dhu.downing.heartrate;

import dhu.downing.util.ImageUtil;
import dhu.downing.wavelet.WaveEnum;
import dhu.downing.wavelet.Wavelet;

public class HeartRate {
	public static int calculation(double[] data,long time){
		Wavelet wavelet = new Wavelet();
		double[] temp = wavelet.waveletDenoise(data,3,WaveEnum.Coif5);
		int peakCount = ImageUtil.countPeak(temp).length;
		int result = Math.round(60*1000/time*peakCount);
		return result;
	}
}
