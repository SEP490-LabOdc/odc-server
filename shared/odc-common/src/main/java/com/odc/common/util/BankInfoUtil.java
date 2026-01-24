package com.odc.common.util;

import java.util.HashMap;
import java.util.Map;

public class BankInfoUtil {
    private static final Map<String, String> BANK_BIN_MAP = new HashMap<>();

    static {
        BANK_BIN_MAP.put("VCB", "970436");
        BANK_BIN_MAP.put("VIETCOMBANK", "970436");

        BANK_BIN_MAP.put("VIETINBANK", "970415");
        BANK_BIN_MAP.put("CTG", "970415");

        BANK_BIN_MAP.put("BIDV", "970418");

        BANK_BIN_MAP.put("TECHCOMBANK", "970407");
        BANK_BIN_MAP.put("TCB", "970407");

        BANK_BIN_MAP.put("MB", "970422");
        BANK_BIN_MAP.put("MB BANK", "970422");
        BANK_BIN_MAP.put("MBBANK", "970422");

        BANK_BIN_MAP.put("ACB", "970416");

        BANK_BIN_MAP.put("VPBANK", "970432");

        BANK_BIN_MAP.put("SACOMBANK", "970403");
        BANK_BIN_MAP.put("STB", "970403");

        BANK_BIN_MAP.put("SHB", "970443");

        BANK_BIN_MAP.put("TPBANK", "970423");

        BANK_BIN_MAP.put("VIB", "970441");

        BANK_BIN_MAP.put("OCB", "970448");

        BANK_BIN_MAP.put("HDBANK", "970437");

        BANK_BIN_MAP.put("SEABANK", "970440");

        BANK_BIN_MAP.put("EXIMBANK", "970431");
    }

    public static String getBin(String bankName) {
        if (bankName == null) {
            return null;
        }
        return BANK_BIN_MAP.get(bankName.trim().toUpperCase());
    }
}
