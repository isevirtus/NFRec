package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.similarity;

import org.apache.commons.math3.ml.distance.ManhattanDistance;

public class CalculateSimilarityManhattan implements CalculateSimilarity{
    @Override
    public double getSimilarity(double[] target, double[] other) {
        ManhattanDistance manhattanDistance = new ManhattanDistance();
        Double distance = manhattanDistance.compute(target,other);
        if(target.length == 0 ) {
            return 1.0;
        }else {
            return 1.0 - (distance / target.length);
        }
    }
}
