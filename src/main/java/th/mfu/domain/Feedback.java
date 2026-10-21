package th.mfu.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Feedback {

    public static final String[] CATEGORIES = { "ทั่วไป", "ความสะอาด", "ความปลอดภัย", "สิ่งอำนวยความสะดวก", "อินเทอร์เน็ต", "อื่นๆ" };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser author;

    // tenant's room at the time of writing, if linked
    @ManyToOne
    private Room room;

    private String category;
    // 1-5 stars
    private int rating = 5;

    @Column(length = 1000)
    private String message;

    private LocalDateTime createdAt;
    private boolean readByOwner;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getAuthor() {
        return author;
    }
    public void setAuthor(AppUser author) {
        this.author = author;
    }

    public Room getRoom() {
        return room;
    }
    public void setRoom(Room room) {
        this.room = room;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public int getRating() {
        return rating;
    }
    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isReadByOwner() {
        return readByOwner;
    }
    public void setReadByOwner(boolean readByOwner) {
        this.readByOwner = readByOwner;
    }
}
