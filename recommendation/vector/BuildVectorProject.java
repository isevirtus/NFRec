package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.vector;

import br.edu.ufcg.embedded.turmalina.domain.general.project.*;
import br.edu.ufcg.embedded.turmalina.domain.projectmanager.userstory.UserStory;

import java.util.ArrayList;
import java.util.List;

public class BuildVectorProject implements BuildVector {
    @Override
    public double[] buildVector(UserStory issueTarget, UserStory otherIssue) {
        //Características do target
        Project projectTarget = issueTarget.getProject();
        ArrayList<String> targetArray = getProjectArray(projectTarget);


        //Características do teste
        Project otherProject = otherIssue.getProject();
        ArrayList<String> otherArray = getProjectArray(otherProject);

        double[] otherVector = buildProjectVector(targetArray, otherArray);

        return otherVector;
    }

    public List<String> generateTargetList(UserStory issueTarget) {
        return getProjectArray(issueTarget.getProject());
    }

    public ArrayList<String> getProjectArray(Project project){
        ArrayList<String> projectlist = new ArrayList<>();
        for(ProjectDomain projectDomain: project.getProjectDomains()) {
            projectlist.add(projectDomain.getName());
        }
        for(ProjectArchitecture projectArchitecture: project.getProjectArchitecture()) {
            projectlist.add(projectArchitecture.getName());
        }
        for(ProjectPlataform projectPlataform: project.getProjectPlataform()) {
            projectlist.add(projectPlataform.getName());
        }
        for(ProjectLanguages projectLanguage: project.getProjectLanguages()) {
            projectlist.add(projectLanguage.getName());
        }
        for(ProjectAPIs projectApi: project.getProjectApi()) {
            projectlist.add(projectApi.getName());
        }
        for(ProjectFramework projectFramework: project.getProjectFrameworks()) {
            projectlist.add(projectFramework.getName());
        }
        for(ProjectDataBase projectDataBase: project.getProjectDataBase()) {
            projectlist.add(projectDataBase.getName());
        }
//		for(ProjectOtherTechnologies projectOthertechnology: project.getProjectOtherTechnologies()) {
//			projectlist.add(projectOthertechnology.getName());
//		}
        return projectlist;
    }

    public double[] buildProjectVector(ArrayList<String> targetProject, ArrayList<String> otherProject){
        double[] projectVector = new double[targetProject.size()];
        for(int i=0; i<targetProject.size(); i++) {
            if(otherProject.contains(targetProject.get(i))) {
                projectVector[i] = 1.0;
            }else {
                projectVector[i] = 0.0;

            }
        }
        return projectVector;
    }
}
