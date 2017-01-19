/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU MAY MODIFY THIS CLASS TO IMPLEMENT Stop & Wait ARQ PROTOCOL.
 *  (You will submit this class to Moodle.)
 *  
 */

package physical_network;

import java.util.Arrays;

/**
 * Encapsulates the data for a network 'data frame'.
 * At the moment this just includes a payload byte array.
 * This may need to be extended to include necessary header information.
 *
 * @author kevin-b
 */

public class DataFrame {

	private final static int HEADER_CHECKSUM = 3;
	private final static int PAYLOAD_CHECKSUM = 4;
	public final byte[] payload;
	private int destination = 0;
	private int source = 0;
	private byte[] header; // Consider changing to public like payload
	private int frameNumber;

	public DataFrame(String payload) {
		this.payload = payload.getBytes();
	}

	public DataFrame(String payload, int destination) {
		this.payload = payload.getBytes();
		this.destination = destination;
	}

	public DataFrame(byte[] payload) {
		this.payload = payload;
	}

	public DataFrame(byte[] payload, int destination) {
		this.payload = payload;
		this.destination = destination;
	}

	public int getDestination() {
		return destination;
	}

	public byte[] getPayload() {
		return payload;
	}

	public String toString() {
		return new String(payload);
	}

	/*
	 * A factory method that can be used to create a data frame
	 * from an array of bytes that have been received.
	 */
	public static DataFrame createFromReceivedBytes(byte[] byteArray) {

		DataFrame created = new DataFrame(byteArray);

		return created;
	}

	/*
	 * This method should return the byte sequence of the transmitted bytes.
	 * At the moment it is just the payload data ... but extensions should
	 * include needed header information for the data frame.
	 * Note that this does not need sentinel or byte stuffing
	 * to be implemented since this is carried out as the data
	 * frame is transmitted and received.
	 */
	public byte[] getTransmittedBytes() {
		byte[] frame = new byte[header.length + payload.length];
		System.arraycopy(header, 0, frame, 0, header.length);
		System.arraycopy(payload, 0, frame, header.length, payload.length);
		return frame;
	}

	private static byte checksum(byte[] buffer) {
		short sum = 0;
		int buffSize = buffer.length;
		for (int i = 0; i < buffSize; i++) {
			sum += buffer[i];
		}

		sum = (sum > 255) ? (short) (sum - 255) : sum;

		return (byte) ~sum;
	}

	public byte[] getHeader() {
		return header;
	}

	public void setHeader(int num) {
		this.frameNumber = num;
		makeHeader();
	}

	private void makeHeader() {
		header = new byte[PAYLOAD_CHECKSUM + 1];
		header[0] = (byte) source;
		header[1] = (byte) destination;
		header[2] = (byte) frameNumber;
		header[HEADER_CHECKSUM] = checksum(Arrays.copyOfRange(header, 0, 1));
		header[PAYLOAD_CHECKSUM] = checksum(payload);
	}

	public void setSource(int source) {
		this.source = source;
	}

	public boolean checkHeader(int dest, int expectedFrameNumber) {
		return (dest == header[1] &&
				expectedFrameNumber ==header[2] &&
				checksum(Arrays.copyOfRange(header, 0, 1)) == header[3] &&
				checksum(payload) == header[4])
				? true : false;
	}
}

