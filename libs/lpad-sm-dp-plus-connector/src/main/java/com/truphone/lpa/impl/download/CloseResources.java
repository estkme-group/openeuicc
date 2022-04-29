package com.truphone.lpa.impl.download;

import java.io.InputStream;

public class CloseResources {

    static void closeResources(InputStream is) {

        if (is != null) {
            try {
                is.close();
            } catch (Exception ignored) {
            }
        }
    }
}
