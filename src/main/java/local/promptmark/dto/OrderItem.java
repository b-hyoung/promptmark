package local.promptmark.dto;

/** Row mapping for {@code order_items}, with the plugin title joined in by the DAO. */
public final class OrderItem {

    private final long id;
    private final long orderId;
    private final long pluginId;
    private final int pricePaid;
    private final String title;

    public OrderItem(long id, long orderId, long pluginId,
                     int pricePaid, String title) {
        this.id = id;
        this.orderId = orderId;
        this.pluginId = pluginId;
        this.pricePaid = pricePaid;
        this.title = title;
    }

    public long getId() { return id; }
    public long getOrderId() { return orderId; }
    public long getPluginId() { return pluginId; }
    public int getPricePaid() { return pricePaid; }
    public String getTitle() { return title; }
}
