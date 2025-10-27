package com.mipt.team4.cloudstorage.modules.user.entity;

import java.util.UUID;

public class UserEntity {
  private final UUID id;
  private String name;
  private String email;
  private String password;
  private String phoneNumber;
  private String surname;
  private long freeSpace;

  public UserEntity(
      UUID id,
      String name,
      String surname,
      String email,
      String password,
      String phoneNumber,
      Long freeSpace) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.password = password;
    this.phoneNumber = phoneNumber;
    this.surname = surname;
    this.freeSpace = freeSpace;
  }

  // геттеры
  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  // сеттеры
  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public long getFreeSpace() {
    return freeSpace;
  }

  public void setFreeSpace(long freeSpace) {
    this.freeSpace = freeSpace;
  }
}
