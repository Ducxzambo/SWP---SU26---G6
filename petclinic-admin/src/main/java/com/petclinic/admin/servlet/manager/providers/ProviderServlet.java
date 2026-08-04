package com.petclinic.admin.servlet.manager.providers;

import com.petclinic.backend.model.Provider;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.ProviderService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin/providers")
public class ProviderServlet extends HttpServlet {
    private final ProviderService service = new ProviderService();
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp); if (manager == null) return;
        try { String q=req.getParameter("q"); req.setAttribute("providers",service.search(q)); req.setAttribute("q",q); String edit=req.getParameter("edit"); if(edit!=null) req.setAttribute("provider",service.find(Integer.parseInt(edit))); req.getRequestDispatcher("/WEB-INF/views/manager/providers/list.jsp").forward(req,resp); }
        catch(Exception e){ req.setAttribute("error",e.getMessage()); req.getRequestDispatcher("/WEB-INF/views/manager/providers/list.jsp").forward(req,resp); }
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8"); if(StaffAuthUtil.requireManager(req,resp)==null)return;
        try { String action=req.getParameter("action"); if("status".equals(action)) service.setActive(Integer.parseInt(req.getParameter("id")),Boolean.parseBoolean(req.getParameter("active"))); else if("delete".equals(action)) service.delete(Integer.parseInt(req.getParameter("id"))); else { Provider p=new Provider(); String id=req.getParameter("providerID"); if(id!=null&&!id.isBlank())p.setProviderID(Integer.parseInt(id)); p.setName(v(req,"name"));p.setContactPerson(v(req,"contactPerson"));p.setPhone(v(req,"phone"));p.setEmail(v(req,"email"));p.setAddress(v(req,"address"));p.setTaxCode(v(req,"taxCode"));p.setNote(v(req,"note"));p.setActive(req.getParameter("active")!=null);service.save(p); } req.getSession().setAttribute("flashSuccess","Provider updated."); }
        catch(Exception e){req.getSession().setAttribute("flashError","Operation failed: "+e.getMessage());}
        resp.sendRedirect(req.getContextPath()+"/admin/providers");
    }
    private String v(HttpServletRequest r,String key){String value=r.getParameter(key);return value==null?null:value.trim();}
}
