package local.promptmark.dto;

import java.io.Serializable;

/**
 * Session-scoped cart entry. We snapshot title/price/type at add-time so the
 * cart view can render without a DB round-trip; the OrderService re-fetches
 * the plugin before payment to re-validate.
 */
public final class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long pluginId;
    private final String title;
    private final int price;
    private final PluginType type;

    public CartItem(long pluginId, String title, int price, PluginType type) {
        this.pluginId = pluginId;
        this.title = title;
        this.price = price;
        this.type = type;
    }

    public long getPluginId() { return pluginId; }
    public String getTitle() { return title; }
    public int getPrice() { return price; }
    public PluginType getType() { return type; }
    public String getTypeName() { return type == null ? null : type.name(); }
}
