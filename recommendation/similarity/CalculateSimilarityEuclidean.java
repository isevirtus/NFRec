package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.similarity;

import org.apache.commons.math3.ml.distance.EuclideanDistance;

public class CalculateSimilarityEuclidean implements CalculateSimilarity {
    @Override
    public double getSimilarity(double[] target, double[] other) {
        EuclideanDistance euclideanDistance = new EuclideanDistance();
        Double distance = euclideanDistance.compute(target,other);
        return 1.0/(1.0 + distance);
    }
}
