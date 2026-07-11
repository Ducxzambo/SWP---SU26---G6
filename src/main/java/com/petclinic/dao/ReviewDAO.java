package com.petclinic.dao;

import com.petclinic.model.Review;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    // -- Write --------------------------------------------------------------

    /** Insert review. Return reviewID */
    public int insert(Review r) throws SQLException {
        String sql = "INSERT INTO Reviews "
                + "(AppointmentID, CustomerID, Rating, Comment, IsPublic, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, GETDATE())";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getAppointmentID());
            ps.setInt(2, r.getCustomerID());
            ps.setInt(3, r.getRating());
            ps.setString(4, r.getComment());
            ps.setBoolean(5, r.getIsPublic());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                return k.next() ? k.getInt(1) : -1;
            }
        }
    }

    public boolean update(Review r) throws SQLException {
        String sql = "UPDATE Reviews SET Rating = ?, Comment = ?, IsPublic = ? "
                + "WHERE AppointmentID = ? AND CustomerID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getRating());
            ps.setString(2, r.getComment());
            ps.setBoolean(3, r.getIsPublic());
            ps.setInt(4, r.getAppointmentID());
            ps.setInt(5, r.getCustomerID());

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    // Read

    /** Tìm review cho 1 appointment */
    public Review findByAppointment(int appointmentId) throws SQLException {
        String sql = buildBaseQuery()
                + " WHERE r.AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Review của 1 customers (nếu có My Review) */
    public List<Review> findByCustomer(int customerId) throws SQLException {
        String sql = buildBaseQuery()
                + " WHERE r.CustomerID = ? ORDER BY r.CreatedAt DESC";
        return queryList(sql, customerId);
    }

    /**
     * Reviews for the community page
     *
     * @param categoryId  0 = all
     * @param serviceId   0 = all
     * @param staffId       0 = all
     * @param petSpecies  null/blank = all
     * @param minRating   1-5, 0 = all
     * @param sortBy      "newest" | "rating_desc" | "rating_asc"
     */
    public List<Review> findPublic(int categoryId, int serviceId, int staffId,
                                   String petSpecies, int minRating, String sortBy)
            throws SQLException {

        StringBuilder sql = new StringBuilder(buildBaseQuery())
                .append(" WHERE r.IsPublic = 1 ");

        if (categoryId > 0)
            sql.append("AND EXISTS (SELECT 1 FROM AppointmentServices asvcf ")
                    .append("JOIN Services sf ON asvcf.ServiceID = sf.ServiceID ")
                    .append("WHERE asvcf.AppointmentID = a.AppointmentID AND sf.CategoryID = ")
                    .append(categoryId).append(") ");
        if (serviceId > 0)
            sql.append("AND EXISTS (SELECT 1 FROM AppointmentServices asvcf2 ")
                    .append("WHERE asvcf2.AppointmentID = a.AppointmentID AND asvcf2.ServiceID = ")
                    .append(serviceId).append(") ");
        if (staffId > 0)
            sql.append("AND EXISTS (SELECT 1 FROM AppointmentServices asvcf3 ")
                    .append("WHERE asvcf3.AppointmentID = a.AppointmentID AND asvcf3.AssignedStaffID = ")
                    .append(staffId).append(") ");
        if (petSpecies != null && !petSpecies.isBlank())
            sql.append("AND p.SpeciesName = '")
                    .append(petSpecies.replace("'","''")).append("' ");
        if (minRating > 0)
            sql.append("AND r.Rating >= ").append(minRating).append(" ");

        switch (sortBy == null ? "newest" : sortBy) {
            case "rating_desc": sql.append("ORDER BY r.Rating DESC, r.CreatedAt DESC"); break;
            case "rating_asc":  sql.append("ORDER BY r.Rating ASC,  r.CreatedAt DESC"); break;
            default:            sql.append("ORDER BY r.CreatedAt DESC");
        }

        return queryList(sql.toString(), null);
    }

    /** Rating trung bình cho 1 staff */
    public double avgRatingByStaff(int staffId) throws SQLException {
        String sql = "SELECT AVG(CAST(r.Rating AS FLOAT)) "
                + "FROM Reviews r "
                + "JOIN Appointments a ON r.AppointmentID = a.AppointmentID "
                + "WHERE EXISTS (SELECT 1 FROM AppointmentServices asvc "
                + "  WHERE asvc.AppointmentID = a.AppointmentID AND asvc.AssignedStaffID = ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    /** Breakdown of ratings cho 1 staff */
    public List<int[]> ratingBreakdownByStaff(int staffId) throws SQLException {
        String sql = "SELECT r.Rating, COUNT(*) AS Cnt "
                + "FROM Reviews r "
                + "JOIN Appointments a ON r.AppointmentID = a.AppointmentID "
                + "WHERE EXISTS (SELECT 1 FROM AppointmentServices asvc "
                + "  WHERE asvc.AppointmentID = a.AppointmentID AND asvc.AssignedStaffID = ?) "
                + "GROUP BY r.Rating ORDER BY r.Rating DESC";
        List<int[]> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(new int[]{ rs.getInt("Rating"), rs.getInt("Cnt") });
            }
        }
        return result;
    }

    /** Pet species đã có review (dùng ở filter) */
    public List<String> findPublicSpecies() throws SQLException {
        String sql = "SELECT DISTINCT p.SpeciesName "
                + "FROM Reviews r "
                + "JOIN Appointments a ON r.AppointmentID = a.AppointmentID "
                + "JOIN Pets p ON a.PetID = p.PetID "
                + "WHERE r.IsPublic = 1 "
                + "ORDER BY p.SpeciesName";
        List<String> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    // -- Helpers ----------------------------------------------------------------

    private String buildBaseQuery() {
        return "SELECT r.*, "
                + "  svcAgg.ServiceNames  AS ServiceName, "
                + "  catAgg.CategoryNames AS CategoryName, "
                + "  catAgg.FirstCategoryID AS CategoryID, "
                + "  staffPick.StaffName  AS StaffName, "
                + "  staffPick.StaffID    AS StaffID, "
                + "  p.SpeciesName AS PetSpecies, "
                + "  a.AppointmentDate "
                + "FROM Reviews r "
                + "JOIN Appointments a ON r.AppointmentID = a.AppointmentID "
                + "JOIN Pets p         ON a.PetID         = p.PetID "
                + "OUTER APPLY ( "
                + "    SELECT STRING_AGG(s.Name, ', ') AS ServiceNames "
                + "    FROM AppointmentServices asvc "
                + "    JOIN Services s ON asvc.ServiceID = s.ServiceID "
                + "    WHERE asvc.AppointmentID = a.AppointmentID "
                + ") svcAgg "
                + "OUTER APPLY ( "
                + "    SELECT STRING_AGG(y.CatName, ', ') AS CategoryNames, MIN(y.CategoryID) AS FirstCategoryID "
                + "    FROM ( "
                + "        SELECT DISTINCT sc.CategoryID, sc.Name AS CatName "
                + "        FROM AppointmentServices asvc2 "
                + "        JOIN Services s2 ON asvc2.ServiceID = s2.ServiceID "
                + "        JOIN ServiceCategories sc ON s2.CategoryID = sc.CategoryID "
                + "        WHERE asvc2.AppointmentID = a.AppointmentID "
                + "    ) y "
                + ") catAgg "
                + "OUTER APPLY ( "
                + "    SELECT TOP 1 st.StaffID, st.FullName AS StaffName "
                + "    FROM AppointmentServices asvc3 "
                + "    JOIN Staff st ON asvc3.AssignedStaffID = st.StaffID "
                + "    WHERE asvc3.AppointmentID = a.AppointmentID AND asvc3.AssignedStaffID IS NOT NULL "
                + "    ORDER BY asvc3.AppointmentServiceID "
                + ") staffPick ";
    }

    private List<Review> queryList(String sql, Integer param) throws SQLException {
        List<Review> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setReviewID(rs.getInt("ReviewID"));
        r.setAppointmentID(rs.getInt("AppointmentID"));
        r.setCustomerID(rs.getInt("CustomerID"));
        r.setRating(rs.getInt("Rating"));
        r.setComment(rs.getString("Comment"));
        r.setIsPublic(rs.getBoolean("IsPublic"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        r.setServiceName(rs.getString("ServiceName"));
        r.setCategoryName(rs.getString("CategoryName"));
        r.setStaffName(rs.getString("StaffName"));
        int vid = rs.getInt("StaffID"); if (!rs.wasNull()) r.setStaffID(vid);
        r.setPetSpecies(rs.getString("PetSpecies"));
        // Anonymous label: "Khach hang an danh" by default
        r.setAnonymousLabel("Khách hàng ẩn danh");
        return r;
    }
}
