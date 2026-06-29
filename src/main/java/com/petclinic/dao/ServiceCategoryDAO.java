package com.petclinic.dao;

import com.petclinic.model.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceCategoryDAO {

    private List<Service> mapList(ResultSet rs) throws SQLException {
        List<Service> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Service mapRow(ResultSet rs) throws SQLException {
        Service s = new Service();
        s.setServiceID(rs.getInt("ServiceID"));
        s.setName(rs.getString("Name"));
        s.setPrice(rs.getBigDecimal("Price"));
        s.setDurationMinutes(rs.getInt("DurationMinutes"));
        s.setActive(rs.getBoolean("IsActive"));
        try { s.setCategoryName(rs.getString("CategoryName")); } catch (SQLException ignored) {}
        return s;
    }
}
