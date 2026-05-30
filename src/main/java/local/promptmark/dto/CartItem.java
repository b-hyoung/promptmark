package local.promptmark.dto;

import java.io.Serializable;

/**
 * Session-scoped cart entry. We snapshot title/price/type at add-time so the
 * cart view can render without a DB round-trip; the OrderService re-fetches
 * the asset before payment to re-validate.
 */
public final class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long assetId;
    private final String title;
    private final int price;
    private final AssetType type;

    public CartItem(long assetId, String title, int price, AssetType type) {
        this.assetId = assetId;
        this.title = title;
        this.price = price;
        this.type = type;
    }

    public long getAssetId() { return assetId; }
    public String getTitle() { return title; }
    public int getPrice() { return price; }
    public AssetType getType() { return type; }
    public String getTypeName() { return type == null ? null : type.name(); }
}
