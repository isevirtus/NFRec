package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.vector;

import br.edu.ufcg.embedded.turmalina.domain.general.project.*;
import br.edu.ufcg.embedded.turmalina.domain.projectmanager.task.Task;
import br.edu.ufcg.embedded.turmalina.domain.projectmanager.userstory.UserStory;
import br.edu.ufcg.embedded.turmalina.repositories.projectmanager.TaskRepository;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Configurable
public class BuildVectorWithTasks implements BuildVector {

    private TaskRepository taskRepository;

    private BuildVectorProject buildVectorProject;

    public BuildVectorWithTasks(TaskRepository taskRepository){
        this.taskRepository = taskRepository;
        this.buildVectorProject = new BuildVectorProject();
    }

    @Override
    public double[] buildVector(UserStory issueTarget, UserStory otherIssue) {
        //Características do target
        Project projectTarget = issueTarget.getProject();
        ArrayList<String> targetArray = buildVectorProject.getProjectArray(projectTarget);
        targetArray = appendUserStoryTags(targetArray, issueTarget);


        //Características do teste
        Project otherProject = otherIssue.getProject();
        ArrayList<String> otherArray = buildVectorProject.getProjectArray(otherProject);
        otherArray = appendUserStoryTags(otherArray, otherIssue);

        double[] otherVector = buildVectorProject.buildProjectVector(targetArray, otherArray);

        return otherVector;

    }

    @Override
    public List<String> generateTargetList(UserStory issueTarget) {
        return buildVectorProject.getProjectArray(issueTarget.getProject());
    }

    private ArrayList<String> appendUserStoryTags(ArrayList<String> projectList, UserStory userStory){
        HashSet<Task> tasks = new HashSet<Task>(taskRepository.findByIssueId(userStory.getId()));
        for (Task task : tasks) {
            projectList.add(task.getWish().getValue());
        }
        return projectList;
    }
}
