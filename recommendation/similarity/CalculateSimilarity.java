package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.similarity;

public interface CalculateSimilarity {
    double getSimilarity(double[] target, double[] other);
}
