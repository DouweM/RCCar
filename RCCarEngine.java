import java.io.*;

import lejos.robotics.navigation.*;
import lejos.nxt.comm.*;

import javax.bluetooth.*;

public class RCCarEngine {
	public static final byte COMMAND_SETSPEED	= 1 << 0; // Followed by speed: positive = forward, negative = backward
	public static final byte COMMAND_TRAVEL		= 1 << 1;
	public static final byte COMMAND_STEER		= 1 << 2; // Followed by turnRate: positive = right, negative = left
	public static final byte COMMAND_STOP		= 1 << 3;
	
	public static enum MoveType { TRAVEL, STEER, STOP };
	public static enum TravelDirection { FORWARD, BACKWARD, NONE };
	
	private boolean _connected 					= false;
	private BTConnection _connection 			= null;
	private RemoteDevice _remoteDevice 			= null;
	private DataInputStream _dataInStream 		= null;
	private DifferentialPilot _pilot 			= null;
	
	private MoveType _moveType 					= MoveType.STOP;
	private float _steerTurnRate 				= 0.0f;
	private TravelDirection _travelDirection 	= TravelDirection.NONE;
	
	public RCCarEngine(DifferentialPilot pilot) {
		_pilot = pilot;
	}
	
	public Boolean isConnected() {
		return _connected;
	}
	
	public RemoteDevice getRemoteDevice() {
		return _remoteDevice;
	}
	
	public void log(String message) {
		System.out.println("CC: " + message);
	}
	
	public BTConnection waitForConnection() {
		if (this.isConnected()) {
			this.log("We are already connected.");
			return null;
		}
		
		this.log("Waiting for connection...");
		 
		BTConnection connection = Bluetooth.waitForConnection();
		
		if (connection == null) {
			return null;
		}
		
		if (!this.useConnection(connection)) {
			return null;
		}
		
		return connection;
	}
	
	public boolean useConnection(BTConnection connection) {
		if (this.isConnected()) {
			this.log("We are already connected.");
			return false;
		}
		
		_connection = connection;
		
		_remoteDevice = null;
		_dataInStream = null;
		try {
			_remoteDevice = RemoteDevice.getRemoteDevice(_connection);
		    _dataInStream = _connection.openDataInputStream();
		} catch (IOException e) {
			this.log("Failed to get device, stream: " + e);
			_connection = null;
			return false;
		}
		
		_connected = true;
		
		return true;
	}
	
	public void closeConnection() {
		if (!this.isConnected()) {
			this.log("We are not actually connected.");
			return;
		}

		try {
			_dataInStream.close();
			_connection.close();
			
			this.reset();
		} catch (IOException e) {
			this.log("Failed to close connection: " + e);
			return;
		}
		
		_connected = false;
	}
	
	private void reset() {
		_dataInStream = null;
		_connection = null;
		_remoteDevice = null;
		
		_moveType = MoveType.STOP;
		_travelDirection = TravelDirection.NONE;
		_steerTurnRate = 0.0f;
	}
	
	public boolean waitForAndHandleCommand() {
		if (!this.isConnected()) {
			this.log("We are not actually connected.");
			return false;
		}
		
		this.log("Waiting for command...");
		
		byte command = 0;
		try {
			command = _dataInStream.readByte();
		} catch (EOFException e) {
			this.log("EOF reached.");
			return false;
		} catch (IOException e) {
			this.log("Failed to read command: " + e);
			return false;
		}
		
		this.log("Received command " + command);
		
		return handleCommand(command);
	}
	
	public boolean handleCommand(byte command) {
		switch (command) {
			case COMMAND_SETSPEED: {
				this.log("Waiting for speed...");
				
				float speed = -1;
				try {
					speed = _dataInStream.readFloat();
				} catch (IOException e) {
					this.log("Failed to read speed: " + e);
					return false;
				}
				
				this.log("Received speed " + speed);
				
				boolean success = this.doSetSpeed(speed);
				
				if (!success) {
					this.log("Failed to set speed.");
					return false;
				}

				break;
			}
			case COMMAND_TRAVEL: {
				boolean success = this.doTravel();
				
				if (!success) {
					this.log("Failed to travel.");
					return false;
				}
				
				break;
			}
			
			case COMMAND_STOP: {
				boolean success = this.doStop();
				
				if (!success) {
					this.log("Failed to stop.");
					return false;
				}
				
				break;
			}
			
			case COMMAND_STEER: {
				this.log("Waiting for turnRate...");
				
				float turnRate = 0;
				try {
					turnRate = _dataInStream.readFloat();
				} catch (IOException e) {
					this.log("Failed to read turnRate: " + e);
					return false;
				}
				
				this.log("Received turnRate " + turnRate);
				
				boolean success = this.doSteer(turnRate);
				
				if (!success) {
					this.log("Failed to steer.");
					return false;
				}
				
				break;
			}
			
			default: {
				this.log("Command not recognized: " + command);
				return false;
			}
		}
		
		this.log("Handled command " + command);
		return true;
	}
	
	private boolean doSetSpeed(float speed) {
		if (speed < 0) {
			_travelDirection = TravelDirection.BACKWARD;
		}
		else {
			_travelDirection = TravelDirection.FORWARD;
		}
		speed = Math.abs(speed);
		
		_pilot.setTravelSpeed(speed);
		
		if (_moveType == MoveType.STEER) {
			this.doSteer(_steerTurnRate);
		}
		else if (_moveType == MoveType.TRAVEL) {
			this.doTravel();
		}
		
		return true;
	}
	
	private boolean doTravel() {
		_moveType = MoveType.TRAVEL;

		if (_travelDirection == TravelDirection.FORWARD) {
			_pilot.forward();
		}
		else {
			_pilot.backward();
		}
		
		return true;
	}
	
	private boolean doSteer(float turnRate) {
		_moveType = MoveType.STEER;
		
		_steerTurnRate = turnRate;
		
		// direction: -1 = left, 1 = right
		int direction = (int) Math.signum(turnRate);
		
		// Absolute turnRate should be kept between 0.0 and 100.0
		turnRate = Math.abs(turnRate);
		turnRate = (float) Math.max(0.0f, (float) Math.min(100.0f, turnRate));
		
		// Our turnRate: positive => right
		// leJOS turnRate: negative => right
		turnRate = -1 * direction * turnRate;
		
		// turnRate: ratio of inside to outside motor: ratio = 1 - turnRate / 100
		// positive turnRate => left  motor drives the inside wheel => car turns left, forward  or right, backward
		// negative turnRate => right                                            right, forward or left, backward
		// turnRate == 0   => ratio = 1 - 0/100   = 1.0  => car travels in straight line
		// turnRate == 100 => ratio = 1 - 100/100 = 0.0  => inside motor stops
		// turnRate == 200 => ratio = 1 - 200/100 = -1.0 => car turns in place

		if (_travelDirection == TravelDirection.FORWARD) {
			_pilot.steer(turnRate);
		}
		else {
			_pilot.steerBackward(turnRate);
		}
		
		return true;
	}
	
	private boolean doStop() {
		_moveType = MoveType.STOP;
		
		_pilot.stop();
		
		return true;
	}
}
