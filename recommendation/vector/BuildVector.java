package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.vector;

import br.edu.ufcg.embedded.turmalina.domain.projectmanager.userstory.UserStory;

import java.util.List;

public interface BuildVector {

    double[] buildVector(UserStory issueTarget, UserStory otherIssue);

    List<String> generateTargetList(UserStory issueTarget);
}
