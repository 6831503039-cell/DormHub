package th.mfu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DormHubApplicationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    public void allPagesLoad() throws Exception {
        String[] urls = { "/", "/rooms", "/rooms/new", "/rooms/1", "/tenants", "/tenants/new", "/tenants/1",
                "/bills", "/bills/new", "/bills/1", "/repairs", "/repairs/new", "/repairs/1" };
        for (String url : urls) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    public void unknownIdReturns404() throws Exception {
        mvc.perform(get("/rooms/999")).andExpect(status().isNotFound());
    }

    @Test
    public void addingTenantMarksRoomOccupied() throws Exception {
        Room room = roomRepository.findByRoomNumber("103").get();
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());

        mvc.perform(post("/tenants")
                .param("firstName", "ทดสอบ")
                .param("lastName", "ระบบ")
                .param("room", room.getId().toString()))
                .andExpect(redirectedUrl("/tenants"));

        assertEquals(RoomStatus.OCCUPIED, roomRepository.findById(room.getId()).get().getStatus());
    }

    @Test
    public void billWithoutRentUsesRoomRent() throws Exception {
        Room room = roomRepository.findByRoomNumber("202").get();

        mvc.perform(post("/bills")
                .param("room", room.getId().toString())
                .param("billMonth", "2026-11")
                .param("waterUnits", "0")
                .param("waterRate", "18")
                .param("electricUnits", "0")
                .param("electricRate", "8"))
                .andExpect(redirectedUrl("/bills"));

        mvc.perform(get("/bills")).andExpect(status().isOk());
    }

    @Test
    public void roomWithTenantsCannotBeDeleted() throws Exception {
        Room room = roomRepository.findByRoomNumber("101").get();

        mvc.perform(get("/rooms/" + room.getId() + "/delete")).andExpect(redirectedUrl("/rooms"));

        assertEquals(true, roomRepository.existsById(room.getId()));
    }
}
