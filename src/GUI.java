import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.*;
import javafx.stage.StageStyle;

import javax.security.auth.callback.Callback;

public class GUI extends Application {

	private Socket clientSocket;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;

	//-----------------------------------------------------------------------------
	public static final int size = 20;
	public static final int scene_height = size * 20 + 100;
	public static final int scene_width = size * 20 + 200;
	public static Image image_floor;
	public static Image image_wall;
	public static Image hero_right, hero_left, hero_up, hero_down;

	public static Player me;
	public static List<Player> players = new ArrayList<>();

	private Label[][] fields;
	private TextArea scoreList;

	private String[] board = {    // 20x20
			"wwwwwwwwwwwwwwwwwwww",
			"w        ww        w",
			"w w  w  www w  w  ww",
			"w w  w   ww w  w  ww",
			"w  w               w",
			"w w w w w w w  w  ww",
			"w w     www w  w  ww",
			"w w     w w w  w  ww",
			"w   w w  w  w  w   w",
			"w     w  w  w  w   w",
			"w ww ww        w  ww",
			"w  w w    w    w  ww",
			"w        ww w  w  ww",
			"w         w w  w  ww",
			"w        w     w  ww",
			"w  w              ww",
			"w  w www  w w  ww ww",
			"w w      ww w     ww",
			"w   w   ww  w      w",
			"wwwwwwwwwwwwwwwwwwww"
	};

	private String hostname;
	private int port;

	@Override
	public void init() {
		this.hostname = "localhost";
		this.port = 6750;
	}

	// -------------------------------------------
	// | Maze: (0,0)              | Score: (1,0) |
	// |-----------------------------------------|
	// | boardGrid (0,1)          | scorelist    |
	// |                          | (1,1)        |
	// -------------------------------------------

	@Override
	public void start(Stage primaryStage) {
		try {

			clientSocket = new Socket(hostname, port);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			me = new Player("Player", 0, 0, "up");

			// registration of the player is done in the pregame dialog
			showPreGameDialog();

			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(0, 10, 0, 10));

			Text mazeLabel = new Text("Maze:");
			mazeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

			Text scoreLabel = new Text("Score:");
			scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

			scoreList = new TextArea();

			GridPane boardGrid = new GridPane();

			image_wall = new Image(getClass().getResourceAsStream("Image/wall4.png"), size, size, false, false);
			image_floor = new Image(getClass().getResourceAsStream("Image/floor1.png"), size, size, false, false);

			hero_right = new Image(getClass().getResourceAsStream("Image/heroRight.png"), size, size, false, false);
			hero_left = new Image(getClass().getResourceAsStream("Image/heroLeft.png"), size, size, false, false);
			hero_up = new Image(getClass().getResourceAsStream("Image/heroUp.png"), size, size, false, false);
			hero_down = new Image(getClass().getResourceAsStream("Image/heroDown.png"), size, size, false, false);

			fields = new Label[20][20];
			for (int j = 0; j < 20; j++) {
				for (int i = 0; i < 20; i++) {
					switch (board[j].charAt(i)) {
						case 'w':
							fields[i][j] = new Label("", new ImageView(image_wall));
							break;
						case ' ':
							fields[i][j] = new Label("", new ImageView(image_floor));
							break;
						default:
							throw new Exception("Illegal field value: " + board[j].charAt(i));
					}
					boardGrid.add(fields[i][j], i, j);
				}
			}
			scoreList.setEditable(false);

			grid.add(mazeLabel, 0, 0);
			grid.add(scoreLabel, 1, 0);
			grid.add(boardGrid, 0, 1);
			grid.add(scoreList, 1, 1);

			Scene scene = new Scene(grid, scene_width, scene_height);
			primaryStage.setScene(scene);
			primaryStage.show();

			scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
				switch (event.getCode()) {
					case UP, W:
						playerMoved(0, -1, "up");
						break;
					case DOWN, S:
						playerMoved(0, +1, "down");
						break;
					case LEFT, A:
						playerMoved(-1, 0, "left");
						break;
					case RIGHT, D:
						playerMoved(+1, 0, "right");
						break;
					default:
						break;
				}
			});
			scene.setOnMouseClicked(new EventHandler<>() {
				@Override
				public void handle(javafx.scene.input.MouseEvent mouseEvent) {
					if (mouseEvent.getButton() != MouseButton.PRIMARY) return;
					System.out.println("shooting");
				}
			});
			primaryStage.setTitle("Maze multiplayer game");
			primaryStage.getIcons().add(image_floor);
			primaryStage.setOnCloseRequest(e -> {
				System.out.println("Shutting down for game...");
				cleanup();
			});

			// Setting up default player
			players.add(me);
			//fields[me.getXpos()][me.getYpos()].setGraphic(new ImageView(hero_up)); // Set the player on the board
			updateScoreList();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showPreGameDialog() {
		Dialog usernameDialog = new Dialog();
		GridPane gridDialog = new GridPane();
		gridDialog.setHgap(10);
		gridDialog.setVgap(10);
		gridDialog.setAlignment(javafx.geometry.Pos.CENTER);

		TextField usernameField = new TextField();
		usernameField.setPromptText("Username");
		gridDialog.add(usernameField, 0, 0);
		Button okButton = new Button("OK");
		okButton.setDefaultButton(true);
		okButton.setPrefSize(40, 20);
		gridDialog.add(okButton, 0, 1);

		Stage preStage = new Stage();
		Scene usernameScene = new Scene(gridDialog, 300, 300);
		preStage.setScene(usernameScene);

		okButton.setOnAction(e -> {
			// send request for username registration
			String name = usernameField.getText();
			if (name.isEmpty()) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Invalid username");
				alert.setContentText("Please enter a valid username");
				alert.showAndWait();
			} else if (!nameIsAllowed(name)) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Invalid username");
				alert.setContentText("Username already taken");
				alert.showAndWait();
			} else {
				me.name = name;
				int[] coords = Player.spawnRandomLocation(board, players);
				new Thread(this::receiveMessages).start(); // activate the listener
				sendMessageToServer(createRegistrationMessage(name, coords[0], coords[1]));
				sendMessageToServer("REGISTER_ALL_PLAYERS");
				usernameDialog.close();
				preStage.close();
			}
		});

		preStage.setOnCloseRequest(e -> {
			System.out.println("Did not enter a username");
			cleanup();
		});

		preStage.setTitle("Insert your player name");
		preStage.initModality(Modality.APPLICATION_MODAL);

		preStage.showAndWait();
	}

	public void playerMoved(int delta_x, int delta_y, String direction) {
		me.direction = direction;
		int x = me.getXpos(), y = me.getYpos();
		//fields[x][y].setGraphic(new ImageView(image_floor));

		String pointMessage = "";
		if (board[y + delta_y].charAt(x + delta_x) == 'w') {
//			me.addPoints(-1);
			pointMessage = "," + "-" + 1;
		} else {
			Player p = getPlayerAt(x + delta_x, y + delta_y);
			if (p != null) {
//				me.addPoints(10);
//				p.addPoints(-10);
				pointMessage = "," + 10 + "," + p.name + "," + "-" + 10;
			} else {
//				me.addPoints(1);
				pointMessage = "," + 1;
				x += delta_x;
				y += delta_y;
			}
		}
		sendMessageToServer("MOVE," + me.getName() + "," + x + "," + y + "," + direction + pointMessage);
	}

	private void updateScoreList() {
		scoreList.setText(getScoreList());
	}

	public String getScoreList() {
		StringBuffer b = new StringBuffer(100);
		for (Player p : players) {
			if (p == me) {
				b.append(p + " (you)\r\n");
			} else {
				b.append(p + "\r\n");
			}
		}
		return b.toString();
	}

	public Player getPlayerAt(int x, int y) {
		for (Player p : players) {
			if (p.getXpos() == x && p.getYpos() == y) {
				return p;
			}
		}
		return null;
	}

	private String createRegistrationMessage(String name, int x, int y) {
		String message = "REGISTER," + name + "," + x + "," + y;
//		String message = "REGISTER," + me.name + "," + me.getXpos() + "," + me.getYpos();
		return message;
	}

	private boolean nameIsAllowed(String name) {
		String message = "NAME_SEARCH," + name;
		sendMessageToServer(message);
		String response = receiveMessageFromServer();
		return response.equals("NAME_AVAILABLE");
	}

	private void sendMessageToServer(String message) {
		try {
			outToServer.writeBytes(message + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String receiveMessageFromServer() {
		try {
			return inFromServer.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void receiveMessages() {
		try {
			while (true) {
				String message = inFromServer.readLine();
				if (message != null) {
					processServerMessage(message);
				}
			}
		} catch (IOException e) {
			e.printStackTrace(); //TODO: disable this in production
			System.exit(1); // Exit the application on communication error
		} finally {
			cleanup();
		}
	}

	private void processServerMessage(String message) {
		Platform.runLater(() -> {
			String[] messageParts = message.split(",");
			String event = messageParts[0];

			switch (event) {
				case "REGISTER":
					handleRegisterMessage(messageParts);
					break;
				case "MOVE":
					handleMoveMessage(messageParts);
					break;
				case "DISCONNECT":
					handleDisconnectMessage(messageParts);
					break;
				case "POINTS":
					handlePointsMessage(messageParts);
					break;
				default:
					break;
			}
		});
	}

	private void handleRegisterMessage(String[] messageParts) {
		String playerName = messageParts[1];
		int x = Integer.parseInt(messageParts[2]);
		int y = Integer.parseInt(messageParts[3]);
		String direction = messageParts.length > 4 ? messageParts[4] : "up";
		System.out.println("Player " + playerName + " registered");
		Player player = getPlayerByName(playerName);
		if (player != null) {
			player.setXpos(x);
			player.setYpos(y);
			player.setDirection(direction);
		}
		else {
			// Player doesn't exist, create a new player and add it to the list
			player = new Player(playerName, x, y, direction);
			players.add(player);
		}
		updatePlayerOnGUI(player);
		updateScoreList();
	}

	private void handleMoveMessage(String[] messageParts) {
		String playerName = messageParts[1];
		int x = Integer.parseInt(messageParts[2]);
		int y = Integer.parseInt(messageParts[3]);
		String direction = messageParts.length > 4 ? messageParts[4] : "up";
		Player playerToUpdate = getPlayerByName(playerName);
		if (playerToUpdate != null) {
			//System.out.println("Player " + playerName + " moved to " + x + ", " + y + " in direction " + direction);
			int oldX = playerToUpdate.getXpos();
			int oldY = playerToUpdate.getYpos();

			fields[oldX][oldY].setGraphic(new ImageView(image_floor));

			playerToUpdate.setXpos(x);
			playerToUpdate.setYpos(y);
			playerToUpdate.setDirection(direction);
			updatePlayerOnGUI(playerToUpdate);
			updateScoreList();
		}
	}

	private void handleDisconnectMessage(String[] messageParts) {
		String playerName = messageParts[1];
		Player playerToRemove = getPlayerByName(playerName);
		if (playerToRemove != null) {
			players.remove(playerToRemove);
			int xpos = playerToRemove.getXpos();
			int ypos = playerToRemove.getYpos();
			fields[xpos][ypos].setGraphic(new ImageView(image_floor));
			updateScoreList();
		}
	}

	private void handlePointsMessage(String[] messageParts) {
		String[] playerPoints = new String[messageParts.length - 1]; // -1 because the first element is "POINTS"
		System.arraycopy(messageParts, 1, playerPoints, 0, playerPoints.length);
		List<Player> playersToRemove = new ArrayList<>(players);
		for (String playerPoint : playerPoints) {
			String[] nameAndPoint = playerPoint.split(":");
			String playerName = nameAndPoint[0];
			int points = Integer.parseInt(nameAndPoint[1]);
			Player playerToUpdate = getPlayerByName(playerName);
			if (playerToUpdate != null) {
				playerToUpdate.point = points;
				playersToRemove.remove(playerToUpdate);
			} else {
				players.remove(playerToUpdate);
				fields[playerToUpdate.getXpos()][playerToUpdate.getYpos()].setGraphic(new ImageView(image_floor));
				System.out.println("Player " + playerName + " not found");
			}
		}
		for (Player p : playersToRemove) {
			players.remove(p);
			fields[p.getXpos()][p.getYpos()].setGraphic(new ImageView(image_floor));
		}
		updateScoreList();
	}

	private Player getPlayerByName(String name) {
		for (Player p : players) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	private void updatePlayerOnGUI(Player player) {
		Platform.runLater(() -> {
			int x = player.getXpos();
			int y = player.getYpos();
			String direction = player.getDirection();
			if (direction.equals("right")) {
				fields[x][y].setGraphic(new ImageView(hero_right));
			}
			if (direction.equals("left")) {
				fields[x][y].setGraphic(new ImageView(hero_left));
			}
			if (direction.equals("up")) {
				fields[x][y].setGraphic(new ImageView(hero_up));
			}
			if (direction.equals("down")) {
				fields[x][y].setGraphic(new ImageView(hero_down));
			}
		});
	}

	private void cleanup() {
		// Close connections and resources
		sendMessageToServer("DISCONNECT," + me.getName());
		try {
			if (clientSocket != null) {
				clientSocket.close();
			}
			if (outToServer != null) {
				outToServer.close();
			}
			if (inFromServer != null) {
				inFromServer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Platform.exit();
		System.exit(0);
	}
}