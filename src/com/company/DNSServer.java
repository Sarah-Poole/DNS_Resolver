package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

/*
Open a UDP Socket (DatagramSocket class)
Listen for requests
Look at all questions in a request
 - if valid answer in cache, add to response
 - else create another UDP socket to forward request to Google (8.8.8.8) and await response
Send the dig additional record (type 41) back in the addition record part of response
 */
public class DNSServer {
    private DNSCache cache = new DNSCache();

    public void runServer() throws IOException {
        //Socket object for carrying the data
        DatagramSocket ds = new DatagramSocket(8053);
        //Byte[] for packet data
        byte[] data = new byte[512];
        //Packet to receive request data
        DatagramPacket dp = null;

        while(true) {
            //Create packet that will fill-in with request data
            dp = new DatagramPacket(data, data.length);

            //Receive the request and fill-in data
            ds.receive(dp);
            System.out.println("Received something from client...");

            //Read-in request message and handle
            DNSMessage requestMessage = DNSMessage.decodeMessage(data);

            //Does my cache already contain the answer?
            if (cache.queryCache(requestMessage.getQuestions().get(0))) {
                //Create an array with the correct answer
                ArrayList<DNSRecord> answers = new ArrayList<>();
                answers.add(cache.getRecord(requestMessage.getQuestions().get(0)));

                //Construct response with original request + cached answers
                DNSMessage cacheResponseAnswer = DNSMessage.buildResponse(requestMessage, answers);
                byte[] finalResponse = cacheResponseAnswer.toBytes();

                //Send the response back to client
                InetAddress clientIP = dp.getAddress();
                int clientPort = dp.getPort();
                DatagramPacket finalData = new DatagramPacket(finalResponse, finalResponse.length, clientIP, clientPort);
                ds.send(finalData);

            } else {
                //Open new socket, send request onto google
                DatagramSocket googleDS = new DatagramSocket();
                byte[] googleData = new byte[512];
                DatagramPacket googleDP = null;

                //Send to Google
                InetAddress googleIP = InetAddress.getByName("8.8.8.8");
                DatagramPacket dp2 = new DatagramPacket(data, data.length, googleIP, 53);
                System.out.println("Data being forwarded on to Google!");
                googleDS.send(dp2);

                //Wait for response from Google
                googleDP = new DatagramPacket(googleData, googleData.length);
                googleDS.receive(googleDP);

                //Read-in Google response message and handle
                DNSMessage googleResponseMessage = DNSMessage.decodeMessage(googleData);

                //What if the URL in question doesn't exist?
                if (googleResponseMessage.getHeader().getRcode() == 3) {
                    byte[] finalResponse = googleResponseMessage.toBytes();
                    //Send Google's response directly to client
                    InetAddress clientIP = dp.getAddress();
                    int clientPort = dp.getPort();
                    DatagramPacket finalData = new DatagramPacket(finalResponse, finalResponse.length, clientIP, clientPort);
                    ds.send(finalData);

                    System.out.println("Bad URL; Forwarding google's response!");

                } else {
                    //Add Google answer to cache
                    cache.addToCache(googleResponseMessage.getQuestions().get(0),googleResponseMessage.getAnswers().get(0));

                    //Construct response with original request + Google answer
                    DNSMessage googleResponseAnswer = DNSMessage.buildResponse(requestMessage, googleResponseMessage.getAnswers());
                    byte[] finalResponse = googleResponseAnswer.toBytes();

                    //Send the response back to client
                    InetAddress clientIP = dp.getAddress();
                    int clientPort = dp.getPort();
                    DatagramPacket finalData = new DatagramPacket(finalResponse, finalResponse.length, clientIP, clientPort);
                    ds.send(finalData);

                    //Clear data after each request?
                    googleData = new byte[512];
                }
            }

            //Clear data after each request
            data = new byte[512];
        }
    }


}
