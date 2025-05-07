package com.example.votingsystem;

public abstract class User {
    protected int id; // Добавляем ID для работы с базой
    protected String name;
    protected String login;
    protected String password;

    public User(int id, String name, String login, String password) {
        this.id = id;
        this.name = name;
        this.login = login;
        this.password = password;
    }

    public boolean enter(String login, String password) {
        return this.login.equals(login) && this.password.equals(password);
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
