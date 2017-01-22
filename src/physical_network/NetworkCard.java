/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU MAY MODIFY THIS CLASS TO IMPLEMENT Stop & Wait ARQ PROTOCOL.
 *  (You will submit this class to Moodle.)
 *
 */

package physical_network;

import java.util.Arrays;
import java.util.concurrent.*;


/**
 * Represents a network card that can be attached to a particular wire.
 * <p>
 * It has only two key responsibilities:
 * i) Allow the sending of data frames consisting of arrays of bytes using send() method.
 * ii) Receives data frames into an input queue with a receive() method to access them.
 *
 * @author K. Bryson
 */

public class NetworkCard {

	// Wire pair that the network card is attached to.
	private final TwistedWirePair wire;

	// Unique device number and name given to the network card.
	private final int deviceNumber;
	private final String deviceName;

	// Default values for high, low and mid- voltages on the wire.
	private final double HIGH_VOLTAGE = 2.5;
	private final double LOW_VOLTAGE = -2.5;

	// Default value for a signal pulse width that should be used in milliseconds.
	private final int PULSE_WIDTH = 200;

	// Default value for maximum payload size in bytes.
	private final int MAX_PAYLOAD_SIZE = 1500;

	// Default value for input & output queue sizes.
	private final int QUEUE_SIZE = 5;

	private final int TIMEOUT = 10000;

	private int framesSent = 0;

	private int framesReceived = 0;

	// Output queue for dataframes being transmitted.
	private LinkedBlockingQueue<DataFrame> outputQueue = new LinkedBlockingQueue<DataFrame>(QUEUE_SIZE);

	// Input queue for dataframes being received.
	private LinkedBlockingQueue<DataFrame> inputQueue = new LinkedBlockingQueue<DataFrame>(QUEUE_SIZE);

	// Transmitter thread.
	private Thread txThread;

	// Receiver thread.
	private Thread rxThread;

	/*
	 * Initial value is the destination of the acknowledgement. 0 implies no acknowledgement to be sent.
	 * Second value is the sequence number.
	 */
	private byte[] ackToSend = {0, 0};
	private boolean ackReceived = false;

	private static final int MAX_TRANSMISSIONS = 5;

	/*
	 * NetworkCard constructor.
	 *
	 * @param deviceName This provides the name of this device, i.e. "Network Card A".
	 * @param wire       This is the shared wire that this network card is connected to.
	 * @param listener   A data frame listener that should be informed when data frames are received.
	 *                   (May be set to 'null' if network card should not respond to data frames.)
	 */
	public NetworkCard(int number, TwistedWirePair wire) {

		this.deviceNumber = number;
		this.deviceName = "NetCard" + number;
		ackToSend[1] = (byte) deviceNumber;
		this.wire = wire;

		txThread = this.new TXThread();
		rxThread = this.new RXThread();
	}

	/*
	 * Initialize the network card.
	 */
	public void init() {
		txThread.start();
		rxThread.start();
	}


	public void send(DataFrame data) throws InterruptedException {
		data.setSource(deviceNumber);
		outputQueue.put(data);
	}

	public DataFrame receive() throws InterruptedException {
		DataFrame data = inputQueue.take();
		return data;
	}


	/*
	 * Private inner thread class that transmits data.
	 */
	private class TXThread extends Thread {
		private int sendAttempts = 0;

		public void run() {
			try {
				while (true) {
//					System.out.println(deviceNumber + " " + ackToSend[0]);

					// Blocks if nothing is in queue.
					DataFrame frame = outputQueue.take();

					if (ackToSend[0] == 0)
						frame.setHeader(++framesSent);
					else {
						System.out.println(deviceNumber + " - sending ack");
					}
					do {
//						System.out.println(deviceNumber + " - Frame no: " + framesSent);
						transmitFrame(frame);

						if (ackToSend[0] != 0) {
							ackToSend[0] = 0;
							break;
						} else {
							sendAttempts++;
							if (sendAttempts > MAX_TRANSMISSIONS)
								break;
						}

					} while (waitingForAcknowledgement());
					if (sendAttempts > MAX_TRANSMISSIONS) {
						System.out.println(deviceNumber + " - " + MAX_TRANSMISSIONS + " transmissions attempted. Terminating transmission.");
						break;
					}
				}
			} catch (InterruptedException except) {
				System.out.println(deviceName + " Transmitter Thread Interrupted - terminated.");
			}

		}

		/*
		 * Sleeps thread until woken by rxThread confirming an acknowledgement
		 * or timeout period has passed.
		 */
		private synchronized boolean waitingForAcknowledgement() {
			System.out.println(deviceNumber + " - waiting for ack..");
			long time = System.currentTimeMillis();
			while (!ackReceived) {
				try {
					wait(TIMEOUT);
					if (System.currentTimeMillis() - time > TIMEOUT) {
						System.out.println(deviceNumber + " - No ack.. Resending...");
						return true;
					}
				} catch (InterruptedException e) {

				}
			}
			System.out.println(deviceNumber + " - Ack received");
			sendAttempts = 0;
			ackReceived = false;
			return false;
		}

		/**
		 * Tell the network card to send this data frame across the wire.
		 * NOTE - THIS METHOD ONLY RETURNS ONCE IT HAS TRANSMITTED THE DATA FRAME.
		 *
		 * @param frame Data frame to transmit across the network.
		 */
		public void transmitFrame(DataFrame frame) throws InterruptedException {

			if (frame != null) {

				// Low voltage signal to get ready ...
				wire.setVoltage(deviceName, LOW_VOLTAGE);
				sleep(PULSE_WIDTH * 4);

				byte[] payload = frame.getTransmittedBytes();

				// Send bytes in asynchronous style with 0.2 seconds gaps between them.
				for (int i = 0; i < payload.length; i++) {

					// Byte stuff if required.
					if (payload[i] == 0x7E || payload[i] == 0x7D)
						transmitByte((byte) 0x7D);

					transmitByte(payload[i]);
				}

				// Append a 0x7E to terminate frame.
				transmitByte((byte) 0x7E);

//				sleep(PULSE_WIDTH * 2);
				wire.setVoltage(deviceName, 0);
				sleep(PULSE_WIDTH);
			}

		}

		private void transmitByte(byte value) throws InterruptedException {

			// Low voltage signal ...
			wire.setVoltage(deviceName, LOW_VOLTAGE);
			sleep(PULSE_WIDTH * 4);

			// Set initial pulse for asynchronous transmission.
			wire.setVoltage(deviceName, HIGH_VOLTAGE);
			sleep(PULSE_WIDTH);

			// Go through bits in the value (big-endian bits first) and send pulses.

			for (int bit = 0; bit < 8; bit++) {
				if ((value & 0x80) == 0x80) {
					wire.setVoltage(deviceName, HIGH_VOLTAGE);
				} else {
					wire.setVoltage(deviceName, LOW_VOLTAGE);
				}

				// Shift value.
				value <<= 1;

				sleep(PULSE_WIDTH);
			}
		}

	}

	/*
	 * Private inner thread class that receives data.
	 */
	private class RXThread extends Thread {

		public void run() {

			try {

				// Listen for data frames.

				while (true) {

					byte[] bytePayload = new byte[MAX_PAYLOAD_SIZE];
					int bytePayloadIndex = 0;
					byte receivedByte;

					while (true) {
						receivedByte = receiveByte();

						if ((receivedByte & 0xFF) == 0x7E)
							break;

						System.out.println(deviceName + " RECEIVED BYTE = " + Integer.toHexString(receivedByte & 0xFF));

						// Unstuff if escaped.
						if (receivedByte == 0x7D) {
							receivedByte = receiveByte();
							System.out.println(deviceName + " ESCAPED RECEIVED BYTE = " + Integer.toHexString(receivedByte & 0xFF));
						}

						bytePayload[bytePayloadIndex] = receivedByte;
						bytePayloadIndex++;
					}
					// Block receiving data if queue full.
					checkFrameData(bytePayloadIndex, bytePayload);
				}

			} catch (InterruptedException except) {
				System.out.println(deviceName + " Interrupted: " + getName());
			}

		}

		/*
		 * Checks received data is a valid frame or acknowledgement
		 */
		private void checkFrameData(int bytePayloadIndex, byte[] bytePayload) throws InterruptedException {
			if (bytePayloadIndex == 2) {
				System.out.println(deviceNumber + " - Putting together ack...");
				byte[] ack = Arrays.copyOfRange(bytePayload, 0, bytePayloadIndex);
				if (ack[0] == deviceNumber && ack[1] == framesSent)
					receivedAck();
			} else if (bytePayloadIndex > 4) {
				System.out.println(deviceNumber + " - Frame received");
				DataFrame newFrame = new DataFrame(Arrays.copyOfRange(bytePayload, 0, bytePayloadIndex));
				if (newFrame.checkHeader(deviceNumber, framesReceived + 1)) {
					System.out.println(deviceNumber + " - Valid frame");
					framesReceived++;
					sendAcknowledgement(newFrame.getHeader()[0]);
					if (!inputQueue.contains(newFrame))
						inputQueue.put(newFrame);
				} else
					System.out.println(deviceNumber + " - Corrupt frame");
			}
		}

		/*
		 * Wakes txThread after confirming acknowledgement is received.
		 */
		private void receivedAck() {
			System.out.println(deviceNumber + " - Announcing ack received.");
			ackReceived = true;
			synchronized (txThread) {
				txThread.notify();
			}
		}

		public byte receiveByte() throws InterruptedException {

			double upperThresholdVoltage = HIGH_VOLTAGE + LOW_VOLTAGE / 3;
			double lowerThresholdVoltage = LOW_VOLTAGE + HIGH_VOLTAGE / 3;
			byte value = 0;

			while (!checkByteStart(upperThresholdVoltage, lowerThresholdVoltage)) ;

			// Sleep till middle of next pulse.
			sleep(PULSE_WIDTH + PULSE_WIDTH / 4);

			// Use 8 next pulses for byte.
			for (int i = 0; i < 8; i++) {
//				System.out.println(deviceNumber + " - i: " + i);
				value *= 2;

				if (wire.getVoltage(deviceName) > 0) {
					value += 1;
				}

				sleep(PULSE_WIDTH);
			}

			return value;
		}

		/*
		 * Checks for when a valid byte is about to be sent.
		 * Prevents false results during timeout period from being read.
		 */
		private boolean checkByteStart(double upperV, double lowerV) throws InterruptedException {
			while (wire.getVoltage(deviceName) > lowerV) {
				sleep(PULSE_WIDTH / 10);
			}

			int i = 0;
			while (wire.getVoltage(deviceName) < lowerV && i < 3) {
				i++;
				sleep(PULSE_WIDTH);
			}

			if (i == 3) {
				while (wire.getVoltage(deviceName) < upperV) {
					sleep(PULSE_WIDTH / 10);
				}
				return true;
			} else
				return false;
		}

		/*
		 * Makes acknowledgement and adds it to outputQueue for txThread to send.
		 * Makes a frame consisting of 2 bytes.
		 */
		private void sendAcknowledgement(int dest) throws InterruptedException {
			ackToSend[0] = (byte) dest;
			ackToSend[1] = (byte) framesReceived;
			DataFrame dataFrame = new DataFrame();
			dataFrame.setHeader(ackToSend);
			dataFrame.setIsAck(true);
			outputQueue.put(dataFrame);
		}

	}


}
