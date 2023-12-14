package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@Getter
@Setter
@ToString
public class Room {
    private String RoomId;
    private String CurrentWord;
    private int CurrentPlayer;
    private String status;
    private List<String> players;
    private String joinStatus;
    private Map<String, Boolean> playerStatus;
    @JsonIgnore
    private Timer timer;
    public Room(){
        this.RoomId = UUID.randomUUID().toString();
        this.CurrentWord = "";
        this.CurrentPlayer = 0;
        this.status = "ready";
        this.players = new ArrayList<>();
        this.playerStatus = new HashMap<>();
    }

    public void addPlayer(String username) {
        this.players.add(username);
        this.playerStatus.put(username, false);
    }

    public void playerReady(String username) {
        this.playerStatus.put(username, true);
    }

    public boolean allReady() {
        return !this.playerStatus.containsValue(false); // Check if any player is not ready
    }
}
