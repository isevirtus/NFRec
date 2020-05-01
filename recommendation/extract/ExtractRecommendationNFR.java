package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.extract;

import br.edu.ufcg.embedded.turmalina.domain.general.project.IssueNFR;
import br.edu.ufcg.embedded.turmalina.domain.projectmanager.userstory.UserStory;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.RecommendableItem;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.RecommendableItemType;
import br.edu.ufcg.embedded.turmalina.rest.util.recommendation.UtilRecommendation;

import java.util.*;

public class ExtractRecommendationNFR implements ExtractRecommendation {
    @Override
    public LinkedList<RecommendableItem> getRecommendation(TreeMap<Double, List<UserStory>> userStoriesSimilarities, int k) {
        List<UserStory> similarUserStories = UtilRecommendation.getSimilarUserStories(userStoriesSimilarities, k);
        List<IssueNFR> relevantNFRs= new ArrayList<>();
        for (UserStory similarUS: similarUserStories) {
            for (IssueNFR nfr: similarUS.getIssueNFRs()) {
                relevantNFRs.add(nfr);
            }
        }

        LinkedList<IssueNFR> recommendableNFRs = orderNFRs(userStoriesSimilarities, similarUserStories, relevantNFRs);
        LinkedList<RecommendableItem> recommendableItems = convertToGenericItems(recommendableNFRs);
        return recommendableItems;
    }

    private LinkedList<RecommendableItem> convertToGenericItems(LinkedList<IssueNFR> recommendableNFRs){
        LinkedList<RecommendableItem> recommendableItems = new LinkedList<>();
        for (IssueNFR nfr : recommendableNFRs) {
            recommendableItems.add(new RecommendableItem(nfr.getOriginalNFR().getId(), RecommendableItemType.NON_FUNCTIONAL_REQUIREMENT));
        }
        return recommendableItems;
    }

    private LinkedList<IssueNFR> orderNFRs(TreeMap<Double, List<UserStory>> userStoriesSimilarities, List<UserStory> similarUserStories, List<IssueNFR> relevantNFRs){
        HashMap<IssueNFR,Double> nfrRelevances = calculateRelevance(userStoriesSimilarities, similarUserStories, relevantNFRs);
        LinkedList<IssueNFR> recommendableItems = new LinkedList<>();
        for(Map.Entry<IssueNFR,Double> nfrRelevance : nfrRelevances.entrySet()) {
            if(recommendableItems.isEmpty()) recommendableItems.add(nfrRelevance.getKey());
            else {
                int size = recommendableItems.size();
                boolean inserted = false;
                for(int i =0; i < size; i++) {
                    if(nfrRelevances.get(recommendableItems.get(i)) < nfrRelevance.getValue()) {
                        recommendableItems.add(i, nfrRelevance.getKey());
                        inserted = true;
                        break;
                    }
                }
                if(!inserted) {
                    recommendableItems.add(nfrRelevance.getKey());
                }
            }
        }
        return recommendableItems;
    }

    private HashMap<IssueNFR, Double> calculateRelevance(TreeMap<Double, List<UserStory>> userStoriesSimilarities, List<UserStory> similarUserStories, List<IssueNFR> relevantNFRs) {
        HashMap<IssueNFR,Double> nfrRelevances = new HashMap<>();
        for(IssueNFR nfr: relevantNFRs ) {
            for(Map.Entry<Double,List<UserStory>> userStorySimilarity : userStoriesSimilarities.entrySet()) {
                for(UserStory userStory: userStorySimilarity.getValue()) {
                    if (similarUserStories.contains(userStory) && userStory.getIssueNFRs().contains(nfr)) {
                        if(nfrRelevances.containsKey(nfr)) {
                            nfrRelevances.put(nfr, nfrRelevances.get(nfr) + userStorySimilarity.getKey());
                        }else {
                            nfrRelevances.put(nfr,userStorySimilarity.getKey());
                        }
                    }
                }

            }
        }
        return nfrRelevances;
    }

}
