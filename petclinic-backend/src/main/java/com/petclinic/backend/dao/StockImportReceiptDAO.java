package com.petclinic.backend.dao;

import com.petclinic.backend.model.StockImportReceipt;
import com.petclinic.backend.model.StockImportReceiptDetail;
import com.petclinic.backend.util.DBConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockImportReceiptDAO {
    public int create(int providerID, int staffID, String code) throws SQLException {
        String sql = "INSERT INTO StockImportReceipts (ReceiptCode,ProviderID,PerformedByID,ImportedAt,TotalAmount) VALUES (?,?,?,SYSDATETIME(),0)";
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,code); ps.setInt(2,providerID); ps.setInt(3,staffID); ps.executeUpdate();
            try(ResultSet keys=ps.getGeneratedKeys()){if(keys.next())return keys.getInt(1);}
        }
        throw new SQLException("Cannot create stock receipt.");
    }

    public void addDetail(int receiptID,String type,int itemID,String name,int quantity,BigDecimal price) throws SQLException {
        String sql="INSERT INTO StockImportReceiptDetails (ReceiptID,ItemType,ItemID,ItemName,Quantity,UnitPrice,LineTotal) VALUES (?,?,?,?,?,?,?)";
        try(Connection c=DBConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            BigDecimal safePrice=price==null?BigDecimal.ZERO:price;
            ps.setInt(1,receiptID);ps.setString(2,type);ps.setInt(3,itemID);ps.setString(4,name);ps.setInt(5,quantity);ps.setBigDecimal(6,safePrice);ps.setBigDecimal(7,safePrice.multiply(BigDecimal.valueOf(quantity)));ps.executeUpdate();
        }
    }

    public void linkLatestTransaction(int receiptID,String itemType,int itemID,int staffID) throws SQLException {
        String sql="UPDATE StockTransactions SET ReceiptID=? WHERE TransactionID=(SELECT TOP 1 TransactionID FROM StockTransactions WHERE ReceiptID IS NULL AND ItemType=? AND ItemID=? AND PerformedByID=? AND TransactionType='Import' ORDER BY TransactionID DESC)";
        try(Connection c=DBConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,receiptID);ps.setString(2,itemType);ps.setInt(3,itemID);ps.setInt(4,staffID);ps.executeUpdate();}
    }

    public void updateTotal(int receiptID) throws SQLException {
        String sql="UPDATE StockImportReceipts SET TotalAmount=(SELECT COALESCE(SUM(LineTotal),0) FROM StockImportReceiptDetails WHERE ReceiptID=?) WHERE ReceiptID=?";
        try(Connection c=DBConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,receiptID);ps.setInt(2,receiptID);ps.executeUpdate();}
    }

    public StockImportReceipt findById(int receiptID) throws SQLException {
        String sql="SELECT r.*,p.Name ProviderName,s.FullName PerformedByName FROM StockImportReceipts r JOIN Providers p ON p.ProviderID=r.ProviderID JOIN Staff s ON s.StaffID=r.PerformedByID WHERE r.ReceiptID=?";
        try(Connection c=DBConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,receiptID);try(ResultSet rs=ps.executeQuery()){if(!rs.next())return null;StockImportReceipt receipt=mapReceipt(rs);receipt.setDetails(findDetails(c,receiptID));return receipt;}}
    }

    public List<StockImportReceipt> findRecent() throws SQLException {
        String sql="SELECT TOP 100 r.*,p.Name ProviderName,s.FullName PerformedByName FROM StockImportReceipts r JOIN Providers p ON p.ProviderID=r.ProviderID JOIN Staff s ON s.StaffID=r.PerformedByID ORDER BY r.ImportedAt DESC";
        try(Connection c=DBConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql);ResultSet rs=ps.executeQuery()){List<StockImportReceipt> list=new ArrayList<>();while(rs.next())list.add(mapReceipt(rs));return list;}
    }

    private List<StockImportReceiptDetail> findDetails(Connection c,int receiptID)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("SELECT * FROM StockImportReceiptDetails WHERE ReceiptID=? ORDER BY ReceiptDetailID")){ps.setInt(1,receiptID);try(ResultSet rs=ps.executeQuery()){List<StockImportReceiptDetail> list=new ArrayList<>();while(rs.next()){StockImportReceiptDetail d=new StockImportReceiptDetail();d.setReceiptDetailID(rs.getInt("ReceiptDetailID"));d.setReceiptID(receiptID);d.setItemType(rs.getString("ItemType"));d.setItemID(rs.getInt("ItemID"));d.setItemName(rs.getString("ItemName"));d.setQuantity(rs.getInt("Quantity"));d.setUnitPrice(rs.getBigDecimal("UnitPrice"));d.setLineTotal(rs.getBigDecimal("LineTotal"));list.add(d);}return list;}}
    }

    private StockImportReceipt mapReceipt(ResultSet rs)throws SQLException{StockImportReceipt r=new StockImportReceipt();r.setReceiptID(rs.getInt("ReceiptID"));r.setReceiptCode(rs.getString("ReceiptCode"));r.setProviderID(rs.getInt("ProviderID"));r.setProviderName(rs.getString("ProviderName"));r.setPerformedByID(rs.getInt("PerformedByID"));r.setPerformedByName(rs.getString("PerformedByName"));Timestamp t=rs.getTimestamp("ImportedAt");if(t!=null)r.setImportedAt(t.toLocalDateTime());r.setTotalAmount(rs.getBigDecimal("TotalAmount"));return r;}
}
