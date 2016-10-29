package com.codeda.SimonSays;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class VAD {
	public static void main(String[] args) {
		int freq = 16000; 		// Hz
		int interval = 1000; 	// ms
		double threshold = 0.01;		// -60 dB
		
		int nSamples = freq*interval/1000;
		int ctr=0;
		int silence=0;
		int totalSilence=0;
		
		short[] shortArray = new short[nSamples];
		float[] doubleArray = new float[nSamples];
		long t=System.currentTimeMillis();
        try {
        	RandomAccessFile aFile = new RandomAccessFile("C:\\1\\out.pcm", "r");
        	FileChannel inChannel = aFile.getChannel();
        	ByteBuffer buffer = ByteBuffer.allocate(nSamples*2);
			while(inChannel.read(buffer) > 0)
			{
				// convert array into array of signed shorts
			    buffer.flip();
			    int l = buffer.limit();
			    if (l!=nSamples*2) {
			    	break;
			    }
				buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);
				for (int i = 0; i < l/2; i++)
			    {
					doubleArray[i] = shortArray[i];
					doubleArray[i]/=32768.0;
			    }
				// filter out unwanted frequencies
				Filter filter = new Filter(85,16000, Filter.PassType.Highpass,1);
				
			    for (int i = 0; i < l/2; i++)
			    {
			        filter.Update(doubleArray[i]);
			        doubleArray[i] = filter.getValue();
			    }
				Filter filter2 = new Filter(255,16000, Filter.PassType.Lowpass,1);
			    for (int i = 0; i < l/2; i++)
			    {
			        filter2.Update(doubleArray[i]);
			        doubleArray[i] = filter2.getValue();
			    }
				double rms=0;
				for (int i=0;i<l/2;i++) {
					rms+=doubleArray[i]*doubleArray[i];
				}
				rms/=(l/2);
				rms=Math.sqrt(rms);
				int res =0;
				if (rms>threshold) res=1;
				System.out.println(rms+";"+res);
				if (res==0) {
					ctr++;
					totalSilence++;
				} else {
					if (ctr>300) {
						silence+=ctr;
					}
					ctr=0;
				}
			    buffer.clear(); // do something with the data and clear/compact it.
			}
	        inChannel.close();
	        aFile.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
		t= System.currentTimeMillis()-t;
		System.out.println(t+";"+totalSilence+";"+silence);
	}
}
