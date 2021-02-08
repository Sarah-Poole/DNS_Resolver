package com.company;

import java.util.HashMap;

/*
A Local Cache
Store the first answer for any question in the cache
Methods for querying and inserting records into the cache
If an entry is too old (TTL has expired), remove it and return "not found"
 */
public class DNSCache {
    private HashMap<DNSQuestion, DNSRecord> cache = new HashMap<>();

    public boolean queryCache(DNSQuestion question){
        if (cache.containsKey(question)) {
            if (cache.get(question).timestampValid()) {
                return true;
            } else {
                cache.remove(question);
                return false;
            }
        } else {
            return false;
        }
    }

    public void addToCache(DNSQuestion question, DNSRecord answerRecord) {
        cache.put(question, answerRecord);
    }

    public DNSRecord getRecord(DNSQuestion question) {
        return cache.get(question);
    }

}
