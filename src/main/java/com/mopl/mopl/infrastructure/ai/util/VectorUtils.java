package com.mopl.mopl.infrastructure.ai.util;

public class VectorUtils
{
    private VectorUtils() {}

    public static String serialize(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
