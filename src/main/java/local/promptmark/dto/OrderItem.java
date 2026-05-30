package local.promptmark.dto;

/** Row mapping for {@code order_items}, with the asset title joined in by the DAO. */
public final class OrderItem {

    private final long id;
    private final long orderId;
    private final long assetId;
    private final int pricePaid;
    private final String title;

    public OrderItem(long id, long orderId, long assetId,
                     int pricePaid, String title) {
        this.id = id;
        this.orderId = orderId;
        this.assetId = assetId;
        this.pricePaid = pricePaid;
        this.title = title;
    }

    public long getId() { return id; }
    public long getOrderId() { return orderId; }
    public long getAssetId() { return assetId; }
    public int getPricePaid() { return pricePaid; }
    public String getTitle() { return title; }
}
