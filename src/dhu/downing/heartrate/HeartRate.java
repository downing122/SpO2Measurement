package dhu.downing.heartrate;

import dhu.downing.util.ImageUtil;

public class HeartRate {
	public static int calculation(double[] data,long time){
		int peakCount = ImageUtil.countPeak(data).length;
		int result = Math.round(60*1000/time*peakCount);
		return result;
	}
}
