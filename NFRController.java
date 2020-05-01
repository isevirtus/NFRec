package br.edu.ufcg.embedded.turmalina.rest.controller.project;

import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.RecommendableItem;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.Recommendation;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.extract.ExtractRecommendationNFR;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.similarity.CalculateSimilarityManhattan;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.vector.BuildVectorProject;
import br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation.vector.BuildVectorWithTasks;
import org.apache.commons.math3.ml.distance.CanberraDistance;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.ml.distance.ManhattanDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;


import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

import br.edu.ufcg.embedded.turmalina.domain.general.Module;
import br.edu.ufcg.embedded.turmalina.domain.general.Operation;
import br.edu.ufcg.embedded.turmalina.domain.general.Wish;
import br.edu.ufcg.embedded.turmalina.domain.general.project.IssueNFR;
import br.edu.ufcg.embedded.turmalina.domain.general.project.NFR;
import br.edu.ufcg.embedded.turmalina.domain.general.project.Project;
import br.edu.ufcg.embedded.turmalina.domain.general.project.ProjectAPIs;
import br.edu.ufcg.embedded.turmalina.domain.general.project.ProjectArchitecture;
import br.edu.ufcg.embedded.turmalina.domain.general.project.ProjectDataBase;
import br.edu.ufcg.embedded.turmalina.domain.general.project.ProjectDomain;
import br.edu.ufcg.embedded.turmalina.domain.general.project.ProjectFramework;
import br.edu.ufcg.embedded.turmalina.domain.general.project.ProjectLanguages;
import br.edu.ufcg.embedded.turmalina.domain.general.project.ProjectPlataform;
import br.edu.ufcg.embedded.turmalina.domain.general.project.Sentence;
import br.edu.ufcg.embedded.turmalina.domain.projectmanager.task.Task;
import br.edu.ufcg.embedded.turmalina.domain.projectmanager.userstory.UserStory;
import br.edu.ufcg.embedded.turmalina.generic.EventException;
import br.edu.ufcg.embedded.turmalina.repositories.general.ModuleRepository;
import br.edu.ufcg.embedded.turmalina.repositories.general.NFRRepository;
import br.edu.ufcg.embedded.turmalina.repositories.general.OperationRepository;
import br.edu.ufcg.embedded.turmalina.repositories.general.project.ProjectRepository;
import br.edu.ufcg.embedded.turmalina.repositories.general.SentenceRepository;
import br.edu.ufcg.embedded.turmalina.repositories.projectmanager.TaskRepository;
import br.edu.ufcg.embedded.turmalina.repositories.projectmanager.UserStoryRepository;
import br.edu.ufcg.embedded.turmalina.rest.domain.general.project.NFRRest;
import br.edu.ufcg.embedded.turmalina.rest.util.Constants;
import br.edu.ufcg.embedded.turmalina.rest.util.MetricType;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.text.DecimalFormat;

@Controller
@RequestMapping(Constants.URL_PROJECT)
public class NFRController {

	@Autowired
	private NFRRepository nfrRepository;

	@Autowired
	private SentenceRepository sentenceRepository;

	@Autowired
	private UserStoryRepository userStoryRepository;

	@Autowired
	private ModuleRepository moduleRepository;

	@Autowired
	private OperationRepository operationRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private TaskRepository taskRepository;

	private Recommendation recommendation;

	private static final int K = 3;
	private static final int N = 2;
	private static DecimalFormat df = new DecimalFormat("#.##");

	/*
	@PreAuthorize("hasAuthority('PROJECT_ISSUE_CREATE')")
	@RequestMapping(method = RequestMethod.GET, value = Constants.URL_PROJECT_ISSUE_NFR)
	public ResponseEntity<ArrayList<NFR>> getAllNFRs(){
		ArrayList<NFR> nfrs = (ArrayList<NFR>) nfrRepository.getAllNFRs();
		return new ResponseEntity<>(nfrs, HttpStatus.OK);
	}
	*/

	@RequestMapping(method = RequestMethod.GET, value = Constants.URL_PROJECT_ISSUE_NFR_TYPES)
	public ResponseEntity<ArrayList<String>> getAllNFRTypes(){
		ArrayList<String> types = (ArrayList<String>) nfrRepository.getAllNFRTypes();
		return new ResponseEntity<>(types, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('PROJECT_ISSUE_CREATE')")
	@RequestMapping(method = RequestMethod.GET, value = Constants.URL_PROJECT_ISSUE_NFR_ATTRIBUTES)
	public ResponseEntity<ArrayList<String>> getTypeAttributes(@PathVariable Integer typeId){
		ArrayList<String> attributes = (ArrayList<String>) nfrRepository.getTypeAttributes(typeId);
		return new ResponseEntity<>(attributes, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('PROJECT_ISSUE_CREATE')")
	@RequestMapping(method = RequestMethod.POST, value = Constants.URL_PROJECT_ISSUE_NFR)
	public ResponseEntity<NFR> saveNFR(@RequestBody NFRRest nfrRest) throws EventException{
		NFR nfr = nfrRest.toCore();
		nfr.getSentence().setValue(0);
		Sentence newSentence = sentenceRepository.save(nfr.getSentence());
		nfr.setSentences(new HashSet<Sentence>());
		nfr.getSentences().add(newSentence);
		NFR nfrs = nfrRepository.save(nfr);
		return new ResponseEntity<NFR>(nfrs, HttpStatus.OK);
	}


	@RequestMapping(method = RequestMethod.GET, value = Constants.METRIC_PRECISION_AND_RECALL_REPORT)
	public ResponseEntity<Void> generatePrecisionAndRecallReport() throws FileNotFoundException, UnsupportedEncodingException{
		crossValidation();
		return new ResponseEntity<>(HttpStatus.OK);
	}



	//@PreAuthorize("hasAuthority('PROJECT_ISSUE_CREATE')")
//	@RequestMapping(method = RequestMethod.GET, value = Constants.URL_PROJECT_ISSUE_ID_RECOMMEND_NFR)
//	public ResponseEntity<LinkedList<NFR>> recommendNFRs(@PathVariable Integer projectId,@PathVariable Integer moduleId,@PathVariable Integer operationId, @PathVariable Integer issueId) throws FileNotFoundException, UnsupportedEncodingException{
//
//		Project targetProject = projectRepository.findById(projectId);
//		Module targetModule = moduleRepository.getOne(moduleId);
//		Operation targetOperation = operationRepository.getOne(operationId);
//		ArrayList<UserStory> userStoryList = new ArrayList<UserStory>(userStoryRepository.findByModuleAndOperation(targetModule, targetOperation));
//
//		//getting nfrs of the target project
//		HashSet<NFR> ownNFRs = getProjectNFRs(userStoryList, projectId);
//
//		userStoryList = removeProjectUserStories(userStoryList, projectId);
//
//		HashMap<Project,ArrayList<UserStory>> projectUserStories = findProjectUserStories(userStoryList, targetProject);
//
//		TreeMap<Double,ArrayList<UserStory>> projectsSim;
//		//This distances can be calculated by others methods
//		if (issueId != null && !taskRepository.findByIssueId(issueId).isEmpty()) {
//			UserStory targetUserStory = userStoryRepository.getOne(issueId);
//			projectsSim = getSimilarityWithTasks(targetUserStory, targetProject, projectUserStories, MetricType.MANHATTAN);
//		}
//		else {
//			projectsSim = getSimilarity(targetProject, projectUserStories, MetricType.MANHATTAN);
//		}
//
//		//Criar estrutura com informações das Tasks da US Target
//
//		ArrayList<UserStory> similarUserStories = getSimilarUserStories(projectsSim, K);
//
//		HashMap<UserStory,HashSet<NFR>> userStoryNfrs = findProjectNfrs(userStoryList);
//		HashSet<NFR> nfrs = getRecommendedNFRsByUserStory(similarUserStories);
//
//		LinkedList<NFR> recommendedNfrs = orderNFRs(userStoryNfrs,projectsSim,nfrs,similarUserStories);
//
//		//Adding nfrs at the top of the list
//		for (NFR nfr : ownNFRs) {
//			if (!recommendedNfrs.contains(nfr)) {
//				recommendedNfrs.addFirst(nfr);
//			}else {
//				recommendedNfrs.remove(nfr);
//				recommendedNfrs.addFirst(nfr);
//			}
//		}
//
//
//		return new ResponseEntity<>(recommendedNfrs, HttpStatus.OK);
//	}

	@RequestMapping(method = RequestMethod.GET, value = Constants.URL_PROJECT_ISSUE_ID_RECOMMEND_NFR)
	public ResponseEntity<LinkedList<NFR>> recommendNFRs2(@PathVariable Integer projectId,@PathVariable Integer moduleId,@PathVariable Integer operationId, @PathVariable Integer issueId){

	    //this.recommendation = new Recommendation(new BuildVectorWithTasks(taskRepository), new CalculateSimilarityManhattan(), new ExtractRecommendationNFR());

	    //UserStory userStory = userStoryRepository.findOne(issueId);
		Project targetProject = projectRepository.findById(projectId);
		Module module = moduleRepository.getOne(moduleId);
		Operation operation = operationRepository.getOne(operationId);

        ArrayList<UserStory> userStoryList = new ArrayList<>(userStoryRepository.findByModuleAndOperation(module, operation));
        HashSet<NFR> ownNFRs = getProjectNFRs(userStoryList, targetProject.getId());
        userStoryList = removeProjectUserStories(userStoryList, targetProject.getId());

		LinkedList<RecommendableItem> recommendableItems = new LinkedList<>();

        if (issueId != null){
			this.recommendation = new Recommendation(new BuildVectorWithTasks(taskRepository), new CalculateSimilarityManhattan(), new ExtractRecommendationNFR());
			UserStory userStory = userStoryRepository.findOne(issueId);
			recommendableItems = this.recommendation.getRecommendation(userStory, userStoryList, K);
        } else {
			this.recommendation = new Recommendation(new BuildVectorProject(), new CalculateSimilarityManhattan(), new ExtractRecommendationNFR());
			recommendableItems = this.recommendation.getRecommendation(targetProject, module, operation, userStoryList, K);
		}

        LinkedList<NFR> originalRecommendation = getOriginalItems(recommendableItems);

        addOwnNFRs(ownNFRs, originalRecommendation);

        return new ResponseEntity<>(originalRecommendation, HttpStatus.OK);
	}

	private LinkedList<NFR> getOriginalItems(LinkedList<RecommendableItem> recommendableItems){
		LinkedList<NFR> nfrs = new LinkedList<>();
		for (RecommendableItem item : recommendableItems) {
			nfrs.add(nfrRepository.findOne(item.getId()));
		}
		return nfrs;
	}

	private void addOwnNFRs(HashSet<NFR> ownNFRs, LinkedList<NFR> recommendation){
		for (NFR nfr : ownNFRs) {
			if (!recommendation.contains(nfr)) {
				recommendation.addFirst(nfr);
			}else {
				recommendation.remove(nfr);
				recommendation.addFirst(nfr);
			}
		}
	}

	@PreAuthorize("hasAuthority('PROJECT_ISSUE_CREATE')")
	@RequestMapping(method = RequestMethod.GET, value = Constants.URL_PROJECT_ISSUE_RECOMMEND_NFR)
	public ResponseEntity<LinkedList<NFR>> recommendNFRs(@PathVariable Integer projectId,@PathVariable Integer moduleId,@PathVariable Integer operationId) throws FileNotFoundException, UnsupportedEncodingException{

		return recommendNFRs2(projectId, moduleId, operationId, null);
	}



	public HashMap<UserStory,HashSet<NFR>> findProjectNfrs(ArrayList<UserStory> userStoryList){

		HashMap<UserStory, HashSet<NFR>> userStoryNfrs = new HashMap<UserStory,HashSet<NFR>>();
		for(UserStory userStory: userStoryList) {
			if(userStoryNfrs.containsKey(userStory)) {
				userStoryNfrs.get(userStory).addAll(getNFRs(userStory));
			}else {
				userStoryNfrs.put(userStory, getNFRs(userStory));
			}
		}
		return userStoryNfrs;
	}

	public HashMap<Project,ArrayList<UserStory>> findProjectUserStories(ArrayList<UserStory> userStoryList, Project targetProject){

		HashMap<Project, ArrayList<UserStory>> projectUserStories = new HashMap<Project, ArrayList<UserStory>>();
		for(UserStory userStory: userStoryList) {
			if(haveSamePlatform(targetProject, userStory.getProject())) {
				if(projectUserStories.containsKey(userStory.getProject())) {
					projectUserStories.get(userStory.getProject()).add(userStory);
				}else {
					ArrayList<UserStory> newList = new ArrayList<UserStory>();
					newList.add(userStory);
					projectUserStories.put(userStory.getProject(), newList);
				}
			}
		}



		return projectUserStories;
	}

	private boolean haveSamePlatform(Project targetProject, Project testProject) {
		for (ProjectPlataform platform : targetProject.getProjectPlataform()) {
			for (ProjectPlataform testPlatform: testProject.getProjectPlataform()) {
				if (platform.getName().equals(testPlatform.getName())){
					return true;
				}
			}
		}
		return true;
	}

	public ArrayList<UserStory> removeProjectUserStories(ArrayList<UserStory> userStoryList, Integer projectId) {
		Iterator<UserStory> i = userStoryList.iterator();
		while (i.hasNext()) {
			UserStory userStory = i.next(); // must be called before you can call i.remove()
			if(userStory.getProject().getId() == projectId) {
				i.remove();
			}
		}
		return userStoryList;
	}

	public HashSet<NFR> getProjectNFRs(ArrayList<UserStory> userStoryList, Integer projectId) {
		HashSet<NFR> nfrs = new HashSet<>();
		Iterator<UserStory> i = userStoryList.iterator();
		while (i.hasNext()) {
			UserStory userStory = i.next(); // must be called before you can call i.remove()
			if(userStory.getProject().getId() == projectId) {
				nfrs.addAll(getNFRs(userStory));
			}
		}
		return nfrs;
	}


	private HashSet<NFR> getNFRs(UserStory userStory){
		HashSet<NFR> nfrs = new HashSet<NFR>();
		for(IssueNFR issueNFR: userStory.getIssueNFRs()) {
			nfrs.add(nfrRepository.getOne(issueNFR.getOriginalNFR().getId()));
			//nfrs.add(issueNFR);
		}
		return nfrs;
	}

	public HashSet<NFR> getNFRsByUserStory(HashMap<UserStory,HashSet<NFR>> userStoriesNfrs){
		HashSet<NFR> nfrs = new HashSet<NFR>();
		for(Map.Entry<UserStory,HashSet<NFR>> usNFR : userStoriesNfrs.entrySet()) {
			nfrs.addAll(usNFR.getValue());
		}
		return nfrs;
	}

	public HashSet<NFR> getRecommendedNFRsByUserStory(ArrayList<UserStory> similarUserStories){
		HashSet<NFR> recommendedNfrs = new HashSet<NFR>();
		for(UserStory userStory: similarUserStories) {
			recommendedNfrs.addAll(getNFRs(userStory));
		}
		return recommendedNfrs;
	}

	private HashMap<Project,HashSet<NFR>> getRecommendedProjectNFRsByTasks(ArrayList<Project> similarProjects, HashMap<Project,ArrayList<UserStory>> projectUserStories, UserStory userStoryTarget){
		HashMap<Project,HashSet<NFR>> projectNfrs = new HashMap<Project,HashSet<NFR>>();
		for(Project project: similarProjects) {
			projectNfrs.put(project,getBestNFRsByProject(projectUserStories.get(project), userStoryTarget));
		}
		return projectNfrs;
	}

	private HashSet<NFR> getBestNFRsByProject(ArrayList<UserStory> projectUserStories, UserStory userStoryTarget){
		TreeMap<Double, ArrayList<UserStory>> userStorySim = new TreeMap<Double, ArrayList<UserStory>>();
		HashSet<Task> targetTasks = new HashSet<Task>(taskRepository.findByIssueId(userStoryTarget.getId()));
		for(UserStory userStory: projectUserStories) {
			HashSet<Task> testTasks = new HashSet<Task>(taskRepository.findByIssueId(userStory.getId()));
			Double jaccardSimilarity = getJaccardSimilarityByTasks(targetTasks, testTasks);
			if(userStorySim.containsKey(jaccardSimilarity)) {
				userStorySim.get(jaccardSimilarity).add(userStory);
			}else {
				ArrayList<UserStory> userStoryList = new ArrayList<UserStory>();
				userStoryList.add(userStory);
				userStorySim.put(jaccardSimilarity, userStoryList);
			}
		}

		HashSet<NFR> recommendedNfrs = new HashSet<NFR>();
		for(UserStory userStory: userStorySim.get(userStorySim.lastKey())) {
			recommendedNfrs.addAll(getNFRs(userStory));
		}
		return recommendedNfrs;
	}

	private Double getJaccardSimilarityByTasks(HashSet<Task> targetTasks, HashSet<Task> testTasks) {
		HashSet<Wish> targetWishSet = buildWishSet(targetTasks);
		HashSet<Wish> testWishSet = buildWishSet(testTasks);
		Set<Wish> intersection = new HashSet<Wish>(targetWishSet);
		intersection.retainAll(testWishSet);
		Set<Wish> union = new HashSet<Wish>(targetWishSet);
		union.addAll(testWishSet);
		if(union.size() == 0 ) return 0.0;
		else return ((double)intersection.size() / (double) union.size());

	}

	private HashSet<Wish> buildWishSet(HashSet<Task> tasks){
		HashSet<Wish> wishSet = new HashSet<Wish>();
		for(Task task: tasks) {
			wishSet.add(task.getWish());
		}
		return wishSet;
	}

	private LinkedList<NFR> orderNFRs(HashMap<UserStory,HashSet<NFR>> userStoryNfrs, TreeMap<Double,ArrayList<UserStory>> userStoriesSim, HashSet<NFR> recommendedNFRs, ArrayList<UserStory> similarUserStories){
		HashMap<NFR, Double> nfrRelevances = calculateRelevance(userStoryNfrs, userStoriesSim, recommendedNFRs,
				similarUserStories);
		LinkedList<NFR> nfrs = new LinkedList<NFR>();
		for(Map.Entry<NFR,Double> nfrRelevance : nfrRelevances.entrySet()) {
			if(nfrs.isEmpty()) nfrs.add(nfrRelevance.getKey());
			else {
				int size = nfrs.size();
				boolean inserted = false;
				for(int i =0; i < size; i++) {
					if(nfrRelevances.get(nfrs.get(i)) < nfrRelevance.getValue()) {
						nfrs.add(i, nfrRelevance.getKey());
						inserted = true;
						break;
					}
				}
				if(!inserted) {
					nfrs.add(nfrRelevance.getKey());
				}
			}
		}
		return nfrs;

	}

	private HashMap<NFR, Double> calculateRelevance(HashMap<UserStory, HashSet<NFR>> userStoryNfrs,
												   TreeMap<Double, ArrayList<UserStory>> userStoriesSim, HashSet<NFR> recommendedNFRs,
												   ArrayList<UserStory> similarUserStories) {
		HashMap<NFR,Double> nfrRelevances = new HashMap<NFR,Double>();
		for(NFR nfr: recommendedNFRs ) {
			for(Map.Entry<Double,ArrayList<UserStory>> userStorySimilarity : userStoriesSim.entrySet()) {
				for(UserStory userStory: userStorySimilarity.getValue()) {
					if (similarUserStories.contains(userStory) && userStoryNfrs.get(userStory).contains(nfr)) {
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

	private ArrayList<UserStory> getSimilarUserStories(TreeMap<Double,ArrayList<UserStory>> userStoriesSimilarities, int K){
		ArrayList<UserStory> similarUserStories = new ArrayList<UserStory> ();

		for(Map.Entry<Double,ArrayList<UserStory>> userStorySimilarity : userStoriesSimilarities.entrySet()) {
			ArrayList<UserStory> userStories = userStorySimilarity.getValue();
			for(UserStory us: userStories) {
				similarUserStories.add(us);
				if(similarUserStories.size() == K) return similarUserStories;
			}
		}

		return similarUserStories;
	}


	private TreeMap<Double,ArrayList<UserStory>> getSimilarity(Project targetProject, HashMap<Project,ArrayList<UserStory>> projectUserStories,MetricType metric){
		ArrayList<String> targetList = generateProjectList(targetProject);
		double[] targetVector = buildTargetVector(targetList);
		TreeMap<Double,ArrayList<UserStory>> similarityMap = new TreeMap<Double,ArrayList<UserStory>>(Collections.reverseOrder());
		for(Map.Entry<Project,ArrayList<UserStory>> project : projectUserStories.entrySet()) {
			for (UserStory userStory : project.getValue()) {
				HashSet<String> userStorySet = generateProjectSet(userStory.getProject());
				HashSet<String> targetSet = generateProjectSet(targetProject);
				double[] userStoryVector = buildProjectVector(targetList, userStorySet);



				double similarity = 0.0;
				switch( metric )
				{
					case EUCLIDEAN:
						similarity = getEuclideanSimilarity(targetVector,userStoryVector);
						break;
					case MANHATTAN:
						similarity = getManhattanSimilarity(targetVector,userStoryVector);
						break;
					case COSINE:
						similarity = getCosineSimilarity(targetVector,userStoryVector);
						break;
					case CANBERRA:
						similarity = getCanberraSimilarity(targetVector,userStoryVector);
						break;
					case JACCARD:
						similarity = getJaccardSimilarity(targetSet,userStorySet);
						break;
					default:
						break;
				}
				if(similarityMap.containsKey(similarity) && similarity > 0) {
					similarityMap.get(similarity).add(userStory);
				}else {
					if (similarity > 0) {
						ArrayList<UserStory> newProjectList = new ArrayList<UserStory>();
						newProjectList.add(userStory);
						similarityMap.put(similarity, newProjectList);
					}
				}
			}
		}
		return similarityMap;
	}

	private TreeMap<Double,ArrayList<UserStory>> getSimilarityWithTasks(UserStory targetUserStory, Project targetProject, HashMap<Project,ArrayList<UserStory>> projectUserStories,MetricType metric){
		ArrayList<String> targetList = generateProjectList(targetProject);
		targetList = appendUserStoryTags(targetList, targetUserStory);
		HashMap<UserStory, Integer> similarTasks = new HashMap<>();

		double[] targetVector = buildTargetVector(targetList);
		TreeMap<Double,ArrayList<UserStory>> similarityMap = new TreeMap<Double,ArrayList<UserStory>>(Collections.reverseOrder());
		for(Map.Entry<Project,ArrayList<UserStory>> project : projectUserStories.entrySet()) {
			for (UserStory userStory : project.getValue()) {
				HashSet<String> userStorySet = generateProjectSet(userStory.getProject());
				userStorySet = appendUserStoryTags(userStorySet, userStory);
				HashSet<String> targetSet = generateProjectSet(targetProject);
				targetSet = appendUserStoryTags(targetSet, targetUserStory);

				double[] userStoryVector = buildProjectVector(targetList, userStorySet);


				similarTasks.put(userStory, getIntersection(appendUserStoryTags(new ArrayList<String>(), targetUserStory), userStorySet));

				double similarity = 0.0;
				switch( metric )
				{
					case EUCLIDEAN:
						similarity = getEuclideanSimilarity(targetVector,userStoryVector);
						break;
					case MANHATTAN:
						similarity = getManhattanSimilarity(targetVector,userStoryVector);
						break;
					case COSINE:
						similarity = getCosineSimilarity(targetVector,userStoryVector);
						break;
					case CANBERRA:
						similarity = getCanberraSimilarity(targetVector,userStoryVector);
						break;
					case JACCARD:
						similarity = getJaccardSimilarity(targetSet,userStorySet);
						break;
					default:
						break;
				}
				if(similarityMap.containsKey(similarity) && similarity > 0) {
					//similarityMap.get(similarity).add(userStory);
					orderlyInsertion(userStory, similarityMap.get(similarity), similarTasks);
				}else {
					if (similarity > 0) {
						ArrayList<UserStory> newProjectList = new ArrayList<UserStory>();
						newProjectList.add(userStory);
						similarityMap.put(similarity, newProjectList);
					}
				}
			}
		}
		return similarityMap;
	}

	private void orderlyInsertion(UserStory userStory, ArrayList<UserStory> orderedList, HashMap<UserStory, Integer> similarTasks){
		if(orderedList.isEmpty()) orderedList.add(userStory);
		else {
			int size = orderedList.size();
			boolean inserted = false;
			for(int i =0; i < size; i++) {
				if(similarTasks.get(orderedList.get(i)) < similarTasks.get(userStory)) {
					orderedList.add(i, userStory);
					inserted = true;
					break;
				}
				//Ordena por data
				else if (similarTasks.get(orderedList.get(i)) == similarTasks.get(userStory)){
					if (userStory.getStartDate().after(orderedList.get(i).getStartDate())){
						orderedList.add(i, userStory);
						inserted = true;
						break;
					}
				}
			}
			if(!inserted) {
				orderedList.add(userStory);
			}
		}
	}

	private int getIntersection(ArrayList<String> targetUS, HashSet<String> testUS){
		int sum = 0;
		for (String us : targetUS) {
			if (testUS.contains(us)) {
				sum++;
			}
		}
		return sum;
	}

	private Double getManhattanSimilarity(double[] targetVector, double[] projectVector) {
		ManhattanDistance manhattanDistance = new ManhattanDistance();
		Double distance = manhattanDistance.compute(targetVector,projectVector);
		if(targetVector.length == 0 ) {
			return 1.0;
		}else {
			return 1.0 - (distance / targetVector.length);
		}
	}

	private Double getEuclideanSimilarity(double[] targetVector, double[] projectVector) {
		EuclideanDistance euclideanDistance = new EuclideanDistance();
		Double distance = euclideanDistance.compute(targetVector,projectVector);
		return 1.0/(1.0 + distance);
	}

	private static Double getCosineSimilarity(double[] targetVector, double[] projectVector) {
		Double numerator = 0.0;
		for(int i =0; i < targetVector.length; i++) {
			numerator += targetVector[i] * projectVector[i];
		}
		Double denominator = getSquareRooted(targetVector) * getSquareRooted(projectVector);
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

	private Double getCanberraSimilarity(double[] targetVector, double[] projectVector) {
		CanberraDistance canberraDistance = new CanberraDistance();
		Double distance = canberraDistance.compute(targetVector,projectVector);
		return 1.0/(1.0 + distance);

	}

	private Double getJaccardSimilarity(Set<String> targetSet, Set<String> projectSet) {
		Set<String> intersection = new HashSet<String>(targetSet);
		intersection.retainAll(projectSet);
		Set<String> union = new HashSet<String>(targetSet);
		union.addAll(projectSet);
		if(union.size() == 0) return 1.0;
		return ((double)intersection.size()) /((double)union.size());
	}

	private ArrayList<String> generateProjectList(Project project) {
		ArrayList<String> projectlist = new ArrayList<String>();
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

	private ArrayList<String> appendUserStoryTags(ArrayList<String> projectList, UserStory userStory){
		HashSet<Task> tasks = new HashSet<Task>(taskRepository.findByIssueId(userStory.getId()));
		for (Task task : tasks) {
			projectList.add(task.getWish().getValue());
		}
		return projectList;
	}

	private ArrayList<String> appendUserStoryTags2(ArrayList<String> projectList, UserStory userStory, HashMap<UserStory, ArrayList<Task>> usTasks){
		for (Map.Entry<UserStory, ArrayList<Task>> us : usTasks.entrySet()) {
			if (us.getKey().equals(userStory)){
				for (Task task : us.getValue()) {
					projectList.add(task.getWish().getValue());
				}
			}
		}

		return projectList;
	}

	private HashSet<String> appendUserStoryTags2(HashSet<String> projectList, UserStory userStory, HashMap<UserStory, ArrayList<Task>> usTasks){
		for (Map.Entry<UserStory, ArrayList<Task>> us : usTasks.entrySet()) {
			if (us.getKey().equals(userStory)){
				for (Task task : us.getValue()) {
					projectList.add(task.getWish().getValue());
				}
			}
		}

		return projectList;
	}

	private HashSet<String> generateProjectSet(Project project) {
		HashSet<String> projectSet = new HashSet<String>();
		for(ProjectDomain projectDomain: project.getProjectDomains()) {
			projectSet.add(projectDomain.getName());
		}
		for(ProjectArchitecture projectArchitecture: project.getProjectArchitecture()) {
			projectSet.add(projectArchitecture.getName());
		}
		for(ProjectPlataform projectPlataform: project.getProjectPlataform()) {
			projectSet.add(projectPlataform.getName());
		}
		for(ProjectLanguages projectLanguage: project.getProjectLanguages()) {
			projectSet.add(projectLanguage.getName());
		}
		for(ProjectAPIs projectApi: project.getProjectApi()) {
			projectSet.add(projectApi.getName());
		}
		for(ProjectFramework projectFramework: project.getProjectFrameworks()) {
			projectSet.add(projectFramework.getName());
		}
		for(ProjectDataBase projectDataBase: project.getProjectDataBase()) {
			projectSet.add(projectDataBase.getName());
		}
//		for(ProjectOtherTechnologies projectOthertechnology: project.getProjectOtherTechnologies()) {
//			projectSet.add(projectOthertechnology.getName());
//		}
		return projectSet;
	}

	private HashSet<String> appendUserStoryTags(HashSet<String> projectList, UserStory userStory){
		HashSet<Task> tasks = new HashSet<Task>(taskRepository.findByIssueId(userStory.getId()));
		for (Task task : tasks) {
			projectList.add(task.getWish().getValue());
		}
		return projectList;
	}

	private double[] buildProjectVector(ArrayList<String> targetProject, HashSet<String> otherProject){
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

	private double[] buildTargetVector(ArrayList<String> targetProject){
		double[] projectVector = new double[targetProject.size()];
		for(int i=0; i<targetProject.size(); i++) {
			projectVector[i] = 1.0;
		}
		return projectVector;
	}

	private List<Project> getProjects(HashMap<Project, ArrayList<UserStory>> projectsMap){
		List<Project> projectList = new ArrayList<Project>();
		for (Map.Entry<Project, ArrayList<UserStory>> project : projectsMap.entrySet()) {
			projectList.add(project.getKey());
		}
		return projectList;
	}

	private void crossValidation() throws FileNotFoundException, UnsupportedEncodingException {
		HashMap<Project, ArrayList<UserStory>> projectsMap = generateProjectsMap();
		List<Project> projectList = getProjects(projectsMap);
		Set<Set<Project>> allCombinations = combinations(projectList, N);

		String workDir = System.getProperty("user.dir");
		PrintWriter pw = new PrintWriter("/home/marcos/team-formation/crossValidation.csv");
		StringBuilder sb = new StringBuilder();
		sb = buildAtributteLine(sb);

		HashMap<Project, ArrayList<UserStory>> testProjects = new HashMap<Project, ArrayList<UserStory>>();
		for (Set<Project> combination : allCombinations) {
			for (Project project: combination) {
				testProjects.put(project,projectsMap.get(project));
			}
			String testProjectsFormated = projectHashMapToString(testProjects);
			HashMap<Project, ArrayList<UserStory>> trainingProjects = getDifferenceBeetwenTwoMaps(projectsMap, testProjects);
			for (Map.Entry<Project, ArrayList<UserStory>> projectTarget : testProjects.entrySet()) {

				int[] Ks = {1,2,3,4,5,6,7,8,9,10,11};
				for(MetricType metricType: MetricType.values()) {
					for(int k: Ks) {
						ArrayList<Double> precisionList = new ArrayList<Double>();
						ArrayList<Double> recallList = new ArrayList<Double>();
						ArrayList<Double> falsePositiveRateList = new ArrayList<Double>();
						for(UserStory userStoryTarget: projectTarget.getValue()){
							if(userStoryTarget.getModule() != null && userStoryTarget.getOperation() != null && !userStoryTarget.getIssueNFRs().isEmpty()) {
								ArrayList<UserStory> userStoryList = new ArrayList<UserStory>();
								for (Map.Entry<Project, ArrayList<UserStory>> trainingProject : trainingProjects.entrySet()) {
									userStoryList.addAll(userStoryRepository.findByProjectAndModuleAndOperation(trainingProject.getKey(), userStoryTarget.getModule(), userStoryTarget.getOperation()));
								}
								HashMap<Project,ArrayList<UserStory>> projectUserStories = findProjectUserStories(userStoryList, projectTarget.getKey());
								TreeMap<Double,ArrayList<UserStory>> projectsSim;
								//This distances can be calculated by others methods
								if (userStoryTarget == null || taskRepository.findByIssueId(userStoryTarget.getId()).isEmpty()) {
									projectsSim = getSimilarity(userStoryTarget.getProject(), projectUserStories, metricType);
								}
								else {
									projectsSim = getSimilarityWithTasks(userStoryTarget, userStoryTarget.getProject(), projectUserStories, metricType);
								}
								ArrayList<UserStory> similarUserStories = getSimilarUserStories(projectsSim,k);


								// Not Considering tasks
										/*
										HashMap<Project,HashSet<NFR>>  projectNfrs = findProjectNfrs(userStoryList);
										HashSet<NFR> nfrs = getRecommendedNFRs(similarProjects,projectUserStories);
										*/

								//Considering tasks

								HashSet<NFR> recommendedNfrs = getRecommendedNFRsByUserStory(similarUserStories);

										/* Considering more relevant
										LinkedList<NFR> orderedNfrs = orderNFRs(projectNfrs,projectsSim,nfrs);
										recommendedNfrs = new HashSet<NFR>(getRelevantNfrs(orderedNfrs, X));
										*/

								double precision = calculatePrecision(getNFRs(userStoryTarget), recommendedNfrs);
								double recall = calculateRecall(getNFRs(userStoryTarget), recommendedNfrs);
								double falsePositiveRate = calculateFalsePositiveRate(getNFRs(userStoryTarget), recommendedNfrs);

								precisionList.add(precision);
								recallList.add(recall);
								falsePositiveRateList.add(falsePositiveRate);
							}

						}


						if(!precisionList.isEmpty() && !recallList.isEmpty() && !falsePositiveRateList.isEmpty()) {
							double precisionAvarage = calculateAverage(precisionList);
							double recallAvarage = calculateAverage(recallList);
							double falsePositiveRateAvarage = calculateAverage(falsePositiveRateList);

							sb  = buildReportLine(sb, projectTarget.getKey(), testProjectsFormated, metricType, k, precisionAvarage, recallAvarage, falsePositiveRateAvarage);
							System.out.println(sb.toString());
						}


					}
				}
			}
			testProjects = new HashMap<Project, ArrayList<UserStory>>();
		}
		pw.write(sb.toString());
		pw.close();
	}

	private String projectHashMapToString( HashMap<Project, ArrayList<UserStory>> testProjects) {
		String testProjectsToString = "";
		for (Map.Entry<Project, ArrayList<UserStory>> pT : testProjects.entrySet()) {
			if(!testProjectsToString.equals("")) testProjectsToString += "-" + Integer.toString(pT.getKey().getId());
			else testProjectsToString += Integer.toString(pT.getKey().getId());
		}
		return testProjectsToString;
	}

	private double calculateAverage(List <Double> values) {
		double sum = 0.0;
		if(!values.isEmpty()) {
			for (Double value : values) {
				sum += value;
			}
			return sum / values.size();
		}
		return sum;
	}

	// code found in https://codereview.stackexchange.com/questions/26854/recursive-method-to-return-a-set-of-all-combinations
	private Set<Set<Project>> combinations(List<Project> projects, int k) {

		Set<Set<Project>> allCombos = new HashSet<Set<Project>> ();
		// base cases for recursion
		if (k == 0) {
			// There is only one combination of size 0, the empty team.
			allCombos.add(new HashSet<Project>());
			return allCombos;
		}
		if (k > projects.size()) {
			// There can be no teams with size larger than the group size,
			// so return allCombos without putting any teams in it.
			return allCombos;
		}

		// Create a copy of the group with one item removed.
		List<Project> groupWithoutX = new ArrayList<Project> (projects);
		Project x = groupWithoutX.remove(groupWithoutX.size()-1);

		Set<Set<Project>> combosWithoutX = combinations(groupWithoutX, k);
		Set<Set<Project>> combosWithX = combinations(groupWithoutX, k-1);
		for (Set<Project> combo : combosWithX) {
			combo.add(x);
		}
		allCombos.addAll(combosWithoutX);
		allCombos.addAll(combosWithX);
		return allCombos;
	}

	private StringBuilder buildAtributteLine(StringBuilder sb) {
		sb.append("project_target");
		sb.append(',');
		sb.append("test_projects");
		sb.append(',');
		sb.append("metric_type");
		sb.append(',');
		sb.append("k");
		sb.append(',');
		sb.append("precision");
		sb.append(',');
		sb.append("recall");
		sb.append(',');
		sb.append("false_positive_rate");
		sb.append('\n');
		return sb;
	}

	private StringBuilder buildReportLine(StringBuilder sb,Project projectTarget , String testProjects, MetricType metricType, int k, double precisionAvarage, double recallAvarage, double falsePositiveRateAvarage) {

		sb.append(projectTarget.getFullName() + " (" + projectTarget.getId() + ") ");
		sb.append(',');
		sb.append(testProjects);
		sb.append(',');
		sb.append(metricType);
		sb.append(',');
		sb.append(k);
		sb.append(',');
		sb.append(df.format(precisionAvarage).replaceAll(",", "."));
		sb.append(',');
		sb.append(df.format(recallAvarage).replaceAll(",", "."));
		sb.append(',');
		sb.append(df.format(falsePositiveRateAvarage).replaceAll(",", "."));
		sb.append('\n');
		return sb;
	}

	private LinkedList<NFR> getRelevantNfrs(LinkedList<NFR> orderedNfrs, int n){
		LinkedList<NFR> relevantNfrs = new LinkedList<NFR>();
		if(orderedNfrs.size() <= n) {
			return new LinkedList<NFR>(orderedNfrs);
		}else {
			for(int i=0;i<n;i++) {
				relevantNfrs.add(orderedNfrs.getFirst());
				orderedNfrs.removeFirst();
			}
		}
		return relevantNfrs;
	}

	private double calculatePrecision(HashSet<NFR> ownNfrs, HashSet<NFR> recommendedNfrs) {
		Set<NFR> intersection = new HashSet<NFR>(ownNfrs);
		intersection.retainAll(recommendedNfrs);
		if(recommendedNfrs.isEmpty()) {
			return 0.0;
		}else {
			double precision = ((double) intersection.size()) / ((double) recommendedNfrs.size());
			return precision;
		}
	}

	private double calculateRecall(HashSet<NFR> ownNfrs, HashSet<NFR> recommendedNfrs) {
		Set<NFR> intersection = new HashSet<NFR>(ownNfrs);
		intersection.retainAll(recommendedNfrs);
		if(ownNfrs.isEmpty()) {
			return 0.0;
		}else {
			double recall = ((double) intersection.size()) / ((double) ownNfrs.size());
			return recall;
		}
	}

	private double calculateFalsePositiveRate(HashSet<NFR> ownNfrs, HashSet<NFR> recommendedNfrs) {
		HashSet<NFR> difference = new HashSet<NFR>(recommendedNfrs);
		difference.removeAll(ownNfrs);
		HashSet<NFR> allNfrs = new HashSet<NFR>(nfrRepository.getAllNFRs());
		allNfrs.removeAll(ownNfrs);
		return ((double)difference.size() / (double)allNfrs.size());
	}


	private HashMap<Project, ArrayList<UserStory>> getDifferenceBeetwenTwoMaps(HashMap<Project, ArrayList<UserStory>> projectsMap, HashMap<Project, ArrayList<UserStory>> tenPercentProjects) {
		HashMap<Project, ArrayList<UserStory>> bv = new HashMap<Project, ArrayList<UserStory>>(projectsMap);
		for (Project project : tenPercentProjects.keySet()) {
			bv.remove(project);
		}
		return bv;
	}

	private HashMap<Project, ArrayList<UserStory>> generateProjectsMap(){
		HashMap<Project, ArrayList<UserStory>> projectsMap = new HashMap<Project, ArrayList<UserStory>>();
		ArrayList<UserStory> allUserStories = (ArrayList<UserStory>) userStoryRepository.findAll();
		for(UserStory userStory: allUserStories) {
			if(projectsMap.containsKey(userStory.getProject())){
				projectsMap.get(userStory.getProject()).add(userStory);
			}else {
				ArrayList<UserStory> newUserStoryList = new ArrayList<UserStory>();
				newUserStoryList.add(userStory);
				projectsMap.put(userStory.getProject(), newUserStoryList);
			}
		}
		return projectsMap;
	}


}