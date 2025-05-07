package com.example.votingsystem;

import java.util.List;

public class Admin extends User {
    public Admin(int id, String name, String login, String password) {
        super(id, name, login, password);
    }

    public Voting createVoting(String title, List<Candidate> candidates) {
        return new Voting(-1, title, candidates); // ID будет установлен после сохранения в базу
    }
}
