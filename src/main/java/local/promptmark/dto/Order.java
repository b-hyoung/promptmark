package local.promptmark.dto;

import java.time.Instant;

/** Row mapping for {@code orders}. */
public final class Order {

    private final long id;
    private final long userId;
    private final int totalAmount;
    private final OrderStatus status;
    private final Instant createdAt;

    public Order(long id, long userId, int totalAmount,
                 OrderStatus status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public int getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public String getStatusName() { return status == null ? null : status.name(); }
}
