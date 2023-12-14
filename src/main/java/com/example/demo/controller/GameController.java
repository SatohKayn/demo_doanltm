package com.example.demo.controller;

import com.example.demo.model.Message;
import com.example.demo.model.Room;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

@Controller
public class GameController {
    @Autowired
    private SimpMessagingTemplate template;

    private Map<String, Room> rooms = new HashMap<>();

    @MessageMapping("/game.createRoom")
    @SendTo("/room/roomCreated")
    public Room createRoom(@Payload Message chatMessage,
                           SimpMessageHeaderAccessor headerAccessor) {
        Room room = new Room();
        room.addPlayer(chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("roomID", room.getRoomId());
        room.setJoinStatus("succes");
        rooms.put(room.getRoomId(), room);
        return room;
    }

    @MessageMapping("/game.ready/{room}")
    public Room playerReady(@DestinationVariable String room, @Payload Message chatMessage) {
        Room roomInstance = rooms.get(room);
        if(chatMessage.getContent().equals("ready")){
                roomInstance.playerReady(chatMessage.getSender());
        }
        if (roomInstance.allReady()) {
            startGame(roomInstance);
        }
        this.template.convertAndSend("/room/getPlayerReady/" + roomInstance.getRoomId(), roomInstance);
        return roomInstance;
    }

    @MessageMapping("/game.joinRoom/{room}")
    public Room joinRoom(@DestinationVariable String room, @Payload Message chatMessage,
                         SimpMessageHeaderAccessor headerAccessor) {
        Room roomInstance = rooms.get(room);
        String response;
        if (roomInstance == null) {
            response = "Room not exist";
        }  else if (roomInstance.getStatus().equals("start")) {
            response = "Game already start";
        }   else {
            response = "success";
            roomInstance.addPlayer(chatMessage.getSender());
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            headerAccessor.getSessionAttributes().put("roomID", roomInstance.getRoomId());
            roomInstance.setJoinStatus("succes");
            this.template.convertAndSend("/room/"+room, roomInstance);
            
        }
        this.template.convertAndSend("/room/joinRoomStatus/", response);
        return roomInstance;
    }

    @SneakyThrows
    @MessageMapping("/game.playWord/{room}")
    public String sendWord(@DestinationVariable String room, @Payload Message chatMessage) {
        Room roomInstance = rooms.get(room);
        String mess = null;
        String answer = chatMessage.getContent();
        String user = chatMessage.getSender();
        String getCurrentPlayerName = roomInstance.getPlayers().get(roomInstance.getCurrentPlayer());
        if(!getCurrentPlayerName.equals(user) ){
            mess = "Not Your Turn";
        } else if(user.isEmpty() && answer.isEmpty()){
            mess = "Empty";
        } else if(!checkAnswer(roomInstance, answer)){
            mess = "Wrong Answer";
        } else {
            mess = "correct";
            roomInstance.setCurrentPlayer((roomInstance.getCurrentPlayer() + 1) % roomInstance.getPlayers().size());
            roomInstance.setCurrentWord(answer);
            goToNextPlayer(roomInstance);
        }
        this.template.convertAndSend("/room/sendWord/" + roomInstance.getRoomId(), mess);
        return mess;
    }
    private void goToNextPlayer(Room roomInstance){
        String getCurrentPlayerName = roomInstance.getPlayers().get(roomInstance.getCurrentPlayer());
        this.template.convertAndSend("/room/getCurrentPlayer/" + roomInstance.getRoomId(), getCurrentPlayerName);
        this.template.convertAndSend("/room/" + roomInstance.getRoomId(), roomInstance);
        roomInstance.getTimer().cancel();
        countdown(roomInstance);
    }
    private String getRandomWord() throws IOException, URISyntaxException {
        List<String> lines = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("static/txt/words.txt").toURI()));
        Collections.shuffle(lines);
        for (String line : lines) {
            String[] words = line.split(" ");
            if (words.length == 2) {
                return words[0];
            }
        }
        return null;
    }

    private boolean checkAnswer(Room room, String answer) throws IOException, URISyntaxException {
        List<String> lines = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("static/txt/words.txt").toURI()));
        for (String line : lines) {
            String[] words = line.toLowerCase().split(" ");
            if(words.length == 2){
                if(words[0].equals(room.getCurrentWord().toLowerCase())  && words[1].equals(answer.toLowerCase()) ){
                    return true;
                }
            }
        }
        return false;
    }

    private void startGame(Room room) {
        List<String> players = room.getPlayers();
        room.setStatus("start");
        String currentPlayer = players.get(room.getCurrentPlayer());
        try{
            room.setCurrentWord(getRandomWord());
        }
        catch (Exception e){

        }
        this.template.convertAndSend("/room/" + room.getRoomId(), room);
        this.template.convertAndSend("/room/getCurrentPlayer/" + room.getRoomId(), currentPlayer);
        countdown(room);
    }

    private void countdown(Room room) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            int time = 60;
            @Override
            public void run() {
                if (time < 0) {
                    room.getPlayers().remove(room.getCurrentPlayer());
                    timer.cancel();
                    goToNextPlayer(room);
                } else {
                    template.convertAndSend("/room/timer/" + room.getRoomId(), time);
                    time--;
                }
                if(room.getPlayers().size() == 1){
                    template.convertAndSend("/room/winner/" + room.getRoomId(), room.getPlayers().get(0));
                }
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
        room.setTimer(timer);
    }

    @MessageMapping("/chat.sendMessage/{room}")
    public Message sendMessage(@DestinationVariable String room, @Payload Message chatMessage) {
        this.template.convertAndSend("/room/"+room, chatMessage);
        return chatMessage;
    }

    @MessageMapping("/chat.addUser/joinRoom/{room}")
    public Message addUser(@DestinationVariable String room, @Payload Message chatMessage,
                           SimpMessageHeaderAccessor headerAccessor)  throws Exception {
        this.template.convertAndSend("/room/"+room, chatMessage);
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        return chatMessage;
    }
}
