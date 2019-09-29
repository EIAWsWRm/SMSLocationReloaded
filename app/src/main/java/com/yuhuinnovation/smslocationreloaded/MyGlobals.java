package com.yuhuinnovation.smslocationreloaded;

public class MyGlobals {
    //function to convert an input stream into a string
    public static String streamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}