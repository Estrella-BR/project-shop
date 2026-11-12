package estrella.tienda.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {

    private String id;
    private Integer level;
    private String link;
    private String name;
    private List<Category> children;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Category> getChildren() { return children; }
    public void setChildren(List<Category> children) { this.children = children; }
}