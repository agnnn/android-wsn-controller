package de.rwth.comsys.enums;

public enum MSP430Variant
{
	MSP430_F149(0xF149), MSP430_F449(0xF449), MSP430_F2131(0xF19), MSP430_F161x(0xF16C), MSP430_F16x(0xF169), MSP430_F42x0(
			0xF427), MSP430_41x2(0x4152), MSP430_47197(0xF47F);

	private int version;




	public static MSP430Variant getDeviceVersion(byte hByte, byte lByte)
	{
		int tmpVersion = (hByte & 0xFF);
		int requestVersion = (tmpVersion << 8) | (lByte & 0xFF);
		for (MSP430Variant device : MSP430Variant.values())
		{

			if (device.getVersion() == requestVersion)
			{
				return device;
			}
		}
		return null;
	}




	private int getVersion()
	{
		// TODO Auto-generated method stub
		return this.version;
	}




	private MSP430Variant(int version)
	{
		this.version = version;
	}
}
