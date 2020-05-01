package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.extract;

import br.edu.ufcg.embedded.turmalina.domain.projectmanager.userstory.UserStory;
import br.edu.ufcg.embedded.turmalina.domain.testmanager.specification.TestSpecification;
import br.edu.ufcg.embedded.turmalina.generic.EventException;
import br.edu.ufcg.embedded.turmalina.generic.event.ListResponseEvent;
import br.edu.ufcg.embedded.turmalina.generic.event.ViewResponseEvent;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.RecommendableItem;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.RecommendableItemType;
import br.edu.ufcg.embedded.turmalina.rest.util.recommendation.UtilRecommendation;
import br.edu.ufcg.embedded.turmalina.services.project.testmanager.TestSpecificationService;
import org.aspectj.weaver.ast.Test;

import java.util.*;

public class ExtractRecommendationTestCases implements ExtractRecommendation {

    TestSpecificationService testSpecificationService;

    public ExtractRecommendationTestCases(TestSpecificationService testSpecificationService){
        this.testSpecificationService = testSpecificationService;
    }

    @Override
    public LinkedList<RecommendableItem> getRecommendation(TreeMap<Double, List<UserStory>> userStoriesSimilarities, int k) {
        try {
            List<UserStory> similarUserStories = UtilRecommendation.getSimilarUserStories(userStoriesSimilarities, k);
            List<TestSpecification> relevantTestCases= new ArrayList<>();
            for (UserStory similarUS: similarUserStories) {
                ListResponseEvent<TestSpecification> testSpecificationListResponseEvent = testSpecificationService.listTestSpecificationFromIssue(similarUS.getProject().getId(), similarUS.getId());
                for (TestSpecification testSpecification: testSpecificationListResponseEvent.getObjects()) {
                    TestSpecification originalSpecification = getOriginalSpecification(testSpecification);
                    if (!relevantTestCases.contains(originalSpecification)) relevantTestCases.add(originalSpecification);
                }
            }

            LinkedList<TestSpecification> recommendableTestCases = orderTestCases(userStoriesSimilarities, similarUserStories, relevantTestCases);
            LinkedList<RecommendableItem> recommendableItems = convertToGenericItems(recommendableTestCases);
            return recommendableItems;
        } catch (Exception e){
            return new LinkedList<>();
        }
    }

    private TestSpecification getOriginalSpecification(TestSpecification recommendedSpecification) throws EventException {
        TestSpecification original;
        if (recommendedSpecification.getOriginalId() != null){
            ViewResponseEvent<TestSpecification> response = testSpecificationService.findOne(recommendedSpecification.getOriginalId());
            original = response.getObject();
        } else {
            original = recommendedSpecification;
        }
        return original;
    }

    private LinkedList<RecommendableItem> convertToGenericItems(LinkedList<TestSpecification> recommendableTestCases){
        LinkedList<RecommendableItem> recommendableItems = new LinkedList<>();
        for (TestSpecification testSpecification : recommendableTestCases) {
            recommendableItems.add(new RecommendableItem(testSpecification.getId(), RecommendableItemType.TEST_CASE));
        }
        return recommendableItems;
    }

    private LinkedList<TestSpecification> orderTestCases(TreeMap<Double, List<UserStory>> userStoriesSimilarities, List<UserStory> similarUserStories, List<TestSpecification> relevantTestCases) throws EventException {
        HashMap<TestSpecification,Double> testSpecificationRelevances = calculateRelevance(userStoriesSimilarities, similarUserStories, relevantTestCases);
        LinkedList<TestSpecification> recommendableItems = new LinkedList<>();
        for(Map.Entry<TestSpecification,Double> testRelevance : testSpecificationRelevances.entrySet()) {
            if(recommendableItems.isEmpty()) recommendableItems.add(testRelevance.getKey());
            else {
                int size = recommendableItems.size();
                boolean inserted = false;
                for(int i =0; i < size; i++) {
                    if(testSpecificationRelevances.get(recommendableItems.get(i)) < testRelevance.getValue()) {
                        recommendableItems.add(i, testRelevance.getKey());
                        inserted = true;
                        break;
                    }
                }
                if(!inserted) {
                    recommendableItems.add(testRelevance.getKey());
                }
            }
        }
        return recommendableItems;
    }

    private HashMap<TestSpecification, Double> calculateRelevance(TreeMap<Double, List<UserStory>> userStoriesSimilarities, List<UserStory> similarUserStories, List<TestSpecification> relevantTestCases) throws EventException {
        HashMap<TestSpecification,Double> testSpecificationRelevances = new HashMap<>();
        for(TestSpecification testSpecification: relevantTestCases ) {
            for(Map.Entry<Double,List<UserStory>> userStorySimilarity : userStoriesSimilarities.entrySet()) {
                for(UserStory userStory: userStorySimilarity.getValue()) {
                    ListResponseEvent<TestSpecification> testSpecificationListResponseEvent = testSpecificationService.listTestSpecificationFromIssue(userStory.getProject().getId(), userStory.getId());
                    if (similarUserStories.contains(userStory) && containsOriginalSpecification(testSpecificationListResponseEvent.getObjects(), testSpecification.getId())) {
                        if(testSpecificationRelevances.containsKey(testSpecification)) {
                            testSpecificationRelevances.put(testSpecification, testSpecificationRelevances.get(testSpecification) + userStorySimilarity.getKey());
                        }else {
                            testSpecificationRelevances.put(testSpecification,userStorySimilarity.getKey());
                        }
                    }
                }

            }
        }
        return testSpecificationRelevances;
    }

    private boolean containsOriginalSpecification(List<TestSpecification> testSpecifications, Integer originalId){
        for (TestSpecification specification : testSpecifications) {
            if (specification.getOriginalId() == originalId) return true;
        }
        return false;
    }


}
