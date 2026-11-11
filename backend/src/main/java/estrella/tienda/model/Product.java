package estrella.tienda.model;

public class Product {

    private String name;
    private String price;
    private String cuantity;
    private String image;
    private String cleanImage;
    private String url;
    private String seller;

    public Product() {
    }

    public Product(
            String name,
            String price,
            String cuantity,
            String image,
            String url,
            String seller) {
        this.name = name;
        this.price = price;
        this.cuantity = cuantity;
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

    public String getCuantity() {
        return cuantity;
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

    public void setCuantity(String cuantity) {
        this.cuantity = cuantity;
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

    public String getCleanImage() {
        return cleanImage;
    }

    public void setCleanImage(String cleanImage) {
        this.cleanImage = cleanImage;
    }

    public String getImgName(){
        return name + "_" + cuantity + "_" + seller;
    }
}
