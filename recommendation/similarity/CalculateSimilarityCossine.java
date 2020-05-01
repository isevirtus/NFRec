package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.similarity;

public class CalculateSimilarityCossine implements CalculateSimilarity {
    @Override
    public double getSimilarity(double[] target, double[] other) {
        Double numerator = 0.0;
        for(int i =0; i < target.length; i++) {
            numerator += target[i] * other[i];
        }
        Double denominator = getSquareRooted(target) * getSquareRooted(other);
        if(denominator == 0.0) {
            return 0.0;
        }else {
            return numerator/denominator;
        }
    }

    private static Double getSquareRooted(double[] vector) {
        Double value = 0.0;
        for(int i =0; i < vector.length; i++) {
            value += Math.pow(vector[i], 2);
        }
        return Math.sqrt(value);

    }
}
