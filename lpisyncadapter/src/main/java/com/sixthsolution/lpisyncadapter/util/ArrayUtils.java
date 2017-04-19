package com.sixthsolution.lpisyncadapter.util;

import java.lang.reflect.Array;

/**
 * @author mehdok (mehdok@gmail.com) on 4/8/2017.
 */

public class ArrayUtils {
    public static <T> T[][] partition(T[] bigArray, int max) {
        int nItems = bigArray.length;
        int nPartArrays = (nItems + max - 1) / max;

        T[][] partArrays = (T[][]) Array.newInstance(bigArray.getClass().getComponentType(), nPartArrays, 0);

        // nItems is now the number of remaining items
        for (int i = 0; nItems > 0; i++) {
            int n = (nItems < max) ? nItems : max;
            partArrays[i] = (T[]) Array.newInstance(bigArray.getClass().getComponentType(), n);
            System.arraycopy(bigArray, i * max, partArrays[i], 0, n);

            nItems -= n;
        }

        return partArrays;
    }
}
