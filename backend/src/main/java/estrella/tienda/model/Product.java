package estrella.tienda.model;

public class Product {

    private String name;
    private String price;
    private String cuantity;
    private String image;
    private String url;

    public Product() {
    }

    public Product(
            String name,
            String price,
            String cuantity,
            String image,
            String url) {
        this.name = name;
        this.price = price;
        this.cuantity = cuantity;
        this.image = image;
        this.url = url;
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
}
