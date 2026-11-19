package estrella.tienda.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Product {

    private String id;
    private String name;
    private String price;
    private String quantity;
    private String image;
    private String cleanImage;
    private String url;
    private String seller;

    public Product() {
    }

    public Product(
            String name,
            String price,
            String quantity,
            String image,
            String url,
            String seller) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.image = image;
        this.cleanImage = null;
        this.url = url;
        this.seller = seller;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getImage() {
        return image;
    }

    public String getUrl() {
        return url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(String price) {
        this.price = price;
    }


    @JsonAlias("cuantity")
    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCleanImage() {
        return cleanImage;
    }

    public void setCleanImage(String cleanImage) {
        this.cleanImage = cleanImage;
    }


    public String createImgName(){
        return name + "_" + quantity + "_" + seller;
    }

    public void generateId() {
        if (url != null && !url.isBlank()) {
            this.id = seller.toLowerCase() + ":" + Integer.toHexString(url.hashCode());
        }else{
        this.id = seller.toLowerCase() + ":"
                + Integer.toHexString((name + "|" + quantity).hashCode());
        }
    }
}
