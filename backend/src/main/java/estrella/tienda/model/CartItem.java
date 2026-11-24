package estrella.tienda.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "cart_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sessionId", "productId"}),
        indexes = @Index(columnList = "sessionId")
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String sessionId;

    @Column(nullable = false, length = 200)
    private String productId;

    @Column(length = 300)
    private String name;

    @Column(length = 100)
    private String quantityLabel;

    @Column(length = 1000)
    private String image;

    @Column(length = 50)
    private String seller;

    private double unitPrice;

    private int units;

    public CartItem() {
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQuantityLabel() { return quantityLabel; }
    public void setQuantityLabel(String quantityLabel) { this.quantityLabel = quantityLabel; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getSeller() { return seller; }
    public void setSeller(String seller) { this.seller = seller; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }
}
