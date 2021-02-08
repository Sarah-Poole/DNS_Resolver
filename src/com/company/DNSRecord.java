package com.company;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

//EVERYTHING AFTER THE HEADER AND QUESTION PARTS OF DNS MESSAGE
public class DNSRecord {
    String[] name;
    byte[] type;
    byte[] rclass;
    byte[] ttl;
    byte[] rdlength;
    byte[] rdata;
    Calendar recordCreated;

    //All the fields listed in the spec
    //Date object - stores when record was created by program
    static DNSRecord decodeRecord(InputStream input, DNSMessage request) throws IOException {
        DNSRecord record = new DNSRecord();

        record.name = request.readDomainName(input);
        record.type = input.readNBytes(2);
        record.rclass = input.readNBytes(2);
        record.ttl = input.readNBytes(4);
        record.rdlength = input.readNBytes(2);

        //Read # bytes = rdlength #
        record.rdata = input.readNBytes(record.getIntCount(record.rdlength));

        //Current date and time
        record.recordCreated = Calendar.getInstance();

        return record;
    }

    //Write record in bytes
    void writeBytes(ByteArrayOutputStream output, HashMap<String, Integer> domainNameLocations) {
        DNSMessage.writeDomainName(output,domainNameLocations,name);
        output.writeBytes(type);
        output.writeBytes(rclass);
        output.writeBytes(ttl);
        output.writeBytes(rdlength);
        output.writeBytes(rdata);
    }

    //Return whether the creation date + time to live is after current time
    boolean timestampValid() {
        //The current time in milliseconds
        Calendar date = Calendar.getInstance();
        //Convert ttl (4 bytes) to an int
        int ttlSeconds = ByteBuffer.wrap(this.ttl).getInt();
        return ((this.recordCreated.getTimeInMillis() + (ttlSeconds * 1000)) > date.getTimeInMillis());
    }

    public int getIntCount(byte[] array) {
        return array[1] & 0xFF | (array[0]) << 8;
    }

    @Override
    public String toString() {
        return "DNSRecord{" +
                "name=" + Arrays.toString(name) +
                ", type=" + Arrays.toString(type) +
                ", rclass=" + Arrays.toString(rclass) +
                ", ttl=" + Arrays.toString(ttl) +
                ", rdlength=" + Arrays.toString(rdlength) +
                ", rdata=" + Arrays.toString(rdata) +
                ", recordCreated=" + recordCreated +
                '}';
    }

}
