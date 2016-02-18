package protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import client.*;

public class SmartDataTransferProtocol extends IRDTProtocol {

    // change the following as you wish:
    static final int HEADERSIZE=1;   // number of header bytes in each packet
    static final int DATASIZE=128;   // max. number of user data bytes in each packet
    Map<Integer, Integer[]> dataReceived = new HashMap<Integer, Integer[]>();

    @Override
    public void sender() {
        System.out.println("Sending...");

        // read from the input file
        Integer[] fileContents = Utils.getFileContents(getFileID());

        // keep track of where we are in the data
        int filePointer = 0;

        // create a new packet of appropriate size
        int datalen = Math.min(DATASIZE, fileContents.length - filePointer);
        Integer[] pkt = new Integer[HEADERSIZE + datalen];
        // write something random into the header byte
        pkt[0] = 123;    
        // copy databytes from the input file into data part of the packet, i.e., after the header
        System.arraycopy(fileContents, filePointer, pkt, HEADERSIZE, datalen);

        // send the packet to the network layer
        getNetworkLayer().sendPacket(pkt);
        System.out.println("Sent one packet with header = "+pkt[0]);

        // schedule a timer for 1000 ms into the future, just to show how that works:
        client.Utils.Timeout.SetTimeout(1000, this, 28);

        // and loop and sleep; you may use this loop to check for incoming acks...
        boolean stop = false;
        while (!stop) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                stop = true;
            }
        }

    }

    @Override
    public void TimeoutElapsed(Object tag) {
        int z=(Integer)tag;
        // handle expiration of the timeout:
        System.out.println("Timer expired with tag = "+z);
    }

    @Override
    public void receiver() {
        System.out.println("Receiving...");

        // create the array that will contain the file contents
        // note: we don't know yet how large the file will be, so the easiest (but not most efficient)
        //   is to reallocate the array every time we find out there's more data
        int amountPackets = 5;
        ArrayList<String> list = new ArrayList<String>();
        
        // loop until we are done receiving the file
        boolean stop = false;
        while (!stop) {

            // try to receive a packet from the network layer
            Integer[] packet = getNetworkLayer().receivePacket();
            
            // if we indeed received a packet
            if (packet != null && !list.contains(packet[0])) {
                if(!list.contains(packet[0])) {
                  list.add(packet[0] + "");
                }
                // tell the user
                System.out.println("Received packet, length="+packet.length+"  first byte="+packet[0] );

                // append the packet's data part (excluding the header) to the fileContents array, first making it larger
                dataReceived.put(packet[0], packet);
                Integer[] ack = new Integer[1];
                ack[0] = packet[0];
                getNetworkLayer().sendPacket(ack);

                // and let's just hope the file is now complete
                
                
                if (list.size() >= amountPackets){
                  stop=true; 
                }


            }else{
                // wait ~10ms (or however long the OS makes us wait) before trying again
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    stop = true;
                }
            }
        }
        
        // write to the output file
        Integer[] fileWrite = new Integer[2085];
        int inPosition = 0;
        for(int i: dataReceived.keySet()){
          if(dataReceived.containsKey(i)){
            System.arraycopy(dataReceived.get(i), HEADERSIZE, fileWrite, inPosition, dataReceived.get(i).length - HEADERSIZE);
            inPosition = inPosition + dataReceived.get(i).length - HEADERSIZE;
          }
        }
        Utils.setFileContents(fileWrite, getFileID());
    }
    
}
