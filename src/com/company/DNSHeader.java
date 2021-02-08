package com.company;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;;
import java.util.Arrays;


/* STORE ALL DATA PROVIDED BY 12 BYTE DNS HEADER
ID: 16-bit identifier of a query
QR: 1-bit query(0) or response(1)
OPCODE: 4-bit (always 0)
AA: 1-bit Authoritative Answer - is responding server authority on question? (0)
TC: 1-bit Truncation (always 0?)
RD: 1-bit indicates query should be pursued recursively (always 1)
RA: 1-bit in response - whether recursive query is supported in server or not (1 for response)
Z: 1-bit reserved for future use (always 0)
AD: 1-bit "Authentic Data" (0)
CD: 1-bit "Checking Disabled" (0)
RCODE: 4-bit Response code: (a value between 0-5)

(Req) (Resp)
QDCOUNT: 16-bit number of entries in question section (1) (1)
ANCOUNT: 16-bit number of resource records in answer section (0) (1)
NSCOUNT: 16-bit authority records section (0) (0) ~IGNORE
ARCOUNT: 16-bit resource records/ additional records section (0) (0) ~IGNORE
*/

public class DNSHeader {
    private byte[] id;
    private byte[] flags;
    private byte qr = 0;
    private byte opcode = 0;
    private byte aa = 0;
    private byte tc = 0;
    private byte rd = 0;
    private byte ra = 0;
    private byte z = 0;
    private byte ad = 0;
    private byte cd = 0;
    private byte rcode = 0;
    private byte[] qdcount;
    private int numQuestions;
    private byte[] ancount;
    private int numAnswers;
    private byte[] nscount;
    private int numAuthority;
    private byte[] arcount;
    private int numAdditional;

    // Read-in header
    static DNSHeader decodeHeader(ByteArrayInputStream input) throws IOException {
        DNSHeader header = new DNSHeader();
        header.id = input.readNBytes(2);

        header.flags = input.readNBytes(2); //Contains: qr, opcode, aa, tc, rd, ra, z, ad, cd & rcode
        header.qr = (byte) (header.flags[0] & 0b10000000);
        header.opcode = (byte) (header.flags[0] & 0b01111000);
        header.aa = (byte) (header.flags[0] & 0b00000100);
        header.tc = (byte) (header.flags[0] & 0b00000010);
        header.rd = (byte) (header.flags[0] & 0b00000001);
        header.ra = (byte) (header.flags[1] & 0b10000000);
        header.z =  (byte) (header.flags[1] & 0b01000000);
        header.ad = (byte) (header.flags[1] & 0b00100000);
        header.cd = (byte) (header.flags[1] & 0b00010000);
        header.rcode = (byte) (header.flags[1] & 0b00001111);

        header.qdcount = input.readNBytes(2);
        header.numQuestions = header.getIntCount(header.qdcount);
        header.ancount = input.readNBytes(2);
        header.numAnswers = header.getIntCount(header.ancount);
        header.nscount = input.readNBytes(2);
        header.numAuthority = header.getIntCount(header.nscount);
        header.arcount = input.readNBytes(2);
        header.numAdditional = header.getIntCount(header.arcount);

        return header;
    }

    //Copy fields from request to create header for response
    static DNSHeader buildResponseHeader(DNSMessage request, DNSMessage response) {
        DNSHeader header = new DNSHeader();

        header.id = request.getHeader().getId();
        header.flags = request.getHeader().getFlags();
        header.flags[0] = (byte) (header.flags[0] | 0b10000000);
        header.flags[1] = (byte) (header.flags[1] | 0b10000000);

        header.qdcount = header.getByteCount(request.getQuestions().size());
        header.ancount = header.getByteCount(response.getAnswers().size());
        header.nscount = header.getByteCount(request.getAuthorityRecords().size());
        header.arcount = header.getByteCount(request.getAdditionalRecords().size());

        return header;
    }

    //Encode header to bytes to be sent back to client
    void writeBytes(ByteArrayOutputStream output) {
        output.writeBytes(id);
        output.writeBytes(flags);
        output.writeBytes(qdcount);
        output.writeBytes(ancount);
        output.writeBytes(nscount);
        output.writeBytes(arcount);
    }

    int getIntCount(byte[] array) {
        return array[1] & 0xFF | (array[0]) << 8;
    }

    public byte[] getByteCount(int val) {
        byte[] data = new byte[2];
        data[1] = (byte) (val & 0xff);
        data[0] = (byte) ((val >> 8) & 0xff);

        return data;
    }

    @Override
    public String toString() {
        return "DNSHeader{" +
                "id=" + Arrays.toString(id) +
                ", qr=" + qr +
                ", opcode=" + opcode +
                ", aa=" + aa +
                ", tc=" + tc +
                ", rd=" + rd +
                ", ra=" + ra +
                ", z=" + z +
                ", ad=" + ad +
                ", cd=" + cd +
                ", rcode=" + rcode +
                ", qdcount=" + Arrays.toString(qdcount) +
                ", ancount=" + Arrays.toString(ancount) +
                ", nscount=" + Arrays.toString(nscount) +
                ", arcount=" + Arrays.toString(arcount) +
                '}';
    }

    public byte[] getId() {
        return id;
    }

    public byte[] getFlags() { return flags; }

    public byte getOpcode() { return opcode; }

    public byte getAa() { return aa; }

    public byte getTc() { return tc; }

    public byte getRd() { return rd; }

    public byte getZ() { return z; }

    public byte getAd() { return ad; }

    public byte getCd() { return cd; }

    public byte getRcode() { return rcode; }

    public int getNumQuestions() {
        return numQuestions;
    }

    public int getNumAnswers() {
        return numAnswers;
    }

    public int getNumAuthority() {
        return numAuthority;
    }

    public int getNumAdditional() {
        return numAdditional;
    }
}
