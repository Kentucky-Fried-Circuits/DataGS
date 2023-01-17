File Descriptions
=================
- RecordMagWeb.java class definition for RecordMagWeb. Container for MagWeb data and methods for parsing Magnum Network packets.
- RecordMagWebVariable.java class definition for RecordMagWebVariable. Methods for parsing Magnum Network data blocks.
- RecordMagWebVariable_DataBlock.java class definition for RecordMagWebVariable_DataBlock. Looks like a container for housekeeping data. 
- WorldDataProcessor.java. class definition for WorldDataProcessor. WorldDataPacketReceived() calls parseRecord for the given record type as defined in buffer[5].
- WorldDataSerialReader.java  public class WorldDataSerialReader extends Thread implements SerialPortEventListener. Calls WorldDataPacketReceived().
