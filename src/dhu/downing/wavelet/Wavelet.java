package dhu.downing.wavelet;

import java.util.ArrayList;

/**
 * 小波类，包含各种小波分解去噪重构函数
 * @author downing
 *
 */
public class Wavelet {
	/**
	 * 滤波器倒序函数，s为原始数组，返回的反转数组为r_s
	 * @param s
	 * @return r_s 
	 */
	public double[] reverse(double[] s){
		int length = s.length;
		double[] r_s = new double[length];
		for(int i=0;i<length;i++){
			r_s[length-i-1] = s[i];
		}
		return r_s;
	}

	/**
	 * 滤波器镜像函数，s为原始数组，返回到镜像数组为m_s
	 * @param s
	 * @return
	 */
	public double[] mirror(double[] s){
		int length = s.length;
		double[] m_s = new double[length];
		for(int i=0;i<length;i++){
			if(i%2==0){
				m_s[length-i-1] = -s[i];
			}else{
				m_s[length-i-1] = s[i];
			}
		}
		return m_s;
	}
	
	/**
	 * 对称延拓的长度
	 * @param sLEN 原序列长
	 * @param filterLEN 滤波器长度
	 * @return
	 */
	public int getDecLength(int sLEN,int filterLEN){
		if(sLEN%2==0){
			return sLEN+2*(filterLEN-2);
		}else{
			return sLEN+2*(filterLEN-2)+1;
		}
	}
	
	/**
	 * 小波分解函数传入低频滤波器则生成低频系数，传入高频滤波器生成小波系数
	 * @param s 原序列
	 * @param h 滤波器
	 * @return 分解后的序列
	 */
	public double[] wdtDec(double[] s,double[] h){
		double data;
		int sLength = s.length;
		int hLength = h.length;
		int decLength = getDecLength(sLength, hLength),p;
		double[] dec = new double[decLength];
		for(int n=1;n<=decLength;n++){
			dec[n-1] = 0.0f;
			for(int k=0;k<hLength;k++){
				p = 2*n-k-1;
				if((p<0) && (p>=-hLength+1))
					data = s[-p-1];
				else{
					if((p>sLength-1)&&(p<=sLength+hLength-2)){
						data = s[2*sLength-p-1];
					}else{
						if((p>=0)&& (p<=sLength-1))
							data = s[p];
						else
							data = 0;
					}
				}
				dec[n-1] += h[k]*data;	
			}
		}
		return dec;
	}
	
	/**
	 * 小波重构函数
	 * @param lo 第j+1层的低频序列 a(j+1)(n)
	 * @param hi 第j+1层的高频序列 d(j+1)(n)
	 * @param sLength 第j+1层序列长度
	 * @param lo_r 低频重构滤波器
	 * @param hi_r 高频重构滤波器
	 * @param filterLength 滤波器长度
	 * @param length 第j层序列长度
	 * @return 重构后第j层的序列a(j)(n)
	 */
	public double[] wdtRec(double[] lo,double[] hi,int sLength,double[] lo_r,double[] hi_r,int filterLength,int length){
		int rLength = 2*sLength + 1 - filterLength;
		double[] rec = new double[rLength];
		int p;
		for(int n=0;n<rLength;n++){
			rec[n] = 0.0f;
			for(int k=0;k<sLength;k++){
				p = n - 2*k + filterLength -2;
				if((p>=0)&&(p<filterLength))
					rec[n] += lo_r[p]*lo[k] + hi_r[p]*hi[k];
			}
		}
		double result[] = new double[length];
		for(int i=0;i<length;i++){
			result[i]=rec[i];
		}
		return result;
	}
	
	/**
	 * 小波N层分解
	 * @param s 原始信号
	 * @param h 低频滤波器
	 * @param g 高频滤波器
	 * @param n 分解层数
	 * @return 分解后各层小波系数和低频系数 
	 */
	public ArrayList<ArrayList<double[]>> wdtDecNLevel(double[] s,double[] h,double[] g,int n){
		ArrayList<double[]> high = new ArrayList<double[]>();
		ArrayList<double[]> lower = new ArrayList<double[]>();
		double[] source = s;
		for(int i=0;i<n;i++){
			if(i==0) 
				source = s;
			else 
				source = wdtDec(source,h);
			double[] a = wdtDec(source, h);
			double[] d = wdtDec(source, g);
			high.add(d);
			lower.add(a);
		}
		ArrayList<ArrayList<double[]>> result = new ArrayList<ArrayList<double[]>>();
		result.add(high);
		result.add(lower);
		return result;
	}
	
	/**
	 * 小波N层重构函数
	 * @param sequence 小波经分解出来后得到的各层低频及高频系数
	 * @param lo_r 低频重构滤波器
	 * @param hi_r 高频重构滤波器
	 * @param n 重构层数
	 * @param length 原始信号长度
	 * @return 重构后的信号
	 */
	public double[] wdtRecNLevel(ArrayList<ArrayList<double[]>> sequence,double[] lo_r,double[] hi_r,int n,int length){
		ArrayList<double[]> high = sequence.get(0);
		ArrayList<double[]> lower = sequence.get(1);
		int sLength=high.get(0).length,filterLength=lo_r.length;
		double[] a = lower.get(n-1);
		double[] source = a;
		for(int i=n;i>0;i--){
			double[] d = high.get(i-1);
			sLength = d.length;
			if(i!=1)
				source = wdtRec(source, d, sLength, lo_r, hi_r, filterLength,high.get(i-2).length);
			else
				source = wdtRec(source, d, sLength, lo_r, hi_r, filterLength,length);
		}
//		double[] result;
//		if(isEven)
//			result=new double[sLength-2*(filterLength-2)];
//		else
//			result = new double[sLength-2*(filterLength-2)-1];
//		for(int i=0;i<result.length;i++){
//			result[i]=source[filterLength-1+i];
//		}
		return source;
	}
	
	/**
	 * 小波N层去噪函数  选用特定的阈值函数以及阈值方法    启发式阈值 软阈值 分层不同阈值
	 * @param sequence 经过小波分解后的各层高频系数及低频系数
	 * @return 去噪后的高频系数及低频系数
	 */
	public ArrayList<ArrayList<double[]>> wdtDenoiseNLevel(ArrayList<ArrayList<double[]>> sequence,double[] s){
		ArrayList<double[]> waveletCoef = sequence.get(0);
		int level = waveletCoef.size();
		double[] coef;
		double threshold;
		int length;
		for(int i=0;i<level;i++){
			coef = waveletCoef.get(i);
			threshold = heursure(coef, s);
			length = coef.length;
			for(int j=0;j<length;j++){
				double temp = coef[j];
				if(Math.abs(temp)>=threshold){
					int k = temp>0?1:-1;
					coef[j]=k*(Math.abs(temp)-threshold);
				}else{
					coef[j]=0;
				}
			}
			waveletCoef.set(i, coef);
		}
		sequence.set(0, waveletCoef);
		return sequence;
	}
	
	/**
	 * 小波N层去噪
	 * @param s 原始信号
	 * @param n 分解层数
	 * @param waveletType 选取的小波类型
	 * @return 去噪后的信号
	 */
	public double[] waveletDenoise(double[] s,int n,WaveEnum waveletType){
		double[] lo_d;
		switch (waveletType) {
		case Haar:
			lo_d = WaveletConst.haar;
			break;
		case Db1:
			lo_d = WaveletConst.db1;
			break;
		case Db2:
			lo_d = WaveletConst.db2;
			break;
		case Db3:
			lo_d = WaveletConst.db3;
			break;
		case Db4:
			lo_d = WaveletConst.db4;
			break;
		case Db5:
			lo_d = WaveletConst.db5;
			break;
		case Db6:
			lo_d = WaveletConst.db6;
			break;
		case Db7:
			lo_d = WaveletConst.db7;
			break;
		case Sym1:
			lo_d = WaveletConst.sym1;
			break;
		case Sym2:
			lo_d = WaveletConst.sym2;
			break;
		case Sym3:
			lo_d = WaveletConst.sym3;
			break;
		case Sym4:
			lo_d = WaveletConst.sym4;
			break;
		case Sym5:
			lo_d = WaveletConst.sym5;
			break;
		case Sym6:
			lo_d = WaveletConst.sym6;
			break;
		case Sym7:
			lo_d = WaveletConst.sym7;
			break;
		case Coif1:
			lo_d = WaveletConst.coif1;
			break;
		case Coif2:
			lo_d = WaveletConst.coif2;
			break;
		case Coif3:
			lo_d = WaveletConst.coif3;
			break;
		case Coif4:
			lo_d = WaveletConst.coif4;
			break;
		case Coif5:
			lo_d = WaveletConst.coif5;
			break;
		case Bior1_1:
			lo_d = WaveletConst.bior1_1;
			break;
		case Bior1_3:
			lo_d = WaveletConst.bior1_3;
			break;
		case Bior1_5:
			lo_d = WaveletConst.bior1_5;
			break;
		case Bior2_2:
			lo_d = WaveletConst.bior2_2;
			break;
		case Bior2_4:
			lo_d = WaveletConst.bior2_4;
			break;
		case Bior2_6:
			lo_d = WaveletConst.bior2_6;
			break;
		case Bior2_8:
			lo_d = WaveletConst.bior2_8;
			break;
		default:
			lo_d = WaveletConst.coif5;
			break;
		}
		double[] lo_r = reverse(lo_d);
		double[] hi_r = mirror(lo_r);
		double[] hi_d = reverse(hi_r);
		ArrayList<ArrayList<double[]>> decomp = wdtDecNLevel(s,lo_d, hi_d, n);
		ArrayList<ArrayList<double[]>> denoise = wdtDenoiseNLevel(decomp, s);
		return wdtRecNLevel(denoise, lo_r, hi_r, n,s.length);
	}
	
	/**
	 * 固定阈值函数（Sqtwolog阈值）
	 * @param s 带噪信号
	 * @param n 经过分解得到的小波系数的个数
	 * @return 固定阈值函数的阈值
	 */
	public double sqtwolog(double[] s,int n){
		double mseValue = mse(s);
		return mseValue*Math.sqrt(2*Math.log(n));
	}
	
	/**
	 * Stein无偏似然估计阈值函数（Rigrsure阈值）
	 * @param waveletCoef 小波分解得到的系数
	 * @param s 原始带躁信号
	 * @return 无偏似然估计阈值
	 */
	public double rigrsure(double[] waveletCoef,double[] s){
		int n = waveletCoef.length;
		double[] w = new double[n];
		double[] r = new double[n];
		for(int i=0;i<n;i++){
			w[i] = waveletCoef[i]*waveletCoef[i];
		}
		w = sort(w);
		double temp;
		for(int j=0;j<n;j++){
			temp=0;
			for(int k=0;k<=j;k++){
				temp +=w[k];
			}
			r[j]=(n-2*(j+1)+(n-j-1)*w[j]+temp)/n;
		}
		int flag =0;
		for(int i=1;i<n;i++){
			double min=r[0];
			if(r[i]<min){
				min = r[i];
				flag = i;
			}
		}
		return mse(s)*Math.sqrt(w[flag]);
	}
	
	/**
	 * 启发式阈值函数（Heursure阈值）
	 * @param waveletCoef 小波分解后得到的系数
	 * @param s 原始信号
	 * @return 启发式阈值
	 */
	public double heursure(double[] waveletCoef,double[] s){
		double sum =0;
		int n = waveletCoef.length;
		for(int i=0;i<n;i++){
			sum += waveletCoef[i]*waveletCoef[i];
		}
		double yeta = (sum-n)/n;
		double u = Math.pow((Math.log(n)/Math.log(2)), 2*Math.sqrt(n)/3);
		if(yeta<=u)
			return sqtwolog(s, n);
		else{
			double temp1 = sqtwolog(s, n);
			double temp2 = rigrsure(waveletCoef, s);
			return temp1<temp2?temp1:temp2;
		}
	}
	
	/**
	 * 排序函数
	 * @param sequence 原始信号
	 * @return 排序后的信号
	 */
	public double[] sort(double[] sequence){
		int n = sequence.length,flag;
		double temp;
		for(int i=0;i<n-1;i++){
			double min = sequence[i];
			flag = i;
			for(int j=i+1;j<n;j++){
				if(sequence[j]<min){
					min=sequence[j];
					flag = j;
				}
			}
			if(flag!=i){
				temp=sequence[i];
				sequence[i] = sequence[flag];
				sequence[flag] = temp;
			}
		}
		return sequence;
	}
	
	/**
	 * 信号的均方差函数
	 * @param s 原始信号
	 * @return 信号的均方差
	 */
	public double mse(double[] s){
		int n = s.length;
		double result = 0;
		double mean = meanValue(s);
		for(int i=0;i<n;i++){
			result += (s[i]-mean)*(s[i]-mean);
		}
		result = result/n;
		result = Math.sqrt(result);
		return result;
	}
	
	/**
	 * 信号的均值函数
	 * @param s 原始信号
	 * @return 信号的均值
	 */
	public double meanValue(double[] s){
		int n = s.length ;
		double result = 0;
		for(int i=0;i<n;i++){
			result += s[i];
		}
		result = result/n;
		return result;
	}
}
