package com.timursoft.podoroznik;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends Activity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.text);

        if (getIntent() != null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            try {
                Dump dump = Dump.fromTag(tag);
                textView.setText(String.format(getString(R.string.balance), dump.getBalanceAsString()));
            } catch (IOException e) {
                e.printStackTrace();
            }

//            info.append("\nCard UID: " + dump.uidAsString)
//            info.append("\n\n  --- Sector #4: ---\n")
//            val blocks = dump.dataAsStrings
//            for (i in blocks.indices) {
//                info.append("\n" + i + "] " + blocks[i])
//            }
//            info.append("\n\n  --- Extracted data: ---\n")
//            info.append("\nCard number:      " + dump.cardNumberAsString)
//            info.append("\nCurrent balance:  " + dump.balanceAsString)
//            info.append("\nLast usage date:  " + dump.lastUsageDateAsString)
//            info.append("\nLast validator:   " + dump.lastValidatorIdAsString)
        }
    }

}
