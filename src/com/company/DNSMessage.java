package com.company;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/*
ENTIRE DNS MESSAGE:
DNS Header
Array of questions
Array of answers
Array of authority records
Array of additional records
Byte Array of complete message (to handle compression techniques)
 */
public class DNSMessage {
    private DNSHeader header;
    private ArrayList<DNSQuestion> questions;
    private ArrayList<DNSRecord> answers, authorityRecords, additionalRecords;
    private byte[] completeMessage;

    static DNSMessage decodeMessage(byte[] bytes) throws IOException {
        DNSMessage message = new DNSMessage();
        //Store the complete message
        message.completeMessage = bytes;

        //Create input stream to read each part of bytes
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);

        //Create header
        message.header = DNSHeader.decodeHeader(input);

        //Loop through each question and run it through decodeQuestion
        message.questions = new ArrayList<>();
        DNSQuestion question;
        for (int i=0; i< message.header.getNumQuestions(); i++) {
            question = DNSQuestion.decodeQuestion(input, message);
            message.questions.add(question);
        }

        //Loop through each answer and run it through decodeRecord
        message.answers = new ArrayList<>();
        DNSRecord answer;
        for (int i=0; i<message.header.getNumAnswers(); i++) {
            answer = DNSRecord.decodeRecord(input, message);
            message.answers.add(answer);
        }

        //Loop through each authority record and run it through decodeRecord
        message.authorityRecords = new ArrayList<>();
        DNSRecord authorityRecord;
        for (int i=0; i<message.header.getNumAuthority(); i++) {
            authorityRecord = DNSRecord.decodeRecord(input, message);
            message.authorityRecords.add(authorityRecord);
        }

        //Loop through each additional record and run it through decodeRecord
        message.additionalRecords = new ArrayList<>();
        DNSRecord additionalRecord;
        for (int i=0; i<message.header.getNumAdditional(); i++) {
            additionalRecord = DNSRecord.decodeRecord(input, message);
            message.additionalRecords.add(additionalRecord);
        }

        return message;
    }

    //Read the pieces of a domain name, starting from current position of input stream
    String[] readDomainName(InputStream input) throws IOException {
        //Check for compression
        byte compressionPointer = input.readNBytes(1)[0];
        int compressionCheck = ((int) compressionPointer & 0xff) >> 6;

        if (compressionCheck == 3) {
            byte compressionOffset = input.readNBytes(1)[0];
            int entireCompression = (compressionPointer << 8) | compressionOffset;
            int offset = entireCompression & 0x3fff;
            return readDomainName(offset);
        }

        //Store bytes from input stream
        ArrayList<Byte> domainNameList = new ArrayList<>();
        domainNameList.add(compressionPointer);

        //domainName = Read-in bytes until 0
        while (true) {
            int currentByte = input.read();
            if (currentByte == 0) {
                break;
            }
            domainNameList.add((byte)currentByte);
        }

        //turn qname back into byte[]
        byte[] domainNameArray = new byte[domainNameList.size()];
        for(int i = 0; i<domainNameList.size(); i++) {
            domainNameArray[i] = domainNameList.get(i);
        }

        //Assign domainName = qname as a split string
        String qnameString = new String(domainNameArray);
        String[] domainName = qnameString.split(" ");

        return domainName;
    }

    //Use this when there's compression, but need to find domain from earlier message
    //Make a ByteArrayInputStream that starts at int firstByte
    //Call other version of readDomainName
    String[] readDomainName(int firstByte) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(completeMessage);
        input.readNBytes(firstByte);
        String[] domainName = readDomainName(input);

        return domainName;
    }

    //Build a response based off request and answers we'll send back
    //answerArray is built in DNSServer
    // - either answers are from cache or from Google
    static DNSMessage buildResponse(DNSMessage request, ArrayList<DNSRecord> answerArray) {
        DNSMessage response = new DNSMessage();

        response.questions = request.questions;
        response.answers = answerArray;
        response.authorityRecords = request.authorityRecords;
        response.additionalRecords = request.additionalRecords;
        response.header = DNSHeader.buildResponseHeader(request, response);

        return response;
    }

    //Get bytes to put in a packet to send
    //Call each class's writeBytes()
    byte[] toBytes() {
        //Create hashmap that holds name and location of un-compressed domain names
        HashMap<String, Integer> domainNameLocation = new HashMap<>();

        //Create an output stream
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        //Get bytes from each portion of the message
        header.writeBytes(output);

        for (DNSQuestion question: this.questions) {
            question.writeBytes(output, domainNameLocation);
        }
        for (DNSRecord answer: this.answers) {
            answer.writeBytes(output, domainNameLocation);
        }
        for (DNSRecord authorityRecord: this.authorityRecords) {
            authorityRecord.writeBytes(output, domainNameLocation);
        }
        for (DNSRecord additionalRecord: this.additionalRecords) {
            additionalRecord.writeBytes(output, domainNameLocation);
        }

        //Create and return a byte[] of the whole message
        return output.toByteArray();
    }

    /*
    If first time seeing domain name:
        - write using DNS encoding - each segment of domain prefixed with length, and a 0 at the end
        - add to hashmap
    Else:
        - write a back pointer to where domain has been previously
     */
    static void writeDomainName(ByteArrayOutputStream output, HashMap<String, Integer> domainNameLocations, String[] domainPieces) {
        String domainName = DNSMessage.octetsToString(domainPieces);

        //First time seeing domain name:
        if (!domainNameLocations.containsKey(domainName)) {
            //Write using DNS encoding - each segment of domain prefixed with length
            for (String s: domainPieces) {
                char[] chars = s.toCharArray();
                for (char c: chars) {
                    output.write(c);
                }
            }
            //Add ending 0
            output.write(0x00);

            //Add to hashmap
            domainNameLocations.put(domainName, output.size());

        //Write a back pointer to where domain was seen previously
        //Construct the 2-byte offset (indicating compression)
        } else {
            int offsetInt = domainNameLocations.get(domainName);
            byte[] offsetBytes = DNSMessage.getBytes(offsetInt);
            offsetBytes[0] = (byte) (offsetBytes[0] | 0xC0);

            output.writeBytes(offsetBytes);
        }
    }

    //Join pieces of a domain name with dots
    // ex. ["utah", "edu"] -> "utah.edu"
    public static String octetsToString(String[] octets) {
        String domainName = "";

        if (octets.length > 0) {
            StringBuilder sb = new StringBuilder();

            for (String s: octets) {
                sb.append(s).append(".");
            }

            domainName = sb.deleteCharAt(sb.length() - 1).toString();
        }
        return domainName;
    }

    public static byte[] getBytes(int val) {
        byte[] data = new byte[2];
        data[1] = (byte) (val & 0xff);
        data[0] = (byte) ((val >>> 8) & 0xff);

        return data;
    }

    public DNSHeader getHeader() { return header; }

    public ArrayList<DNSQuestion> getQuestions() { return questions; }

    public ArrayList<DNSRecord> getAnswers() { return answers; }

    public ArrayList<DNSRecord> getAuthorityRecords() { return authorityRecords; }

    public ArrayList<DNSRecord> getAdditionalRecords() { return additionalRecords; }

    public byte[] getCompleteMessage() { return completeMessage; }
}
