package com.petclinic.listener;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.CustomerDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.Customer;
import com.petclinic.service.EmailService;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Background scheduler that runs every 15 minutes:
 *
 *  1. Find Pending/Confirmed appointments whose EndTime has passed TODAY:
 *     → Send "missed appointment" email notification to the customer.
 *
 *  2. Of those, find ones that passed more than 24 hours ago:
 *     → Change status to 'NoShow' (Absent).
 *
 * Note: Tomcat may restart → schedule is in-memory only.
 * For production, use a DB-persisted job queue (e.g. Quartz).
 */
@WebListener
public class AppointmentStatusJob implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppointmentStatusJob.class.getName());

    private ScheduledExecutorService scheduler;

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final CustomerDAO    customerDAO    = new CustomerDAO();
    private final EmailService   emailService   = new EmailService();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "appt-status-job");
            t.setDaemon(true);
            return t;
        });

        // Run once 1 minute after startup, then every 12 hours
        scheduler.scheduleAtFixedRate(this::runCheck, 1, 12*60, TimeUnit.MINUTES);
        LOG.info("[AppointmentStatusJob] Scheduler started.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) scheduler.shutdownNow();
        LOG.info("[AppointmentStatusJob] Scheduler stopped.");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void runCheck() {
        try {
            checkOverdue();
            checkAbsent();
        } catch (Exception e) {
            LOG.warning("[AppointmentStatusJob] Error: " + e.getMessage());
        }
    }

    /**
     * Step 1: appointments that just passed their end time (overdue, not yet absent).
     * Send email notification. Only notify once — check that status is still Pending/Confirmed
     * and endTime has passed but is within the last 24h.
     */
    private void checkOverdue() {
        try {
            List<Appointment> overdue = appointmentDAO.findOverdueActive();
            for (Appointment appt : overdue) {
                LocalDateTime endDt = LocalDateTime.of(appt.getAppointmentDate(), appt.getEndTime());
                // Only notify if missed within last 24h (so we don't spam repeat emails)
                if (endDt.isAfter(LocalDateTime.now().minusHours(24))) {
                    try {
                        Customer c = customerDAO.findById(appt.getCustomerID());
                        if (c != null) {
                            emailService.sendOverdueNotification(c, appt);
                        }
                    } catch (Exception e) {
                        LOG.warning("[AppointmentStatusJob] Failed to notify appt #"
                                + appt.getAppointmentID() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOG.warning("[AppointmentStatusJob] checkOverdue failed: " + e.getMessage());
        }
    }

    /**
     * Step 2: appointments that passed end time by more than 24 hours.
     * Change status to 'NoShow'.
     */
    private void checkAbsent() {
        try {
            List<Appointment> absent = appointmentDAO.findOverdueOlderThan24h();
            for (Appointment appt : absent) {
                appointmentDAO.markNoShow(appt.getAppointmentID());
                LOG.info("[AppointmentStatusJob] Marked NoShow: appt #" + appt.getAppointmentID());
            }
        } catch (Exception e) {
            LOG.warning("[AppointmentStatusJob] checkAbsent failed: " + e.getMessage());
        }
    }
}