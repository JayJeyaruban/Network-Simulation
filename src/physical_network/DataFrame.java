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

	private final static int HEADER_CHECKSUM_INDEX = 3;
	private final static int PAYLOAD_CHECKSUM_INDEX = 4;
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
		this.header = Arrays.copyOfRange(input, 0, PAYLOAD_CHECKSUM_INDEX + 1);
		this.payload = Arrays.copyOfRange(input, PAYLOAD_CHECKSUM_INDEX + 1, input.length);
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

	private static byte checksum(byte[] buffer) {
		short sum = 0;
		int buffSize = buffer.length;
		for (int i = 0; i < buffSize; i++) {
			sum += buffer[i];
		}

		sum = (sum > 255) ? (short) ((sum  & 0xFF) + 1) : sum;

		return (byte) ~sum;
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
		header = new byte[PAYLOAD_CHECKSUM_INDEX + 1];
		header[0] = (byte) source;
		header[1] = (byte) destination;
		header[2] = (byte) frameNumber;
		header[HEADER_CHECKSUM_INDEX] = checksum(Arrays.copyOfRange(header, 0, HEADER_CHECKSUM_INDEX));
		header[PAYLOAD_CHECKSUM_INDEX] = checksum(payload);
	}

	public void setSource(int source) {
		this.source = source;
	}

	public boolean checkHeader(int dest, int expectedFrameNumber) {
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
//
//		if (checksum(Arrays.copyOfRange(header, 0, HEADER_CHECKSUM_INDEX)) == header[HEADER_CHECKSUM_INDEX])
//			System.out.println("Right header checksum");
//		else
//			System.out.println("Wrong header checksum");

//		if (checksum(payload) == header[PAYLOAD_CHECKSUM_INDEX]);
////			System.out.println("Right payload checksum");
//		else {
////			System.out.println("Wrong payload checksum");
//			System.out.println(checksum(payload) + " " + header[PAYLOAD_CHECKSUM_INDEX]);
//			System.out.println(Arrays.toString(payload));
//		}

		return (dest == header[1] &&
				expectedFrameNumber == header[2] &&
				checksum(Arrays.copyOfRange(header, 0, HEADER_CHECKSUM_INDEX)) == header[HEADER_CHECKSUM_INDEX] &&
				checksum(payload) == header[PAYLOAD_CHECKSUM_INDEX]);

	}
}

