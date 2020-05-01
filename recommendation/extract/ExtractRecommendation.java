package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.extract;

import br.edu.ufcg.embedded.turmalina.domain.projectmanager.userstory.UserStory;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.RecommendableItem;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public interface ExtractRecommendation {
    LinkedList<RecommendableItem> getRecommendation(TreeMap<Double, List<UserStory>> similarUserStories, int k);
}
