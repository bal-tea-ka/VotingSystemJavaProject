package com.example.votingsystem;

import java.util.List;

public class Voting {
    private int id; // Добавляем ID для работы с базой
    private String title;
    private List<Candidate> candidates;

    public Voting(int id, String title, List<Candidate> candidates) {
        this.id = id;
        this.title = title;
        this.candidates = candidates;
    }

    public String getTitle() {
        return title;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
