package com.petclinic.backend.dao;

import com.petclinic.backend.model.Provider;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProviderDAO {
    public List<Provider> search(String q) throws SQLException {
        String sql = "SELECT * FROM Providers WHERE (? IS NULL OR Name LIKE ? OR ContactPerson LIKE ? OR Phone LIKE ? OR Email LIKE ?) ORDER BY IsActive DESC, Name";
        List<Provider> result = new ArrayList<>(); String like = q == null || q.isBlank() ? null : "%" + q.trim() + "%";
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            for(int i=1;i<=5;i++) ps.setString(i, like);
            try(ResultSet rs=ps.executeQuery()){ while(rs.next()) result.add(map(rs)); }
        } return result;
    }
    public List<Provider> findActive() throws SQLException {
        List<Provider> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM Providers WHERE IsActive=1 ORDER BY Name");
             ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(map(rs)); }
        return result;
    }
    public Provider findById(int id) throws SQLException {
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement("SELECT * FROM Providers WHERE ProviderID=?")) { ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;} }
    }
    public void save(Provider p) throws SQLException {
        boolean add=p.getProviderID()==0;
        String sql=add ? "INSERT INTO Providers(Name,ContactPerson,Phone,Email,Address,TaxCode,Note,IsActive,CreatedAt) VALUES(?,?,?,?,?,?,?,?,SYSUTCDATETIME())" : "UPDATE Providers SET Name=?,ContactPerson=?,Phone=?,Email=?,Address=?,TaxCode=?,Note=?,IsActive=? WHERE ProviderID=?";
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) { bind(ps,p); if(!add) ps.setInt(9,p.getProviderID()); ps.executeUpdate(); }
    }
    public void setActive(int id, boolean active) throws SQLException { try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement("UPDATE Providers SET IsActive=? WHERE ProviderID=?")){ps.setBoolean(1,active);ps.setInt(2,id);ps.executeUpdate();} }
    public void delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Providers WHERE ProviderID=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Provider not found.");
        }
    }
    private void bind(PreparedStatement ps, Provider p)throws SQLException{ps.setString(1,p.getName());ps.setString(2,p.getContactPerson());ps.setString(3,p.getPhone());ps.setString(4,p.getEmail());ps.setString(5,p.getAddress());ps.setString(6,p.getTaxCode());ps.setString(7,p.getNote());ps.setBoolean(8,p.isActive());}
    private Provider map(ResultSet r)throws SQLException{Provider p=new Provider();p.setProviderID(r.getInt("ProviderID"));p.setName(r.getString("Name"));p.setContactPerson(r.getString("ContactPerson"));p.setPhone(r.getString("Phone"));p.setEmail(r.getString("Email"));p.setAddress(r.getString("Address"));p.setTaxCode(r.getString("TaxCode"));p.setNote(r.getString("Note"));p.setActive(r.getBoolean("IsActive"));Timestamp t=r.getTimestamp("CreatedAt");if(t!=null)p.setCreatedAt(t.toLocalDateTime());return p;}
}
