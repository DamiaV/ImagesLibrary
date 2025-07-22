package net.darmo_creations.bildumilo.utils;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtilsTest {
  @Test
  void getCallingMethodName_success() {
    assertEquals("getCallingMethodName_success", this.dummy());
  }

  private String dummy() {
    return ReflectionUtils.getCallingMethodName();
  }

  @Test
  void getCallingMethodName_notCaller() {
    assertNotEquals("getCallingMethodName_notCaller", ReflectionUtils.getCallingMethodName());
  }
}