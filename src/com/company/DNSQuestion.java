package com.company;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

//REPRESENTS CLIENT REQUEST
public class DNSQuestion {
    private byte[] qtype;
    private byte[] qclass;
    private String[] domainName;

    //Read question from input stream
    static DNSQuestion decodeQuestion(InputStream input, DNSMessage request) throws IOException {
        DNSQuestion question = new DNSQuestion();

        question.domainName = request.readDomainName(input);
        question.qtype = input.readNBytes(2);
        question.qclass = input.readNBytes(2);

        return question;
    }

    //Write the question in bytes to send to client
    //Hashmap used to compress the message (created in Message class)
    void writeBytes(ByteArrayOutputStream output, HashMap<String, Integer> domainNameLocations) {
        DNSMessage.writeDomainName(output,domainNameLocations,domainName);
        output.writeBytes(qtype);
        output.writeBytes(qclass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSQuestion that = (DNSQuestion) o;
        return Arrays.equals(qtype, that.qtype) &&
                Arrays.equals(qclass, that.qclass) &&
                Arrays.equals(domainName, that.domainName);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(qtype);
        result = 31 * result + Arrays.hashCode(qclass);
        result = 31 * result + Arrays.hashCode(domainName);
        return result;
    }
}
