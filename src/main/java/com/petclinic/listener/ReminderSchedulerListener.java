package com.petclinic.listener;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.CustomerDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.Customer;
import com.petclinic.service.EmailService;
import com.petclinic.util.DBConnection;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * On server startup, re-schedules email reminders for all upcoming Confirmed
 * appointments whose reminder times haven't passed yet.
 *
 * This compensates for reminders lost when the server restarts.
 * For production with frequent restarts, use a DB-persisted job table (e.g. Quartz).
 */
@WebListener
public class ReminderSchedulerListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(ReminderSchedulerListener.class.getName());

    private final EmailService    emailSvc       = new EmailService();
    private final AppointmentDAO  appointmentDAO = new AppointmentDAO();
    private final CustomerDAO     customerDAO    = new CustomerDAO();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("ReminderSchedulerListener: scheduling pending reminders...");
        try {
            List<Appointment> upcoming = findUpcomingConfirmed();
            int count = 0;
            for (Appointment appt : upcoming) {
                try {
                    Customer customer = customerDAO.findById(appt.getCustomerID());
                    if (customer != null) {
                        emailSvc.scheduleReminders(customer, appt);
                        count++;
                    }
                } catch (Exception e) {
                    LOG.warning("Failed to schedule reminder for appt #" + appt.getAppointmentID()
                            + ": " + e.getMessage());
                }
            }
            LOG.info("ReminderSchedulerListener: scheduled reminders for " + count + " appointments.");
        } catch (Exception e) {
            LOG.warning("ReminderSchedulerListener startup failed: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // EmailService uses daemon threads — JVM will stop them on shutdown
    }

    /**
     * Load all Confirmed appointments from today onward.
     * Only includes those where at least one reminder (48h or 18h) is still in the future.
     */
    private List<Appointment> findUpcomingConfirmed() throws Exception {
        String sql = "SELECT a.*, p.Name AS PetName, s.Name AS ServiceName, "
                   + "sc.Name AS CategoryName, st.FullName AS VetName "
                   + "FROM Appointments a "
                   + "JOIN Pets p ON a.PetID = p.PetID "
                   + "JOIN Services s ON a.ServiceID = s.ServiceID "
                   + "JOIN ServiceCategories sc ON s.CategoryID = sc.CategoryID "
                   + "LEFT JOIN Staff st ON a.AssignedVetID = st.StaffID "
                   + "WHERE a.Status = 'Confirmed' AND a.AppointmentDate >= ?";

        List<Appointment> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Appointment a = mapRow(rs);
                    if (a.getAppointmentDate() == null || a.getStartTime() == null) continue;
                    // Only schedule if 18h reminder hasn't passed yet
                    LocalDateTime apptDt = LocalDateTime.of(a.getAppointmentDate(), a.getStartTime());
                    if (apptDt.minusHours(18).isAfter(now)) {
                        list.add(a);
                    }
                }
            }
        }
        return list;
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setPetID(rs.getInt("PetID"));
        a.setServiceID(rs.getInt("ServiceID"));
        a.setAppointmentDate(rs.getDate("AppointmentDate").toLocalDate());
        a.setStartTime(rs.getTime("StartTime").toLocalTime());
        a.setEndTime(rs.getTime("EndTime").toLocalTime());
        a.setStatus(rs.getString("Status"));
        try { a.setPetName(rs.getString("PetName")); }     catch (Exception ignored) {}
        try { a.setServiceName(rs.getString("ServiceName")); } catch (Exception ignored) {}
        try { a.setVetName(rs.getString("VetName")); }     catch (Exception ignored) {}
        return a;
    }
}
