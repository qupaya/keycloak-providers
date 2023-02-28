package com.qupaya.sha1;

public class Sha1User {
    String firstName = "";
    String lastName = "";
    String email = "";
    String username = "";
    String hash = "";
    String salt = "";
    boolean emailVerified = true;
    boolean enabled = true;

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
