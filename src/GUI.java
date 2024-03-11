import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.*;
import javafx.stage.StageStyle;

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
	public static List<Player> players = new ArrayList<Player>();

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



	// -------------------------------------------
	// | Maze: (0,0)              | Score: (1,0) |
	// |-----------------------------------------|
	// | boardGrid (0,1)          | scorelist    |
	// |                          | (1,1)        |
	// -------------------------------------------

	@Override
	public void start(Stage primaryStage) {
		try {

			clientSocket = new Socket("10.10.138.107", 6750);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			me = new Player("Orville", 9, 4, "up");

			// register all active players
			sendMessageToServer("REGISTER_ALL_PLAYERS");

			// Insert your username here
			showPreGameDialog();

			// Send registration message
			String registrationMessage = createRegistrationMessage(); // Implement message creation
			System.out.println(registrationMessage);
			sendMessageToServer(registrationMessage); // Implement message sending

			new Thread(() -> receiveMessages()).start();

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

			// Setting up standard players

			players.add(me);
			fields[9][4].setGraphic(new ImageView(hero_up));

			scoreList.setText(getScoreList());
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
			me.name = usernameField.getText();
			usernameDialog.close();
			preStage.close();
		});

		preStage.onCloseRequestProperty().addListener(e -> {
			System.out.println("Shutting down for game...");
			cleanup();
		});

		preStage.setTitle("Insert your player name");
		preStage.initModality(Modality.APPLICATION_MODAL);
		preStage.initStyle(StageStyle.UTILITY);

		preStage.showAndWait();
	}


	public void playerMoved(int delta_x, int delta_y, String direction) {
		me.direction = direction;
		int x = me.getXpos(), y = me.getYpos();

		if (board[y + delta_y].charAt(x + delta_x) == 'w') {
			me.addPoints(-1);
		} else {
			Player p = getPlayerAt(x + delta_x, y + delta_y);
			if (p != null) {
				me.addPoints(10);
				p.addPoints(-10);
			} else {
				me.addPoints(1);

				fields[x][y].setGraphic(new ImageView(image_floor));
				x += delta_x;
				y += delta_y;

				if (direction.equals("right")) {
					fields[x][y].setGraphic(new ImageView(hero_right));
				}
				;
				if (direction.equals("left")) {
					fields[x][y].setGraphic(new ImageView(hero_left));
				}
				;
				if (direction.equals("up")) {
					fields[x][y].setGraphic(new ImageView(hero_up));
				}
				;
				if (direction.equals("down")) {
					fields[x][y].setGraphic(new ImageView(hero_down));
				}
				;

				me.setXpos(x);
				me.setYpos(y);
			}
		}
        // Send movement update to server
        sendMessageToServer("MOVE," + me.getName() + "," + me.getXpos() + "," + me.getYpos() + "," + direction);
        scoreList.setText(getScoreList());
	}

	public String getScoreList() {
		StringBuffer b = new StringBuffer(100);
		for (Player p : players) {
			b.append(p + "\r\n");
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

	private String createRegistrationMessage() {
		String message = "REGISTER," + me.name + "," + me.getXpos() + "," + me.getYpos();
		return message;
	}

	private void sendMessageToServer(String message) {
		try {
			outToServer.writeBytes(message + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void receiveMessages() {
		try {
			while (true) {
				String message = inFromServer.readLine();
				// Process received message (movement, scores, etc.)
				// Update game state and visuals

				String[] messageParts = message.split(",");
                String event = messageParts[0];

                if (event.equals("REGISTER")) {
					System.out.println("Received registration message: " + message);
                    String playerName = messageParts[1];
                    int x = Integer.parseInt(messageParts[2]);
                    int y = Integer.parseInt(messageParts[3]);
                    String direction = messageParts.length > 4 ? messageParts[4] : "up";
                    players.add(new Player(playerName, x, y, direction));
                }

			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // Exit the application on communication error
		} finally {
			cleanup();
		}
	}

	private void cleanup() {
		// Close connections and resources
	}

}