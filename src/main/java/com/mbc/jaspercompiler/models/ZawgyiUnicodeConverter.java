package com.mbc.jaspercompiler.models;

import com.google.myanmartools.TransliterateU2Z;
import com.google.myanmartools.TransliterateZ2U;
import com.google.myanmartools.ZawgyiDetector;

public class ZawgyiUnicodeConverter {
    public static final ZawgyiDetector detector = new ZawgyiDetector();
    public static final TransliterateZ2U unicodeConverter = new TransliterateZ2U("Zawgyi to Unicode");
    public static final TransliterateU2Z zawgyiConverter = new TransliterateU2Z("Unicode to Zawgyi");
    public static String convertToZawgyi(String text) {
        return zawgyiConverter.convert(text);
    }
    public static String convertToUnicode(String text) {
        return unicodeConverter.convert(text);
    }
    public static boolean isUnicode(String text) {
        return detector.getZawgyiProbability(text) > 0.001;
    }
}
