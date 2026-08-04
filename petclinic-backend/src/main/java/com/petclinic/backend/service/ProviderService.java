package com.petclinic.backend.service;

import com.petclinic.backend.dao.ProviderDAO;
import com.petclinic.backend.model.Provider;
import java.sql.SQLException;
import java.util.List;

public class ProviderService {
    private final ProviderDAO dao = new ProviderDAO();
    public List<Provider> search(String query) throws SQLException { return dao.search(query); }
    public List<Provider> getActive() throws SQLException { return dao.findActive(); }
    public Provider findActiveByName(String name) throws SQLException { for (Provider p : dao.findActive()) if (p.getName().equalsIgnoreCase(name.trim())) return p; return null; }
    public Provider find(int id) throws SQLException { return dao.findById(id); }
    public void save(Provider provider) throws SQLException {
        if (provider.getName() == null || provider.getName().isBlank()) throw new IllegalArgumentException("Provider name is required.");
        dao.save(provider);
    }
    public void setActive(int id, boolean active) throws SQLException { dao.setActive(id, active); }
    public void delete(int id) throws SQLException { dao.delete(id); }
}
