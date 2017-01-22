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

	private final static int CHECKSUM_INDEX = 3;
	private final static int HEADER_INDEX = 4;
	private byte[] payload;
	private int destination = 0;
	private int source = 0;
	private byte[] header; // Consider changing to public like payload
	private int frameNumber;
	private boolean isAck = false;

	public DataFrame(String payload) {
		this.payload = payload.getBytes();
	}

	public DataFrame(String payload, int destination) {
		this.payload = payload.getBytes();
		this.destination = destination;
	}

	public DataFrame(byte[] input) {
		this.header = Arrays.copyOfRange(input, 0, HEADER_INDEX + 1);
		this.payload = Arrays.copyOfRange(input, HEADER_INDEX + 1, input.length);
	}

	public DataFrame() {

	}

	public void setIsAck(boolean ack) {
		this.isAck = ack;
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
		byte[] frame = new byte[header.length + ((isAck) ? 0 : payload.length)];
		System.arraycopy(header, 0, frame, 0, header.length);
		if (!isAck)
			System.arraycopy(payload, 0, frame, header.length, payload.length);
		return frame;
	}

	private static byte[] checksum(byte[] buffer) {
		short sum = 0;
		for (byte b : buffer)
			sum += b;

		sum = (sum > 255) ? (short) ((sum  & 0xFFFF) + 1) : sum;

		byte[] output = {
				(byte) ((sum >> 8) & 0xff),
				(byte) (sum & 0xff)
		};

		return output;
	}

	public byte[] getHeader() {
		return header;
	}

	public void setHeader(byte[] bytes) {
		this.header = bytes;
	}

	// TODO: 20/01/2017 Consider getting rid of this method
	public void setHeader(int num) {
		this.frameNumber = num;
		makeHeader();
	}

	private void makeHeader() {
		header = new byte[HEADER_INDEX + 1];
		header[0] = (byte) source;
		header[1] = (byte) destination;
		header[2] = (byte) frameNumber;

		byte[] frameNoChecksum = new byte[CHECKSUM_INDEX + payload.length];
		System.arraycopy(header, 0, frameNoChecksum, 0, CHECKSUM_INDEX);
		System.arraycopy(payload, 0, frameNoChecksum, CHECKSUM_INDEX, payload.length);
		byte[] checksum = checksum(frameNoChecksum);

		header[CHECKSUM_INDEX] = checksum[0];
		header[HEADER_INDEX] = checksum[1];
	}

	public void setSource(int source) {
		this.source = source;
	}

	public synchronized boolean checkHeader(int dest, int expectedFrameNumber) {
		byte[] frameNoChecksum = new byte[header.length - 1 + payload.length];
		System.arraycopy(header, 0, frameNoChecksum, 0, header.length - 1);
		System.arraycopy(payload, 0, frameNoChecksum, header.length - 1, payload.length);
		byte[] checksum = checksum(frameNoChecksum);

//		System.out.println(header.length);
//
//		if (dest == header[1])
//			System.out.println("Right dest");
//		else
//			System.out.println("Wrong dest");
//
//		if (expectedFrameNumber == header[2])
//			System.out.println("Right frame");
//		else
//			System.out.println("Wrong frame");
// TODO: 22/01/2017 Rewrite checksum debug prints

		return (dest == header[1] &&
				expectedFrameNumber == header[2] &&
				checksum[0] == header[CHECKSUM_INDEX] &&
				checksum[1] == header[HEADER_INDEX]);

	}
}

