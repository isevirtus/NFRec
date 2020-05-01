package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.similarity;

import org.apache.commons.math3.ml.distance.CanberraDistance;

public class CalculateSimilarityCanberra implements CalculateSimilarity {
    @Override
    public double getSimilarity(double[] target, double[] other) {
        CanberraDistance canberraDistance = new CanberraDistance();
        Double distance = canberraDistance.compute(target,other);
        return 1.0/(1.0 + distance);
    }
}
