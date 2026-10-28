package th.mfu.domain;

import java.time.LocalDateTime;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

/**
 * A parcel that arrived at the office for a room. Only parcels whose label shows the room number can be
 * recorded; the others wait at the office until the owner comes to look for them.
 */
@Entity
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // many parcels -> one room; only tenants of this room see the parcel
    @ManyToOne(optional = false)
    private Room room;

    // name on the label, if any
    private String recipientName;
    // delivery company, e.g. Kerry, Flash, Thailand Post
    private String carrier;
    private String note;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] photo;
    private String photoContentType;

    private LocalDateTime arrivedAt;
    // null = still waiting at the office
    private LocalDateTime pickedUpAt;

    public boolean hasPhoto() {
        return photoContentType != null;
    }

    public boolean isWaiting() {
        return pickedUpAt == null;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }
    public void setRoom(Room room) {
        this.room = room;
    }

    public String getRecipientName() {
        return recipientName;
    }
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getCarrier() {
        return carrier;
    }
    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }

    public byte[] getPhoto() {
        return photo;
    }
    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }
    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }
    public void setArrivedAt(LocalDateTime arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }
    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }
}
