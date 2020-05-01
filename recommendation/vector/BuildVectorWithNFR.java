package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.vector;

import br.edu.ufcg.embedded.turmalina.domain.general.project.*;
import br.edu.ufcg.embedded.turmalina.domain.projectmanager.userstory.UserStory;

import java.util.ArrayList;
import java.util.List;

public class BuildVectorWithNFR implements BuildVector {

    private BuildVectorProject buildVectorProject;

    public BuildVectorWithNFR(){
        this.buildVectorProject = new BuildVectorProject();
    }

    @Override
    public double[] buildVector(UserStory issueTarget, UserStory otherIssue) {
        //Características do target
        Project projectTarget = issueTarget.getProject();
        ArrayList<String> targetArray = buildVectorProject.getProjectArray(projectTarget);
        targetArray = appendNFRTags(targetArray, issueTarget);

        //Características do teste
        Project otherProject = otherIssue.getProject();
        ArrayList<String> otherArray = buildVectorProject.getProjectArray(otherProject);
        otherArray = appendNFRTags(otherArray, otherIssue);

        double[] otherVector = buildVectorProject.buildProjectVector(targetArray, otherArray);

        return otherVector;

    }

    @Override
    public List<String> generateTargetList(UserStory issueTarget) {
        return buildVectorProject.getProjectArray(issueTarget.getProject());
    }


    private ArrayList<String> appendNFRTags(ArrayList<String> projectList, UserStory userStory){
        List<IssueNFR> nfrs = userStory.getIssueNFRs();
        for (IssueNFR nfr : nfrs ) {
            NFR originalNFR = nfr.getOriginalNFR();
            projectList.add(originalNFR.getType() + " " + originalNFR.getAttribute() + " " + originalNFR.getSentence().getText());
        }
        return projectList;
    }
}
