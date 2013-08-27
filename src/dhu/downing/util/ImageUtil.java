package dhu.downing.util;

import java.util.ArrayList;

import android.util.Log;

import dhu.downing.wavelet.WaveEnum;
import dhu.downing.wavelet.Wavelet;

public class ImageUtil {
	// YUV420SP转换为RGB
	public static byte[] decodeYUV420SP(byte[] rgbBuf, byte[] yuv420sp,
			int width, int height) {
		final int frameSize = width * height;
		if (rgbBuf == null)
			throw new NullPointerException("buffer 'rgbBuf' is null");
		if (rgbBuf.length < frameSize * 3)
			throw new IllegalArgumentException("buffer 'rgbBuf' size "
					+ rgbBuf.length + " < minimum " + frameSize * 3);

		if (yuv420sp == null)
			throw new NullPointerException("buffer 'yuv420sp' is null");

		if (yuv420sp.length < frameSize * 3 / 2)
			throw new IllegalArgumentException("buffer 'yuv420sp' size "
					+ yuv420sp.length + " < minimum " + frameSize * 3 / 2);

		int i = 0, y = 0;
		int uvp = 0, u = 0, v = 0;
		int y1192 = 0, r = 0, g = 0, b = 0;

		for (int j = 0, yp = 0; j < height; j++) {
			uvp = frameSize + (j >> 1) * width;
			u = 0;
			v = 0;
			for (i = 0; i < width; i++, yp++) {
				y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}

				y1192 = 1192 * y;
				r = (y1192 + 1634 * v);
				g = (y1192 - 833 * v - 400 * u);
				b = (y1192 + 2066 * u);

				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;

				rgbBuf[yp * 3] = (byte) (r >> 10);
				rgbBuf[yp * 3 + 1] = (byte) (g >> 10);
				rgbBuf[yp * 3 + 2] = (byte) (b >> 10);
			}
		}
		return rgbBuf;
	}

	// 求一帧图像的平均亮度值，也可以是RGB某个分量的平均值
	public static double average(byte[] buf, int width, int height) {
		double result = 0;
		int frameSize = width * height;
		int temp;
		byte data;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {//注意byte类型是有符号的
				data = buf[j + i * height];
				temp = data>=0?data:(256+data);
				result += temp;
			}
		}
		return result / frameSize;
	}

	// 均值滤波
	public static double[] avgFilter(double[] buf) {
		int size = buf.length;
		int i = 0;
		double[] result = new double[size];
		result[0] = (buf[0] + buf[1] + buf[2]) / 3;
		for (i = 1; i < size - 1; i++) {
			result[i] = (buf[i - 1] + buf[i] + buf[i + 1]) / 3;
		}
		result[size - 1] = (buf[size - 1] + buf[size - 2] + buf[size - 3]) / 3;
		return result;
	}

	// 计算经过滤波之后的波峰数和所在的位置,返回数组的length为波峰数，所对应的值为波峰所在的位置
	public static int[] countPeak(double[] buf) {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		int length = buf.length;
		for (int i = 4; i < length - 4; i++) {
			if ((buf[i] > buf[i - 1]) && (buf[i] > buf[i + 1]) && (buf[i] > buf[i - 2]) && (buf[i] > buf[i + 2])&& (buf[i] > buf[i - 3]) && (buf[i] > buf[i + 3])
					&& (buf[i] > buf[i - 4]) && (buf[i] > buf[i + 4])){
				temp.add(i);
			}
		}
		int size = temp.size();
		int[] result = new int[size];
		for(int i=0;i<size;i++){
			result[i]=temp.get(i);
		}
		return result;
	}

	// 计算经过滤波之后的波谷数和所在的位置，返回数组的length为波谷数，所对应的值为波谷所在的位置
	public static int[] countTrough(double[] buf) {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		int length = buf.length;
		for (int i = 4; i < length - 4; i++) {
			if ((buf[i] < buf[i - 1]) && (buf[i] < buf[i + 1]) && (buf[i] < buf[i - 2]) && (buf[i] < buf[i + 2])&& (buf[i] < buf[i - 3]) && (buf[i] < buf[i + 3])
					&& (buf[i] < buf[i - 4]) && (buf[i] < buf[i + 4])){
				temp.add(i);
			}
		}
		int size = temp.size();
		int[] result = new int[size];
		for(int i=0;i<size;i++){
			result[i]=temp.get(i);
		}
		return result;
	}

	// 计算RGB每个分量的D=Iac/Idc的值
	public static double[] calculationD(double[] buf) {
		/*
		Wavelet wavelet = new Wavelet();
		double[] temp = wavelet.waveletDenoise(buf,3,WaveEnum.Coif5);*、
		/*
		for(int k=0;k<temp.length;k++){
			Log.e("SpO2Measurement",temp[k]+" ");
		}
		try {
			Thread.currentThread().sleep(100000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		double[] avg = avgFilter(buf);
		int[] peaks = countPeak(avg);
		int[] troughs = countTrough(avg);
		int length = peaks.length > troughs.length ? troughs.length
				: peaks.length;
		Log.e("SpO2Measurement",length+" "+peaks.length);
		double[] result = new double[length];
		for (int i = 0; i < length; i++) {
			double a = buf[peaks[i]];
			double b = buf[troughs[i]];
			result[i] = (a-b) / b;
		}
		return result;
	}

	// 计算红色分量与蓝色分量每个D之间的比值，用以求出SpO2=A-B*Dred/Dblue,再用曲线拟合得出A、B值
	public static double[] dRedDivideDBlue(double[] rBuf, double[] bBuf) {
		double[] dRed = calculationD(rBuf);
		double[] dBlue = calculationD(bBuf);
		int length = dRed.length > dBlue.length ? dBlue.length : dRed.length;
		double[] result = new double[length];
		for (int i = 0; i < length; i++) {
			result[i] = dRed[i] / dBlue[i];
		}
		return result;
	}

	// 数组左移
	public static double[] leftShift(double[] test,int n) {
		int length = test.length;
		//double temp = test[0];
		for (int i = n; i < length; i++) {
			test[i - n] = test[i];
		}
		for(int j=length-n;j<length;j++)
			test[j]=0;
		return test;
	}
	
	public static double minValue(double[] test){
		double temp = test[0];
		for(int i = 0;i<test.length;i++){
			if(temp>test[i]){
				temp = test[i];
			}
		}
		return temp;
	}
	
	public static double maxValue(double[] test){
		double temp = test[0];
		for(int i = 0;i<test.length;i++){
			if(temp<test[i]){
				temp = test[i];
			}
		}
		return temp;
	}
}
