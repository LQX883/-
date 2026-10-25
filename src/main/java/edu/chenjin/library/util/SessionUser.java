package edu.chenjin.library.util;

import java.io.Serializable;

/**
 * 登录后保存在 Session 中的用户信息（轻量、可序列化）。
 */
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String role;
    private String major;

    public SessionUser() {
    }

    public SessionUser(String id, String name, String role, String major) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.major = major;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }
}
