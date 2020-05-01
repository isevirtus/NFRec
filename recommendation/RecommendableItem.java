package br.edu.ufcg.embedded.turmalina.rest.controller.project.recommendation;

public class RecommendableItem {
    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RecommendableItemType getType() {
        return type;
    }

    public void setType(RecommendableItemType type) {
        this.type = type;
    }

    private RecommendableItemType type;

    public RecommendableItem(Integer id, RecommendableItemType type){
        this.id = id;
        this.type = type;
    }
}
