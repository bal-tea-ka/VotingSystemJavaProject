package com.example.votingsystem;

public class Candidate {
    private int id; // Добавляем ID для работы с базой
    private String name;
    private int voices;

    public Candidate(int id, String name, int voices) {
        this.id = id;
        this.name = name;
        this.voices = voices;
    }

    public String getName() {
        return name;
    }

    public void addVoice() {
        voices++;
    }

    public int getVoices() {
        return voices;
    }

    public int getId() {
        return id;
    }
}
