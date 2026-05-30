package local.promptmark.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import local.promptmark.dto.Tag;

/**
 * Tag lookup + asset_tags relationship management. Tag names are unique;
 * {@link #upsert(String)} returns the id whether the row already existed
 * or was just inserted.
 */
public class TagDao {

    private final DataSource ds;

    public TagDao(DataSource ds) {
        this.ds = ds;
    }

    public Optional<Tag> findByName(String name) {
        String sql = "SELECT id, name FROM tags WHERE name = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Tag(rs.getLong(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByName failed: " + e.getMessage(), e);
        }
    }

    /**
     * Insert-if-missing then return the id. Uses {@code ON CONFLICT DO NOTHING}
     * so concurrent insertions still resolve to the same row via a follow-up
     * SELECT.
     */
    public long upsert(String name) {
        try (Connection c = ds.getConnection()) {
            return upsertOn(c, name);
        } catch (SQLException e) {
            throw new RuntimeException("upsert tag failed: " + e.getMessage(), e);
        }
    }

    /** Same as {@link #upsert(String)} but participates in the caller's tx. */
    public long upsert(String name, Connection conn) {
        try {
            return upsertOn(conn, name);
        } catch (SQLException e) {
            throw new RuntimeException("upsert tag failed: " + e.getMessage(), e);
        }
    }

    private long upsertOn(Connection conn, String name) throws SQLException {
        // ON CONFLICT DO NOTHING returns no row when the tag already exists,
        // so we fall through to a SELECT.
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO tags (name) VALUES (?) ON CONFLICT (name) DO NOTHING RETURNING id")) {
            ins.setString(1, name);
            try (ResultSet rs = ins.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT id FROM tags WHERE name = ?")) {
            sel.setString(1, name);
            try (ResultSet rs = sel.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("tag '" + name + "' missing after upsert");
                }
                return rs.getLong(1);
            }
        }
    }

    /** Tags joined to an asset via {@code asset_tags}. */
    public List<Tag> findByAssetId(long assetId) {
        String sql = "SELECT t.id, t.name FROM tags t " +
                     "JOIN asset_tags at ON at.tag_id = t.id " +
                     "WHERE at.asset_id = ? " +
                     "ORDER BY t.name";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Tag> out = new ArrayList<>();
                while (rs.next()) out.add(new Tag(rs.getLong(1), rs.getString(2)));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByAssetId failed: " + e.getMessage(), e);
        }
    }

    /**
     * Atomically replace all tags on an asset. Caller manages the transaction;
     * we just consume the supplied connection.
     */
    public void replaceForAsset(long assetId, List<String> tagNames, Connection conn) {
        try {
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM asset_tags WHERE asset_id = ?")) {
                del.setLong(1, assetId);
                del.executeUpdate();
            }
            if (tagNames == null || tagNames.isEmpty()) return;

            // De-dup while preserving insertion order.
            Set<String> dedup = new LinkedHashSet<>();
            for (String t : tagNames) {
                if (t == null) continue;
                String norm = t.trim();
                if (norm.isEmpty()) continue;
                if (norm.length() > 40) norm = norm.substring(0, 40);
                dedup.add(norm);
            }
            for (String name : dedup) {
                long tagId = upsertOn(conn, name);
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO asset_tags (asset_id, tag_id) VALUES (?, ?) " +
                        "ON CONFLICT DO NOTHING")) {
                    ins.setLong(1, assetId);
                    ins.setLong(2, tagId);
                    ins.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("replaceForAsset failed: " + e.getMessage(), e);
        }
    }

    /** Up to {@code limit} tags ordered by name — used for the form picker. */
    public List<Tag> findAllOrLimit(int limit) {
        String sql = "SELECT id, name FROM tags ORDER BY name LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Tag> out = new ArrayList<>();
                while (rs.next()) out.add(new Tag(rs.getLong(1), rs.getString(2)));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAllOrLimit failed: " + e.getMessage(), e);
        }
    }
}
