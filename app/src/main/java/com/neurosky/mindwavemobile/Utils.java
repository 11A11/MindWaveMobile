package com.neurosky.mindwavemobile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Mukesh on 11/3/2016.
 */
public class Utils {

    public static boolean autoBond(Class btClass,BluetoothDevice device,String strPin) throws Exception {
    	Method autoBondMethod = btClass.getMethod("setPin",new Class[]{byte[].class});
    	Boolean result = (Boolean)autoBondMethod.invoke(device,new Object[]{strPin.getBytes()}); 
    	return result;
    }
    public static boolean createBond(Class btClass,BluetoothDevice device) throws Exception {
    	Method createBondMethod = btClass.getMethod("createBond"); 
    	Boolean returnValue = (Boolean) createBondMethod.invoke(device);
    	return returnValue.booleanValue();
    }
	public static  int getRawWaveValue(byte highOrderByte, byte lowOrderByte)
	 {
		   int hi = ((int)highOrderByte)& 0xFF;
		   int lo = ((int)lowOrderByte) & 0xFF;
		   return( (hi<<8) | lo );
	 }
    
	public static String byte2String( byte[] b) {  
		StringBuffer sb = new StringBuffer();
		   for (int i = 0; i < b.length; i++) { 
		     String hex = Integer.toHexString(b[i] & 0xFF); 
		     if (hex.length() == 1) { 
		       hex = '0' + hex; 
		     } 
		     sb.append(hex);
		   } 
		   return sb.toString().toLowerCase();
		}

	public static byte[] compressToByte(final String data, final String encoding) throws IOException {
		if (data == null || data.length() == 0)
		{
			return null;
		}
		else
		{
			byte[] bytes = data.getBytes(encoding);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream os = new GZIPOutputStream(baos);
			os.write(bytes, 0, bytes.length);
			os.close();
			byte[] result = baos.toByteArray();
			return result;
		}
	}

	public static String unCompressString(final byte[] data, final String encoding) throws IOException {
		if (data == null || data.length == 0)
		{
			return null;
		}
		else
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			GZIPInputStream is = new GZIPInputStream(bais);
			byte[] tmp = new byte[256];
			while (true)
			{
				int r = is.read(tmp);
				if (r < 0)
				{
					break;
				}
				buffer.write(tmp, 0, r);
			}
			is.close();

			byte[] content = buffer.toByteArray();
			return new String(content, 0, content.length, encoding);
		}
	}
}
