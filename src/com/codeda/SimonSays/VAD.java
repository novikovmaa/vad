package com.codeda.SimonSays;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class VAD {
	public static void main(String[] args) {
		// must match ffmpeg encoding speed, e.g.
		// ffmpeg -i audio.wav -ac 1 -ar 16000 -f s16le -acodec pcm_s16le -
		int freq = 16000; 		// Hz
		// duration of a piece
		int interval = 1000; 	// ms
		// rms of signal in 85..255 hz range over a piece
		// which is a threshold for voice detection
		double threshold = 0.01;		// -60 dB
		// number of samples in a piece
		int nSamples = freq*interval/1000;
		// counter of contiguous silence pieces up to current position
		int ctr=0;
		// sum of all silence intervals longer than silenceInterval seconds each
		int silence=0;
		// sum of all silent pieces
		int totalSilence=0;
		// seconds
		int silenceInterval=300;
		
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
				if (res==0) {
					ctr++;
					totalSilence++;
				} else {
					if (ctr>silenceInterval) {
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
        // in case audio ends with silence
		if (ctr>silenceInterval) {
			silence+=ctr;
		}
		t= System.currentTimeMillis()-t;
		System.out.println(t+";"+totalSilence+";"+silence);
	}
}
