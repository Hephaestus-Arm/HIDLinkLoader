@GrabResolver(name='nr', root='https://oss.sonatype.org/service/local/repositories/releases/content/')
@GrabResolver(name='mvnRepository', root='https://repo1.maven.org/maven2/')
@Grab(group='com.neuronrobotics', module='SimplePacketComsJava', version='1.0.0')

import Jama.Matrix;
import edu.wpi.SimplePacketComs.*;
import edu.wpi.SimplePacketComs.phy.*;

import com.neuronrobotics.bowlerstudio.BowlerStudio
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.AbstractRotoryLink
import com.neuronrobotics.sdk.addons.kinematics.INewLinkProvider
import com.neuronrobotics.sdk.addons.kinematics.LinkConfiguration
import com.neuronrobotics.sdk.addons.kinematics.LinkFactory
import com.neuronrobotics.sdk.addons.kinematics.imu.*;
import com.neuronrobotics.sdk.common.DeviceManager

import edu.wpi.SimplePacketComs.BytePacketType;
import edu.wpi.SimplePacketComs.FloatPacketType;
import edu.wpi.SimplePacketComs.*;
import edu.wpi.SimplePacketComs.phy.UDPSimplePacketComs;
import edu.wpi.SimplePacketComs.device.gameController.*;
import edu.wpi.SimplePacketComs.device.*

import java.util.Arrays;

import edu.wpi.SimplePacketComs.FloatPacketType;
import edu.wpi.SimplePacketComs.PacketType;
public class NumOfPID {
	private int myNum = -1;

	public int getMyNum() {
		return myNum;
	}

	public void setMyNum(int myNum) {
		this.myNum = myNum;
	}
}

public class UDP7DOf  extends UDPSimplePacketComs{
	FloatPacketType setSetpoint = new FloatPacketType(1848, 64);
	FloatPacketType pidStatus = new FloatPacketType(1910, 64);

	NumOfPID myNum = new NumOfPID();
	String name="test"
	
	public UDP7DOf(def address) throws Exception {
		super(address);
		if(address==null)
			setVirtual(true)
		setupPidCommands(7);
		connect();
		//		if(isVirtual())
		//			throw new RuntimeException("Device is virtual!");
	}
	void setupPidCommands(int numPID) {
		//new Exception().printStackTrace();
		myNum.setMyNum(numPID);
		setSetpoint.waitToSendMode();
		for (PacketType pt : Arrays.asList(pidStatus,  setSetpoint)) {
			addPollingPacket(pt);
		}
		
//		addEvent(1910,{
//			print "\n[ ";
//			for(int i=0;i<numPID;i++){
//				print getPidPosition(i);
//				if(i!=numPID-1)
//					print " , ";
//			}
//			print " ] ";
//		})
	}

	public double getNumPid() {
		return myNum.getMyNum();
	}


	public double getPidSetpoint(int index) {

		return pidStatus.getUpstream()[1 + index * 2 + 0].doubleValue();
	}

	public double getPidPosition(int index) {
		if(isVirtual()) {
			def val=setSetpoint.getDownstream()[index+2].doubleValue()
			//println "Virtual getPosition "+index+" "+val
			return val;
		}
		return pidStatus.getUpstream()[1 + index * 2 + 1].doubleValue();
	}
	public void setPidSetpoints(int msTransition, int mode, double[] data) {
		def down = new double[2 + getMyNumPid()];
		down[0] = msTransition;
		down[1] = mode;
		for (int i = 0; i < getMyNumPid(); i++) {
			down[2 + i] = data[i];
		}
		writeFloats(setSetpoint.idOfCommand, down);
		setSetpoint.pollingMode();

	}

	public void setPidSetpoint(int msTransition, int mode, int index, double data) {

		double[] cur = new double[getMyNumPid()];
		for (int i = 0; i < getMyNumPid(); i++) {
			if (i == index)
				cur[index] = data;
			else
				cur[i] = setSetpoint.getDownstream()[i+2].doubleValue()
		}
		cur[index] = data;
		setPidSetpoints(msTransition, mode, cur);

	}

	
	public int getMyNumPid() {
		return myNum.getMyNum();
	}

	public void setMyNumPid(int myNumPid) {
		if (myNumPid > 0)
			myNum.setMyNum(myNumPid);
		throw new RuntimeException("Can not have 0 PID");
	}

	public void stop(int currentIndex) {
		setPidSetpoint(0, 0, currentIndex, getPidPosition(currentIndex));
	}
	@Override
	public String toString() {
		return getName();
	}
	
	void setName(String n) {
		name=n;
	}
	
	String getName() {
		return name;
	}
}


public class UDPRotoryLink extends AbstractRotoryLink{
	UDP7DOf device;
	int index =0;
	int lastPushedVal = Integer.MAX_VALUE;
	/**
	 * Instantiates a new HID rotory link.
	 *
	 * @param c the c
	 * @param conf the conf
	 */
	public UDPRotoryLink(UDP7DOf c,LinkConfiguration conf) {
		super(conf);
		conf.setDeviceTheoreticalMax(180);
		conf.setDeviceTheoreticalMin(-180);

		index = conf.getHardwareIndex()
		device=c
		if(device ==null)
			throw new RuntimeException("Device can not be null")
		c.addEvent(1910,{
			int val= getCurrentPosition();
			if(lastPushedVal!=val){
				//println " Status packet: "+c.pidStatus.getUpstream()
				//println "Fire Link Listner "+index+" value "+getCurrentPosition()
				try {
					fireLinkListener(getCurrentPosition());
				}catch(Throwable t) {
					t.printStackTrace()
//					BowlerStudio.printStackTrace(t)
				}
			}
			lastPushedVal=val
		})

	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.addons.kinematics.AbstractLink#cacheTargetValueDevice()
	 */
	@Override
	public void cacheTargetValueDevice() {
		device.setPidSetpoint(0,0,index,(float)getTargetValue()*100.0)
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.addons.kinematics.AbstractLink#flush(double)
	 */
	@Override
	public void flushDevice(double time) {
		// auto flushing
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.addons.kinematics.AbstractLink#flushAll(double)
	 */
	@Override
	public void flushAllDevice(double time) {
		// auto flushing
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.addons.kinematics.AbstractLink#getCurrentPosition()
	 */
	@Override
	public double getCurrentPosition() {
		return device.getPidPosition(index)/100.0;
	}

}

UDP7DOf getDevice(LinkConfiguration conf) {
	String searchName = conf.getDeviceScriptingName();

	UDP7DOf dev= DeviceManager.getSpecificDevice( searchName,{
		//If the device does not exist, prompt for the connection
		def simp = null;
		HashSet<InetAddress> addresses = UDPSimplePacketComs.getAllAddresses(searchName);
		println "loadUDP.groovy: Searched for "+searchName+" and found addresses:"+addresses
		if (addresses.size() >= 1){
			
			UDP7DOf d = new UDP7DOf(addresses.toArray()[0])
			d.setName(searchName);
			println "Creating UDP7DOf "+d
			return d	
		}
		return new UDP7DOf(null);
	})
	println "Device created "+dev
	return DeviceManager.getSpecificDevice( searchName)
}

INewLinkProvider provider= new INewLinkProvider() {
	public AbstractLink generate(LinkConfiguration conf) {
		return new UDPRotoryLink(getDevice(conf),conf);
	}
}

if(args==null)
	args=["udp-7dof-float"]
LinkFactory.addLinkProvider(args[0], provider)
return provider


