package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation;

import br.edu.ufcg.embedded.turmalina.domain.general.Module;
import br.edu.ufcg.embedded.turmalina.domain.general.Operation;
import br.edu.ufcg.embedded.turmalina.domain.general.project.Project;
import br.edu.ufcg.embedded.turmalina.domain.projectmanager.userstory.UserStory;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.extract.ExtractRecommendation;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.extract.ExtractRecommendationNFR;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.similarity.CalculateSimilarity;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.vector.BuildVector;
import br.edu.ufcg.embedded.turmalina.rest.util.recommendation.UtilRecommendation;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;


public class Recommendation {

    private BuildVector buildVector;
    private CalculateSimilarity calculateSimilarity;
    private ExtractRecommendation extractRecommendation;

    public Recommendation(BuildVector buildVector, CalculateSimilarity calculateSimilarity, ExtractRecommendation extractRecommendation){
        this.buildVector = buildVector;
        this.calculateSimilarity = calculateSimilarity;
        this.extractRecommendation = extractRecommendation;
    }

    public LinkedList<RecommendableItem> getRecommendation(UserStory userStory, List<UserStory> userStoryList, int K) {
        TreeMap<Double,List<UserStory>> similarityMap = new TreeMap<>(Collections.reverseOrder());

        List<String> targetProjectList = buildVector.generateTargetList(userStory);
        double[] targetVector = generateDoubleVectorWith1(targetProjectList.size());

        for (UserStory us: userStoryList) {
            double[] otherVector = this.buildVector.buildVector(userStory, us);

            double similarity = calculateSimilarity.getSimilarity(targetVector, otherVector);

            UtilRecommendation.insertIntoTreeMap(similarityMap, similarity, us);
        }

        LinkedList<RecommendableItem> recommendableItems = extractRecommendation.getRecommendation(similarityMap, K);

        return recommendableItems;
    }

    public LinkedList<RecommendableItem> getRecommendation(Project project, Module module, Operation operation, List<UserStory> userStoryList, int K) {
        TreeMap<Double,List<UserStory>> similarityMap = new TreeMap<>(Collections.reverseOrder());

        UserStory userStoryTemp = new UserStory();
        userStoryTemp.setModule(module);
        userStoryTemp.setOperation(operation);
        userStoryTemp.setProject(project);

        List<String> targetProjectList = buildVector.generateTargetList(userStoryTemp);
        double[] targetVector = generateDoubleVectorWith1(targetProjectList.size());

        for (UserStory us: userStoryList) {
            double[] otherVector = this.buildVector.buildVector(userStoryTemp, us);

            double similarity = calculateSimilarity.getSimilarity(targetVector, otherVector);

            UtilRecommendation.insertIntoTreeMap(similarityMap, similarity, us);
        }

        LinkedList<RecommendableItem> recommendableItems = extractRecommendation.getRecommendation(similarityMap, K);

        return recommendableItems;
    }

    private double[] generateDoubleVectorWith1(int size){
        double[] vector = new double[size];
        for (int i = 0; i < size; i++ ){
            vector[i] = 1;
        }
        return vector;
    }
}
