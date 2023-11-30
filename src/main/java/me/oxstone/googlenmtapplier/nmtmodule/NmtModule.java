package me.oxstone.googlenmtapplier.nmtmodule;

import com.google.protobuf.InvalidProtocolBufferException;

import javax.management.RuntimeErrorException;
import java.io.IOException;
import java.util.Map;

public interface NmtModule {
    Map<String, String> batchTranslateText(Map<String, String> segmentMap) throws IOException, RuntimeErrorException;
    String translateText(String text) throws InvalidProtocolBufferException;
}
