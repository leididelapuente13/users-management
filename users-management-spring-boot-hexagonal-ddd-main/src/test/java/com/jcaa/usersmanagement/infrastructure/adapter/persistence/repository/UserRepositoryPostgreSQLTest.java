package com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.exception.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UserRepositoryPostgreSQL")
@ExtendWith(MockitoExtension.class)
class UserRepositoryPostgreSQLTest {

  private static final String ID = "u-001";
  private static final String NAME = "John Doe";
  private static final String EMAIL = "john@example.com";
  private static final String HASH = "$2a$12$abcdefghijklmnopqrstuO";
  private static final String ROLE = "ADMIN";
  private static final String STATUS = "ACTIVE";
  private static final String CREATED_AT = "2024-01-01 10:00:00";
  private static final String UPDATED_AT = "2024-01-02 10:00:00";

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private PreparedStatement statement;
  @Mock private ResultSet resultSet;

  private UserRepositoryPostgreSQL repository;
  private UserModel userModel;
  private UserId userId;
  private UserEmail userEmail;

  @BeforeEach
  void setUp() {
    repository = new UserRepositoryPostgreSQL(dataSource);
    userId = new UserId(ID);
    userEmail = new UserEmail(EMAIL);
    userModel =
        new UserModel(
            userId,
            new UserName(NAME),
            userEmail,
            UserPassword.fromHash(HASH),
            UserRole.ADMIN,
            UserStatus.ACTIVE);
  }

  private void configureStatementAndResultSet() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
  }

  private void configureResultSetRow() throws SQLException {
    when(resultSet.getString("id")).thenReturn(ID);
    when(resultSet.getString("name")).thenReturn(NAME);
    when(resultSet.getString("email")).thenReturn(EMAIL);
    when(resultSet.getString("password")).thenReturn(HASH);
    when(resultSet.getString("role")).thenReturn(ROLE);
    when(resultSet.getString("status")).thenReturn(STATUS);
    when(resultSet.getString("created_at")).thenReturn(CREATED_AT);
    when(resultSet.getString("updated_at")).thenReturn(UPDATED_AT);
  }

  @Test
  @DisplayName("save() ejecuta INSERT y retorna el usuario persistido")
  void shouldSaveUserAndReturnById() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    configureResultSetRow();

    // Act
    final UserModel result = repository.save(userModel);

    // Assert
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(ID, result.getId().value()),
        () -> assertEquals(NAME, result.getName().value()),
        () -> assertEquals(EMAIL, result.getEmail().value()),
        () -> assertEquals(UserRole.ADMIN, result.getRole()),
        () -> assertEquals(UserStatus.ACTIVE, result.getStatus()),
        () -> verify(statement).executeUpdate());
  }

  @Test
  @DisplayName("save() lanza PersistenceException cuando la consulta falla")
  void shouldThrowPersistenceExceptionWhenSaveFails() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenThrow(new SQLException("Duplicate key"));

    // Act & Assert
    assertThrows(PersistenceException.class, () -> repository.save(userModel));
  }

  @Test
  @DisplayName("save() lanza UserNotFoundException si el usuario no se encuentra tras el insert")
  void shouldThrowUserNotFoundExceptionWhenUserNotFoundAfterSave() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    // Act & Assert
    assertThrows(UserNotFoundException.class, () -> repository.save(userModel));
  }

  @Test
  @DisplayName("update() ejecuta UPDATE y retorna el usuario actualizado")
  void shouldUpdateUserAndReturnById() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    configureResultSetRow();

    // Act
    final UserModel result = repository.update(userModel);

    // Assert
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(ID, result.getId().value()),
        () -> assertEquals(NAME, result.getName().value()),
        () -> assertEquals(EMAIL, result.getEmail().value()),
        () -> assertEquals(UserRole.ADMIN, result.getRole()),
        () -> assertEquals(UserStatus.ACTIVE, result.getStatus()),
        () -> verify(statement).executeUpdate());
  }

  @Test
  @DisplayName("update() lanza PersistenceException cuando el update falla")
  void shouldThrowPersistenceExceptionWhenUpdateFails() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenThrow(new SQLException("Update error"));

    // Act & Assert
    assertThrows(PersistenceException.class, () -> repository.update(userModel));
  }

  @Test
  @DisplayName("getById() retorna Optional con el usuario si existe")
  void shouldReturnUserWhenFoundById() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(true);
    configureResultSetRow();

    // Act
    final Optional<UserModel> result = repository.getById(userId);

    // Assert
    assertAll(
        () -> assertTrue(result.isPresent()),
        () -> assertEquals(ID, result.get().getId().value()),
        () -> assertEquals(NAME, result.get().getName().value()),
        () -> assertEquals(EMAIL, result.get().getEmail().value()),
        () -> verify(statement).setString(1, ID));
  }

  @Test
  @DisplayName("getById() retorna Optional vacio si no existe")
  void shouldReturnEmptyOptionalWhenNotFoundById() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(false);

    // Act
    final Optional<UserModel> result = repository.getById(userId);

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("getById() lanza PersistenceException en error SQL")
  void shouldThrowPersistenceExceptionWhenGetByIdFails() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Connection error"));

    // Act & Assert
    assertThrows(PersistenceException.class, () -> repository.getById(userId));
  }

  @Test
  @DisplayName("getByEmail() retorna Optional con el usuario si existe")
  void shouldReturnUserWhenFoundByEmail() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(true);
    configureResultSetRow();

    // Act
    final Optional<UserModel> result = repository.getByEmail(userEmail);

    // Assert
    assertAll(
        () -> assertTrue(result.isPresent()),
        () -> assertEquals(EMAIL, result.get().getEmail().value()),
        () -> verify(statement).setString(1, EMAIL));
  }

  @Test
  @DisplayName("getByEmail() retorna Optional vacio si no existe")
  void shouldReturnEmptyOptionalWhenNotFoundByEmail() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(false);

    // Act
    final Optional<UserModel> result = repository.getByEmail(userEmail);

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("getByEmail() lanza PersistenceException en error SQL")
  void shouldThrowPersistenceExceptionWhenGetByEmailFails() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

    // Act & Assert
    assertThrows(PersistenceException.class, () -> repository.getByEmail(userEmail));
  }

  @Test
  @DisplayName("getAll() retorna lista con usuarios persistidos")
  void shouldReturnAllUsers() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(true, false);
    configureResultSetRow();

    // Act
    final List<UserModel> result = repository.getAll();

    // Assert
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(1, result.size()),
        () -> assertEquals(ID, result.get(0).getId().value()));
  }

  @Test
  @DisplayName("getAll() lanza PersistenceException en error SQL")
  void shouldThrowPersistenceExceptionWhenGetAllFails() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));

    // Act & Assert
    assertThrows(PersistenceException.class, () -> repository.getAll());
  }

  @Test
  @DisplayName("delete() ejecuta DELETE correctamente")
  void shouldDeleteUser() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);

    // Act
    repository.delete(userId);

    // Assert
    assertAll(
        () -> verify(statement).setString(1, ID),
        () -> verify(statement).executeUpdate());
  }

  @Test
  @DisplayName("delete() lanza PersistenceException en error SQL")
  void shouldThrowPersistenceExceptionWhenDeleteFails() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    doThrow(new SQLException("Delete error")).when(statement).executeUpdate();

    // Act & Assert
    assertThrows(PersistenceException.class, () -> repository.delete(userId));
  }
}
