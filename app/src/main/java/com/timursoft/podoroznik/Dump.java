package com.timursoft.podoroznik;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Dump {

    public static final int BLOCK_COUNT = 4;
    public static final int BLOCK_SIZE = MifareClassic.BLOCK_SIZE;
    public static final int SECTOR_INDEX = 4;

    public static final byte[] KEY_A =
            {(byte) -27, (byte) 106, (byte) -63, (byte) 39, (byte) -35, (byte) 69};

    // raw
    protected byte[] uid;
    protected byte[][] data;

    // parsed
    protected int cardNumber;
    protected float balance;
    protected Date lastUsageDate;
    protected int lastValidatorId;

    public Dump(byte[] uid, byte[][] sector8) {
        this.uid = uid;
        this.data = sector8;
        parse();
    }

    public static Dump fromTag(Tag tag) throws IOException {
        MifareClassic mfc = getMifareClassic(tag);

        int blockCount = mfc.getBlockCountInSector(SECTOR_INDEX);
        if (blockCount < BLOCK_COUNT) {
            throw new IOException("Wtf? Not enough blocks on this card");
        }

        byte[][] data = new byte[BLOCK_COUNT][BLOCK_SIZE];

        for (int i = 0; i < BLOCK_COUNT; i++) {
            data[i] = mfc.readBlock(mfc.sectorToBlock(SECTOR_INDEX) + i);
        }

        return new Dump(tag.getId(), data);
    }

    protected static MifareClassic getMifareClassic(Tag tag) throws IOException {
        MifareClassic mfc = MifareClassic.get(tag);
        mfc.connect();

        // good card
        if (mfc.authenticateSectorWithKeyA(SECTOR_INDEX, KEY_A)) {
            return mfc;
        }

        throw new IOException("No permissions");
    }

    protected void parse() {
        // block#0 bytes#3-6
        cardNumber = intval(data[0][3], data[0][4], data[0][5], data[0][6]) >> 4;

        // block#1 bytes#0-1
        lastValidatorId = intval(data[1][0], data[1][1]);

        // block#1 bytes#2-4½
        int lastUsageDay = intval(data[1][2], data[1][3]);
        if (lastUsageDay > 0) {
            double lastUsageTime = (double) intval(
                    (byte) (data[1][4] >> 4 & 0x0F),
                    (byte) (data[1][5] >> 4 & 0x0F | data[1][4] << 4 & 0xF0)
            );
            lastUsageTime = lastUsageTime / 120.0;
            int lastUsageHour = (int) Math.floor(lastUsageTime);
            int lastUsageMinute = (int) Math.round((lastUsageTime % 1) * 60);

            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"));
            c.set(1992, 0, 1, lastUsageHour, lastUsageMinute);
            c.add(Calendar.DATE, lastUsageDay - 1);
            lastUsageDate = c.getTime();
        } else {
            lastUsageDate = null;
        }

        balance = (float) (((data[0][1] & 255) << 8) + (data[0][0] & 255)) / 100;
    }

    public byte[] getUid() {
        return uid;
    }

    public String getUidAsString() {
        return HexUtils.toString(getUid());
    }

    public byte[][] getData() {
        return data;
    }

    public String[] getDataAsStrings() {
        String blocks[] = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            blocks[i] = HexUtils.toString(data[i]);
        }
        return blocks;
    }

    public Date getLastUsageDate() {
        return lastUsageDate;
    }

    public String getLastUsageDateAsString() {
        if (lastUsageDate == null) {
            return "<NEVER USED>";
        }
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(lastUsageDate);
    }

    public int getLastValidatorId() {
        return lastValidatorId;
    }

    public String getLastValidatorIdAsString() {
        return "ID# " + getLastValidatorId();
    }

    public float getBalance() {
        return balance;
    }

    public String getBalanceAsString() {
        return "" + getBalance() + " RUB";
    }

    public int getCardNumber() {
        return cardNumber;
    }

    public String getCardNumberAsString() {
        return formatCardNumber(cardNumber);
    }

    public static String formatCardNumber(int cardNumber) {
        int cardNum3 = cardNumber % 1000;
        int cardNum2 = (int) Math.floor(cardNumber / 1000) % 1000;
        int cardNum1 = (int) Math.floor(cardNumber / 1000000) % 1000;
        return String.format("%04d %03d %03d", cardNum1, cardNum2, cardNum3);
    }

    public String toString() {
        return "[Card UID=" + getUidAsString() + " " + getBalanceAsString() + "RUR]";
    }

    protected static int intval(byte... bytes) {
        int value = 0;
        for (int i = 0; i < bytes.length; i++) {
            int x = (int) bytes[bytes.length - i - 1];
            while (x < 0) x = 256 + x;
            value += x * Math.pow(0x100, i);
        }
        return value;
    }

}
