package th.mfu;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import th.mfu.domain.Bill;
import th.mfu.domain.PaymentStatus;
import th.mfu.domain.Role;
import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;

// gemini key forced empty so tests never call the real API, even if GEMINI_API_KEY is set locally
@SpringBootTest(properties = "dormhub.gemini.api-key=")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DormHubApplicationTest {

    private static final String ADMIN_EMAIL = "sjdadmin@gmail.com";
    private static final String TENANT_EMAIL = "somchai@example.com"; // room 101, shared by 2

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    private static RequestPostProcessor admin() {
        return user(ADMIN_EMAIL).roles("ADMIN");
    }

    private static RequestPostProcessor tenant() {
        return user(TENANT_EMAIL).roles("TENANT");
    }

    // ---------- login / register ----------

    @Test
    public void anonymousUserIsSentToLogin() throws Exception {
        mvc.perform(get("/admin")).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/app")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    public void adminAccountLogsInToDashboard() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user(ADMIN_EMAIL).password("dormhub123"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(get("/").with(admin())).andExpect(redirectedUrl("/admin"));
    }

    @Test
    public void tenantLogsInToApp() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user(TENANT_EMAIL).password("tenant123"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(get("/").with(tenant())).andExpect(redirectedUrl("/app"));
    }

    @Test
    public void wrongPasswordIsRejected() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user(ADMIN_EMAIL).password("wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    public void adminEmailCannotBeRegistered() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Fake Admin").param("email", "SJDAdmin@gmail.com")
                .param("password", "123456").param("confirmPassword", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("สงวนไว้สำหรับผู้ดูแลหอพัก")));
        assertEquals(Role.ADMIN, userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).get().getRole());
    }

    @Test
    public void registeredAccountIsAlwaysTenant() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "นักศึกษา ใหม่").param("email", "New@Example.com")
                .param("password", "secret1").param("confirmPassword", "secret1"))
                .andExpect(redirectedUrl("/login?registered"));
        assertEquals(Role.TENANT, userRepository.findByEmailIgnoreCase("new@example.com").get().getRole());

        mvc.perform(formLogin("/login").userParameter("email").user("new@example.com").password("secret1"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    public void tenantCannotOpenAdminPages() throws Exception {
        mvc.perform(get("/admin").with(tenant())).andExpect(status().isForbidden());
        mvc.perform(get("/admin/bills").with(tenant())).andExpect(status().isForbidden());
    }

    // ---------- pages ----------

    @Test
    public void allAdminPagesLoad() throws Exception {
        String[] urls = { "/admin", "/admin/analytics", "/admin/layout", "/admin/rooms", "/admin/rooms/new", "/admin/rooms/1",
                "/admin/tenants", "/admin/tenants/new", "/admin/tenants/1", "/admin/bills", "/admin/bills/new", "/admin/bills/1",
                "/admin/payments", "/admin/repairs", "/admin/repairs/new", "/admin/repairs/1", "/admin/feedback" };
        for (String url : urls) {
            mvc.perform(get(url).with(admin())).andExpect(status().isOk());
        }
    }

    @Test
    public void allTenantPagesLoad() throws Exception {
        for (String url : new String[] { "/app", "/app/bills", "/app/chat", "/app/laundry", "/app/requests" }) {
            mvc.perform(get(url).with(tenant())).andExpect(status().isOk());
        }
    }

    @Test
    public void billPageShowsDormPromptPayQr() throws Exception {
        mvc.perform(get("/app/bills").with(tenant()))
                .andExpect(content().string(containsString("/img/promptpay-qr.png")))
                .andExpect(content().string(containsString("ญาณวุฒิ ทิศหนองแวง")))
                .andExpect(content().string(containsString("0628307781")));
    }

    @Test
    public void logoAndQrImagesArePublic() throws Exception {
        // the login page shows the logo before anyone is signed in
        mvc.perform(get("/img/logo.png")).andExpect(status().isOk());
        mvc.perform(get("/img/logo-icon.png")).andExpect(status().isOk());
    }

    @Test
    public void unlinkedAccountStillSeesTheApp() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "ยังไม่มี ห้อง").param("email", "noroom@example.com")
                .param("password", "secret1").param("confirmPassword", "secret1"));
        for (String url : new String[] { "/app", "/app/bills", "/app/requests" }) {
            mvc.perform(get(url).with(user("noroom@example.com").roles("TENANT")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("ยังไม่ผูกกับห้องพัก")));
        }
    }

    @Test
    public void unknownIdReturns404() throws Exception {
        mvc.perform(get("/admin/rooms/999").with(admin())).andExpect(status().isNotFound());
    }

    // ---------- room / tenant rules ----------

    @Test
    public void addingTenantMarksRoomOccupied() throws Exception {
        Room room = roomRepository.findByRoomNumber("103").get();
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());

        mvc.perform(post("/admin/tenants").with(admin()).with(csrf())
                .param("firstName", "ทดสอบ").param("lastName", "ระบบ").param("room", room.getId().toString()))
                .andExpect(redirectedUrl("/admin/tenants"));

        assertEquals(RoomStatus.OCCUPIED, roomRepository.findById(room.getId()).get().getStatus());
    }

    @Test
    public void roomCapacityIsEnforced() throws Exception {
        Room room = roomRepository.findByRoomNumber("103").get(); // capacity 1
        mvc.perform(post("/admin/tenants").with(admin()).with(csrf())
                .param("firstName", "คนที่").param("lastName", "หนึ่ง").param("room", room.getId().toString()))
                .andExpect(redirectedUrl("/admin/tenants"));
        mvc.perform(post("/admin/tenants").with(admin()).with(csrf())
                .param("firstName", "คนที่").param("lastName", "สอง").param("room", room.getId().toString()))
                .andExpect(redirectedUrl("/admin/tenants/new"));
    }

    @Test
    public void roomWithTenantsCannotBeDeleted() throws Exception {
        Room room = roomRepository.findByRoomNumber("101").get();
        mvc.perform(get("/admin/rooms/" + room.getId() + "/delete").with(admin())).andExpect(redirectedUrl("/admin/rooms"));
        assertTrue(roomRepository.existsById(room.getId()));
    }

    // ---------- payments ----------

    @Test
    public void slipWithoutAiWaitsForAdminThenClosesBill() throws Exception {
        Room room = roomRepository.findByRoomNumber("101").get();
        Bill bill = unpaidBill(room);
        MockMultipartFile slip = new MockMultipartFile("slip", "slip.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        mvc.perform(multipart("/app/bills/" + bill.getId() + "/pay").file(slip)
                .param("amount", String.valueOf(bill.getTotal())).with(tenant()).with(csrf()))
                .andExpect(redirectedUrl("/app/bills"));

        var payment = paymentRepository.findByBillOrderBySubmittedAtDesc(bill).get(0);
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertFalse(billRepository.findById(bill.getId()).get().isPaid());

        mvc.perform(post("/admin/payments/" + payment.getId() + "/approve").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/payments"));
        assertTrue(billRepository.findById(bill.getId()).get().isPaid());
    }

    @Test
    public void nonImageSlipIsFlagged() throws Exception {
        Bill bill = unpaidBill(roomRepository.findByRoomNumber("101").get());
        MockMultipartFile slip = new MockMultipartFile("slip", "slip.pdf", "application/pdf", new byte[] { 1 });
        mvc.perform(multipart("/app/bills/" + bill.getId() + "/pay").file(slip)
                .param("amount", "100").with(tenant()).with(csrf()));
        assertEquals(PaymentStatus.SUSPICIOUS, paymentRepository.findByBillOrderBySubmittedAtDesc(bill).get(0).getStatus());
    }

    @Test
    public void tenantCannotPayAnotherRoomsBill() throws Exception {
        Bill other = unpaidBill(roomRepository.findByRoomNumber("204").get());
        MockMultipartFile slip = new MockMultipartFile("slip", "slip.jpg", "image/jpeg", new byte[] { 1 });
        mvc.perform(multipart("/app/bills/" + other.getId() + "/pay").file(slip)
                .param("amount", "100").with(tenant()).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ---------- chatbot / laundry ----------

    @Test
    public void chatbotAnswersWifiWithoutGemini() throws Exception {
        mvc.perform(post("/app/chat/send").with(tenant()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"ขอรหัส wifi หน่อย\",\"history\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.reply", containsString("dormhub2026")));
    }

    @Test
    public void laundryMachineCanBeStartedOnce() throws Exception {
        mvc.perform(post("/app/laundry/1/start").with(tenant()).with(csrf())).andExpect(redirectedUrl("/app/laundry"));
        mvc.perform(get("/app/laundry").with(tenant())).andExpect(content().string(containsString("ห้อง 101")));
    }

    private Bill unpaidBill(Room room) {
        List<Bill> bills = billRepository.findByRoomOrderByBillMonthDesc(room);
        return bills.stream().filter(b -> !b.isPaid()).findFirst().orElseThrow();
    }
}
