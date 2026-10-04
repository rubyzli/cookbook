package family.cookbook.image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

// An uploaded or imported photo, served at /api/images/{id}
@Entity
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 50)
    private String contentType;
    // Mapped to bytea; @Lob would make it a Postgres large object instead
    @Column(nullable = false)
    private byte[] data;
    @Column(nullable = false)
    private Instant createdAt;

    protected Image() {
    }

    public Image(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = data;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }
}
