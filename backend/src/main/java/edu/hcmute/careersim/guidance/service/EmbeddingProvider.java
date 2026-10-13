package edu.hcmute.careersim.guidance.service;

public interface EmbeddingProvider {
    boolean available();

    String profile();

    double[] embed(String text, boolean query);
}
