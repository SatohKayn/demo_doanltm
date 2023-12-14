document.addEventListener("DOMContentLoaded", function () {
  var stompClient = null;
  var username = null;
  var createRoomButton = document.getElementById("create-room-button");
  var joinRoomButton = document.getElementById("join-room-button");
  var readyButton = document.getElementById("ReadyButton");
  var connectingElement = document.querySelector(".connecting");
  var roomIdText = document.getElementById("roomId");
  var currentWord = document.getElementById("CurrentWord");
  var yourAnswer = document.getElementById("yourAnswer");
  var menu = document.getElementById("menu-page");
  var game = document.getElementById("game-page");
  function createRoom(event) {
    username = prompt("Enter your name");
    menu.classList.add("hidden");
    game.classList.remove("hidden");
    if (username) {
      var socket = new SockJS("/ws");
      stompClient = Stomp.over(socket);
      stompClient.connect(
        {},
        function (frame) {
          stompClient.subscribe("/room/roomCreated", function (response) {
            var roomInfo = JSON.parse(response.body);
            roomIdText.textContent = roomInfo.roomId;
            stompClient.subscribe(
              "/room/" + roomInfo.roomId,
              function (response) {
                var roomInfo = JSON.parse(response.body);
                getListUsername(roomInfo);
                currentWord.textContent = roomInfo.currentWord;
              }
            );
            onStart(username, roomInfo);
            onConnected(username, roomInfo);
          });

          stompClient.send(
            "/app/game.createRoom",
            {},
            JSON.stringify({ sender: username })
          );
        },
        onError
      );
    }
    connectingElement.classList.remove("hidden");
    event.preventDefault();
  }

  function joinRoom(event) {
    username = prompt("Enter your name");
    menu.classList.add("hidden");
    game.classList.remove("hidden");
    if (username) {
      var socket = new SockJS("/ws");
      stompClient = Stomp.over(socket);
      stompClient.connect(
        {},
        function (frame) {
          var roomId = document.getElementById("roomInput").value.trim();
          if (roomId === "") return;
          stompClient.subscribe("/room/" + roomId, function (response) {
            var roomInfo = JSON.parse(response.body);
            roomIdText.textContent = roomInfo.roomId;
            getListUsername(roomInfo);
            currentWord.textContent = roomInfo.currentWord;
            onConnected(username, roomInfo);
            onStart(username, roomInfo);
          });

          stompClient.subscribe(
              "/room/joinRoomStatus/",
              function (response) {
                alert(response.body);
              }
          );

          stompClient.send(
            "/app/game.joinRoom/" + roomId,
            {},
            JSON.stringify({ sender: username })
          );
        },
        onError
      );
    }
    connectingElement.classList.remove("hidden");
    event.preventDefault();
  }

  function onConnected(name, roomInfo) {
    readyButton.addEventListener("click", function () {
      ready(name, roomInfo.roomId);
    });



    stompClient.subscribe(
      "/room/getCurrentPlayer/" + roomInfo.roomId,
      function (response) {
        document.getElementById("CurrentPlayer").innerText = response.body;
      }
    );

    stompClient.subscribe(
      "/room/timer/" + roomInfo.roomId,
      function (response) {
        var time = JSON.parse(response.body);
        document.getElementById("Timer").innerText = time;
      }
    );

    stompClient.subscribe(
      "/room/winner/" + roomInfo.roomId,
      function (response) {
        alert(response.body);
      }
    );

    connectingElement.classList.add("hidden");
  }

  function onError(error) {
    connectingElement.textContent =
      "Could not connect to WebSocket server. Please refresh this page to try again!";
    connectingElement.style.color = "red";
  }

  function ready(name, RoomId) {
    stompClient.send(
      "/app/game.ready/" + RoomId,
      {},
      JSON.stringify({ content: "ready", sender: name })
    );
  }

  function onStart(name, RoomInfo) {
    yourAnswer.addEventListener("keydown", function (event) {
      stompClient.subscribe(
        "/room/sendWord/" + RoomInfo.roomId,
        function (response) {
          document.getElementById("AnserValid").innerText = response.body;
        }
      );
      if (event.key === "Enter") {
        event.preventDefault();
        if (yourAnswer.value.trim() !== "") {
          stompClient.send(
            "/app/game.playWord/" + RoomInfo.roomId,
            {},
            JSON.stringify({ content: yourAnswer.value, sender: name })
          );
          yourAnswer.value = "";
        }
      }
    });
  }

  function getListUsername(roomInfo) {
    var players = roomInfo.players;
    var playersList = document.getElementById("playersList");
    playersList.innerText = "";
    var ul = document.createElement("ul");
    for (var i = 0; i < players.length; i++) {
      var li = document.createElement("li");
      li.textContent = players[i];
      ul.appendChild(li);
    }
    playersList.appendChild(ul);
  }

  createRoomButton.addEventListener("click", createRoom, true);
  joinRoomButton.addEventListener("click", joinRoom, true);
});
