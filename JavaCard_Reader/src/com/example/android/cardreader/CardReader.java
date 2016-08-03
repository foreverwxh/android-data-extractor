
package com.example.android.cardreader;

import java.io.IOException;
import java.util.Arrays;

import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Parcelable;
import android.util.Log;


public final class CardReader {
    private static final String TAG = "LoyaltyCardReader";
    // AID for our loyalty card service.
    private static final String SAMPLE_CARD_AID = "1122001122";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};
    //自定义的命令
    private static final String[] SAMPLE_COMMAND={"8010000000",//卡片收到后返回"Hello"
    	"8020000000"};//卡片收到后返回"World"
    
	public static String[][] TECHLISTS;
	public static IntentFilter[] FILTERS;

	static {
		try {
			//the tech lists used to perform matching for dispatching of the ACTION_TECH_DISCOVERED intent
			TECHLISTS = new String[][] { { IsoDep.class.getName() }};

			FILTERS = new IntentFilter[] { new IntentFilter(
					NfcAdapter.ACTION_TECH_DISCOVERED, "*/*") };
		} catch (Exception e) {
		}
	}
    
	static public String tagDiscovered(Tag tag) {
		Log.e(TAG, "New tag discovered");

		String strResult="";
		IsoDep isoDep = IsoDep.get(tag);
		if (isoDep != null) {
			try {
				// Connect to the remote NFC device
				isoDep.connect();

				//发送select 命令,卡片会返回SELECT_OK_SW（90 00）
				byte[] cmdSelect = BuildSelectApdu(SAMPLE_CARD_AID);
				Log.e(TAG, "Sending: " + ByteArrayToHexString(cmdSelect));
				byte[] result = isoDep.transceive(cmdSelect);
				Log.e(TAG, "Receive: " + ByteArrayToHexString(result));
				byte[][] response = getResponse(result);
				byte[] statusWord =response[0];

				if (Arrays.equals(SELECT_OK_SW, statusWord) == false)
					return "";

				//循环发送自定义命令
				for(int i=0;i<SAMPLE_COMMAND.length;i++){
					String command = SAMPLE_COMMAND[i];
					result = HexStringToByteArray(command);
					Log.e(TAG, "Sending: " + command);
					
					result = isoDep.transceive(result);
					Log.e(TAG, "Receive: " + ByteArrayToHexString(result));
					response = getResponse(result);
					byte[] body =response[1];
					
					strResult=strResult+new String(body)+":"+ByteArrayToHexString(body)+"\r\n";
				}
				

				return strResult;

			} catch (IOException e) {
				Log.e(TAG, "Error communicating with card: " + e.toString());
			}
		}
		return null;
	}

	/***
	 * 分解卡片返回的数据
	 * @param b
	 * @return [0]表示返回的状态值，[1]表示返回的正文
	 */
	private static byte[][] getResponse(byte[] b){
		byte[][] result = new byte[2][];
		
		int length = b.length;
		byte[] status = { b[length - 2],b[length - 1] };
		
		byte[] body = Arrays.copyOf(b, length - 2);

		result[0]=status;
		result[1]=body;
		return result;
	}
	
	public static String load(Parcelable parcelable) {
		// 从Parcelable筛选出各类NFC标准数据
		final Tag tag = (Tag) parcelable;
		return tagDiscovered(tag);
	}


    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
    }

    /**
     * Utility class to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Utility class to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     */
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
