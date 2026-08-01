package com.petclinic.backend.listener;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.CustomerDAO;
import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.service.EmailService;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

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


    private void checkOverdue() {
        try {
            List<Appointment> overdue = appointmentDAO.findOverdueActive();
            for (Appointment appt : overdue) {
                LocalDateTime endDt = LocalDateTime.of(appt.getAppointmentDate(), appt.getEndTime());
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
