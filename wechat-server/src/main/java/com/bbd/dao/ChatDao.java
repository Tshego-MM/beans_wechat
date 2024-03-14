// <<<<<<< HEAD
// package com.bbd.dao;

// import java.sql.ResultSet;
// import java.sql.SQLException;
// import java.util.Collection;
// import java.util.List;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.jdbc.core.RowMapper;
// import org.springframework.stereotype.Repository;

// import com.bbd.model.Chat;

// @Repository
// public class ChatDao {

//   @Autowired
//   private JdbcTemplate jdbcTemplate;

//   final String dbQuery = "USE beanwechat;";

//   private static class ChatRowMapper implements RowMapper<Chat> {

//     @Override
//     public Chat mapRow(ResultSet resultSet, int i) throws SQLException {
//       Chat chat = new Chat();

//       chat.setChatID(resultSet.getInt("ChatId"));
//       chat.setSender(resultSet.getString("Sender"));
//       chat.setReceiver(resultSet.getString("Receiver"));

//       return chat;
//     }
//   }

//   public Collection<Chat> getAllChats() {
//     final String sql = "SELECT * FROM Chat";
//     List<Chat> chats = jdbcTemplate.query(dbQuery + sql, new ChatRowMapper());

//     return chats;
//   }

//   public Chat getChatById(int ChatId) {
//     final String sql = "SELECT ChatId, Sender, Receiver FROM Chat WHERE ChatId = ?";
//     Chat chat = jdbcTemplate.queryForObject(dbQuery + sql, new ChatRowMapper(), ChatId);

//     return chat;
//   }

//   public void insertChatToDb(Chat chat) {
//     final String sql = "EXECUTE InsertIntoChat @Sender= ?, @Receiver= ?";
//     final String senderId = chat.getSender();
//     final String receiverId = chat.getReceiver();
//     System.out.println("DEBUG: "+senderId+" && "+receiverId);
//     try {
//       jdbcTemplate.update(dbQuery + sql, new Object[] { senderId, receiverId });
//     } catch (Exception e) {
//       System.err.println("DEBUG: Failed to insert chat " + e.toString());
//     }
//   }

//   public void deleteChatById(int ChatId) {
//     final String sql = "DELETE FROM Chat WHERE ChatId = ?";
//     jdbcTemplate.update(dbQuery + sql, ChatId);
//   }

//   public void updateChat(Chat Chat) {
//   }

// }
package com.bbd.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.bbd.model.Chat;
import com.bbd.model.CustomObject;
import com.google.gson.Gson;

@Repository
public class ChatDao {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  final String dbQuery = "USE beanwechat;";

  private static class ChatRowMapper implements RowMapper<Chat> {

    @Override
    public Chat mapRow(ResultSet resultSet, int i) throws SQLException {
      Chat chat = new Chat();

      chat.setChatID(resultSet.getInt("ChatId"));
      chat.setSender(resultSet.getString("Sender"));
      chat.setReceiver(resultSet.getString("Receiver"));
      chat.setCreatedAt(resultSet.getTimestamp("CreatedAt"));

      return chat;
    }
  }

  private static class CustomObjectRowMapper implements RowMapper<CustomObject> {

    @Override
    public CustomObject mapRow(ResultSet resultSet, int i) throws SQLException {
      CustomObject customObject = new CustomObject();

      customObject.setSenderUserName(resultSet.getString("SenderName"));
      customObject.setReceiverUserName(resultSet.getString("ReceiverName"));
      customObject.setContent(resultSet.getString("Content"));
      customObject.setCreatedAt(resultSet.getTimestamp("CreatedAt"));

      return customObject;
    }
  }

  public String getAllChats() {

    List<Chat> chats = null;
    try {
      final String sql = "SELECT ChatId, Sender, Receiver, CreatedAt FROM Chat";
      chats = jdbcTemplate.query(dbQuery + sql, new ChatRowMapper());
    } catch (EmptyResultDataAccessException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()).toString();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(e.getMessage()).toString();
    }

    String json = new Gson().toJson(chats);

    return json;
  }

  public String getChatById(int ChatId) {
    Chat chat;
    try {
      final String sql = "SELECT ChatId, Sender, Receiver, CreatedAt FROM Chat WHERE ChatId = ?";
      chat = jdbcTemplate.queryForObject(dbQuery + sql, new ChatRowMapper(), ChatId);
    } catch (EmptyResultDataAccessException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()).toString();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(e.getMessage()).toString();
    }

    String json = new Gson().toJson(chat);

    return json;
  }

  public String getUserChat(String senderUserName, String receiverUserName) {
    final String sql = "EXECUTE GetUserChat ?, ?";

    List<CustomObject> chatData;
    try {
      chatData = jdbcTemplate.query(dbQuery + sql, new CustomObjectRowMapper(),
          new Object[] { senderUserName, receiverUserName });

    } catch (EmptyResultDataAccessException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()).toString();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(e.getMessage()).toString();
    }

    if (chatData.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found").toString();
    }

    String json = new Gson().toJson(chatData);

    return json;
  }

  public String insertChatToDb(Chat chat) {
    final String sql = "EXECUTE InsertIntoChat @Sender= ?, @Receiver= ?";
    final String sender = chat.getSender();
    final String receiver = chat.getReceiver();

    try {
      jdbcTemplate.update(dbQuery + sql, new Object[] { sender, receiver });
    } catch (EmptyResultDataAccessException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("data not found").toString();
    } catch (Exception e) {
      boolean isFkConstraintError = e.getMessage()
          .contains("The INSERT statement conflicted with the CHECK constraint");

      if (isFkConstraintError) {
        return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body("Failed to INSERT: UserName not found")
            .toString();
      }

      return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(e.getMessage()).toString();
    }

    return ResponseEntity.status(HttpStatus.CREATED).body("Record Inserted successfully").toString();
  }

  public String deleteChatById(int ChatId) {
    final String sql = "DELETE FROM Chat WHERE ChatId = ?";

    int res;
    try {
      res = jdbcTemplate.update(dbQuery + sql, ChatId);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(e.getMessage()).toString();
    }

    if (res == 0) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("failed to delete record").toString();
    }

    return ResponseEntity.status(HttpStatus.OK).body("Record deleted successfully").toString();
  }

}
