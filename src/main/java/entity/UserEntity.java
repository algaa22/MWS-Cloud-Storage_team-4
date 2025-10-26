package entity;

import java.util.List;
import java.util.UUID;

public class UserEntity {
  private UUID id;
  private String name;
  private String email;
  private String password;
  private String phoneNumber;
  private String surname;

  public UserEntity(UUID id, String name, String surname, String email, String password, String phoneNumber) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.password = password;
    this.phoneNumber = phoneNumber;
    this.surname = surname;
  }


  // Геттеры и сеттеры (методы для чтения/записи полей)
  public UUID getId() { return id; }
  public String getName() { return name; }
  public String getEmail() { return email; }
  public String getSurname() { return surname; }
  public String getPassword() { return password; }
  public String getPhoneNumber() { return phoneNumber; }
  public void setId(UUID id) { this.id = id; }
  public void setName(String name) { this.name = name; }
  public void setEmail(String email) { this.email = email; }
  public void setPassword(String password) { this.password = password; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}

