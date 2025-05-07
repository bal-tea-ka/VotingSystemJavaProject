package com.example.votingsystem;

public class Elector extends User {
    private boolean voted;

    public Elector(int id, String name, String login, String password, boolean voted) {
        super(id, name, login, password);
        this.voted = voted;
    }

    public boolean vote(Candidate candidate) {
        if (!voted) {
            candidate.addVoice();
            voted = true;
            return true;
        }
        return false;
    }

    public boolean isVoted() {
        return voted;
    }
}