package de.htwberlin.dbtech.aufgaben.ue02;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.htwberlin.dbtech.exceptions.CoolingSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.htwberlin.dbtech.exceptions.DataException;

public class CoolingJdbc implements ICoolingJdbc {

  private static final Logger L = LoggerFactory.getLogger(CoolingJdbc.class);
  private Connection connection;

  @Override
  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  @SuppressWarnings("unused")
  private Connection useConnection() {
    if (connection == null) {
      throw new DataException("Connection not set");
    }
    return connection;
  }

  @Override
  public List<String> getSampleKinds() {
    PreparedStatement pStmt = null;
    ResultSet rs = null;
    List<String> sampleKind = null;
    try {
      String sql = "Select text from Samplekind order by text asc";
      sampleKind = new LinkedList<String>();
      pStmt = useConnection().prepareStatement(sql);
      rs = pStmt.executeQuery();
      while (rs.next()) {
        sampleKind.add(rs.getString("text"));
      }
    } catch (SQLException e) {
      throw new DataException(e);
    }
    return sampleKind;
  }

  @Override
  public Sample findSampleById(Integer sampleId) {
    String sql = "SELECT sampleid, samplekindid, expirationdate FROM Sample WHERE sampleid = ?";
    Sample sample = null;
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, sampleId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          int sampleid = rs.getInt("sampleid");
          int samplekindid = rs.getInt("samplekindid");
          LocalDate expirationdate = rs.getDate("expirationdate").toLocalDate();
          sample = new Sample(sampleId, samplekindid, expirationdate);
        } else {
          throw new CoolingSystemException("No sample found with ID: " + sampleId);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return sample;
  }

  /*
  @Override
  public Sample findSampleById(Integer sampleId) {
    PreparedStatement pStmt;
    ResultSet rs;
    Sample sample = new Sample();
    try {
      String sql = "Select sampleid, samplekindid, expirationdate from Sample where sampleid = ?";
      pStmt = useConnection().prepareStatement(sql);
      pStmt.setInt(1, sampleId);
      rs = pStmt.executeQuery();
      while (rs.next()) {
        sample.setSampleId(rs.getInt("SAMPLEID"));
        sample.setSampleKindId(rs.getInt("SAMPLEKINDID"));
        sample.setExpirationDate(rs.getObject("expirationdate", LocalDate.class));
      }
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
      return sample;
  }
*/

  @Override
  public void createSample(Integer sampleId, Integer sampleKindId) {
    L.info("createSample: sampleId: " + sampleId + ", sampleKindId: " + sampleKindId);
    String sql = "INSERT INTO Sample (sampleid, samplekindid, expirationdate) VALUES (?, ?, ?)";
    try (PreparedStatement pStmt = connection.prepareStatement(sql)) {
      pStmt.setInt(1, sampleId);
      pStmt.setInt(2, sampleKindId);
      if (sampleKindId == 1) {
        pStmt.setDate(3, Date.valueOf(LocalDate.now().plusDays(4)));
      }
      if (sampleKindId == 2) {
        pStmt.setDate(3, Date.valueOf(LocalDate.now().plusDays(5)));
      }
      if (sampleKindId == 3) {
        pStmt.setDate(3, Date.valueOf(LocalDate.now().plusDays(6)));
      }
      pStmt.executeUpdate();
    } catch (SQLException e) {
      throw new CoolingSystemException(e);
    }
  }


  @Override
  public void clearTray(Integer trayId) {
    L.info("clearTray: trayId: " + trayId);

    String checkSql = "SELECT trayid FROM Tray WHERE trayid = ?";
    String deletetray = "delete from Tray where trayid = ?";
    String collectSampleData = String.join("select * from Sample" +
            "join place on sample.sampleid = place.sampleid" +
            "join tray on tray.trayid = place.trayid where tray.trayid =?" +
            "order by sample.sampleid asc");
    String clearSamples = "delete from Sample where sampleid in ?";

    List<Integer> sampleData = null;

    try (PreparedStatement checkStmt = connection.prepareStatement(checkSql);
         PreparedStatement deleteStmt = connection.prepareStatement(deletetray)) {

      checkStmt.setInt(1, trayId);
      try (ResultSet rs = checkStmt.executeQuery()) {
        if (!rs.next()) {
          throw new SQLException("Das angegebene Tray (ID: " + trayId + ") existiert nicht.");
        }
      } catch (SQLException e) {
        throw new CoolingSystemException(e);
      }
      try (PreparedStatement collectStmt = connection.prepareStatement(collectSampleData);) {
        try (ResultSet rs = collectStmt.executeQuery()) {
          sampleData = new ArrayList<Integer>();
          while (rs.next()) {
            sampleData.add(rs.getInt("sampleid"));
          }
          for (Integer sampleId : sampleData) {
            try (PreparedStatement clear = connection.prepareStatement(clearSamples)) {
              deleteStmt.setInt(1, sampleId);
              deleteStmt.executeUpdate();
            }
          }
        }
        deleteStmt.setInt(1, trayId);
        deleteStmt.executeUpdate();
      } catch (SQLException e) {
        throw new CoolingSystemException(e);
      }
      try (PreparedStatement pStmt = connection.prepareStatement(deletetray)) {
        pStmt.setInt(1, trayId);
        pStmt.executeUpdate();

      } catch (SQLException e) {
        throw new CoolingSystemException(e);
      }
    } catch (SQLException e) {
      throw new CoolingSystemException(e);
    }
  }
}
