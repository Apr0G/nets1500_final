package pennplanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import pennplanner.algorithm.DataLoader;
import pennplanner.algorithm.ScheduleBuilder;
import pennplanner.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ChatbotApp extends Application {

    // ── Brand palette ─────────────────────────────────────────────────────────
    private static final String PENN_BLUE    = "#011F5B";
    private static final String PENN_LIGHT   = "#3C6DC9";
    private static final String BG           = "#F0F2F5";
    private static final String BUBBLE_BOT   = "#FFFFFF";
    private static final String BUBBLE_USER  = "#0A84FF";
    private static final String TEXT_DARK    = "#1C1C1E";

    // ── Conversation states ───────────────────────────────────────────────────
    enum State { MAJOR, MINOR, DESIRED, EXCLUDED, INTERESTS, MAX_CU, START_SEASON, START_YEAR, DONE }
    private State state = State.MAJOR;

    // ── Profile data ──────────────────────────────────────────────────────────
    private String            majorId;
    private final List<String> minorIds            = new ArrayList<>();
    private final List<String> desiredElectiveIds  = new ArrayList<>();
    private final Set<String>  excludedCourseIds   = new LinkedHashSet<>();
    private final List<String> interests           = new ArrayList<>();
    private double            maxCUsPerSemester;
    private boolean           startInFall;
    private int               startYear;

    // ── Loaded data ───────────────────────────────────────────────────────────
    private Map<String, RawCourse> courses;
    private Map<String, MajorData> majors;
    private Map<String, MinorData> minors;

    // ── UI ────────────────────────────────────────────────────────────────────
    private VBox       chatBox;
    private ScrollPane scrollPane;
    private TextField  inputField;
    private Button     sendBtn;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        // Run scraper first to refresh courses.json and majors.json
        try {
            pennplanner.scraper.Scraper.run();
        } catch (Exception e) {
            System.out.println("Scraper failed, using existing data: " + e.getMessage());
        }

        try {
            courses = DataLoader.loadCourses(Path.of("courses.json"));
            majors  = DataLoader.loadMajors(Path.of("majors.json"));
            minors  = DataLoader.loadMinors(Path.of("minors.json"));
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Could not load data files:\n" + e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }

        // ── Header bar ────────────────────────────────────────────────────────
        Label pennLogo = new Label("🎓");
        pennLogo.setFont(Font.font(22));

        Label appTitle = new Label("Penn Course Planner");
        appTitle.setFont(Font.font("System Bold", 16));
        appTitle.setTextFill(Color.WHITE);

        Label appSub = new Label("AI Schedule Builder");
        appSub.setFont(Font.font(11));
        appSub.setTextFill(Color.web("#A8C0E8"));

        VBox titleCol = new VBox(1, appTitle, appSub);
        titleCol.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(10, pennLogo, titleCol);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setStyle("-fx-background-color: " + PENN_BLUE + ";");

        // ── Chat area ─────────────────────────────────────────────────────────
        chatBox = new VBox(12);
        chatBox.setPadding(new Insets(16, 14, 16, 14));
        chatBox.setFillWidth(true);

        scrollPane = new ScrollPane(chatBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + ";");

        // ── Input bar ─────────────────────────────────────────────────────────
        inputField = new TextField();
        inputField.setFont(Font.font(14));
        inputField.setPromptText("Type your response…");
        inputField.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: #D1D5DB;" +
            "-fx-border-radius: 22;" +
            "-fx-padding: 9 16 9 16;"
        );
        inputField.setOnAction(e -> handleInput());

        sendBtn = new Button("▶");
        sendBtn.setFont(Font.font("System Bold", 13));
        sendBtn.setStyle(
            "-fx-background-color: " + PENN_BLUE + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 22;" +
            "-fx-padding: 9 16 9 16;" +
            "-fx-cursor: hand;"
        );
        sendBtn.setOnAction(e -> handleInput());

        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle(
            "-fx-background-color: " + PENN_LIGHT + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 22;" +
            "-fx-padding: 9 16 9 16;" +
            "-fx-cursor: hand;"
        ));
        sendBtn.setOnMouseExited(e -> sendBtn.setStyle(
            "-fx-background-color: " + PENN_BLUE + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 22;" +
            "-fx-padding: 9 16 9 16;" +
            "-fx-cursor: hand;"
        ));

        HBox inputRow = new HBox(8, inputField, sendBtn);
        inputRow.setAlignment(Pos.CENTER);
        inputRow.setPadding(new Insets(10, 16, 10, 16));
        inputRow.setStyle(
            "-fx-background-color: #FAFAFA;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-width: 1 0 0 0;"
        );
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // ── Root ──────────────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scrollPane);
        root.setBottom(inputRow);

        stage.setTitle("Penn Course Planner");
        stage.setScene(new Scene(root, 760, 600));
        stage.setMinWidth(520);
        stage.setMinHeight(440);
        stage.show();

        askFor(State.MAJOR);
        inputField.requestFocus();
    }

    // ── Conversation flow ─────────────────────────────────────────────────────

    private void askFor(State s) {
        state = s;
        switch (s) {
            case MAJOR -> bot(
                "👋  Welcome! I'll help you build your Penn course plan.\n\n" +
                "What is your major?\n\n" +
                "Available: " + majors.keySet().stream().map(ChatbotApp::stripDegree).collect(Collectors.joining(", "))
            );
            case MINOR -> bot(
                "Any minors? Available:\n" +
                minors.keySet().stream().map(ChatbotApp::stripMinor).collect(Collectors.joining(", ")) +
                "\n\nType one at a time, or  done  /  none  to skip."
            );
            case DESIRED -> bot(
                "Any specific courses you want included? " +
                "Enter a course ID (e.g.  CIS 5450), or  done  to skip."
            );
            case EXCLUDED -> bot(
                "Any courses you want to exclude? Enter a course ID, or  done  /  none  to skip."
            );
            case INTERESTS -> bot(
                "What are your academic interests?\n\n" +
                "Enter one keyword at a time (e.g.  machine learning), or  done  to skip."
            );
            case MAX_CU    -> bot("Max CU units per semester? (e.g.  4.0  or  5.0)");
            case START_SEASON -> bot("Are you starting in  Fall  or  Spring?");
            case START_YEAR  -> bot("What year are you starting? (e.g.  2024)");
            case DONE        -> buildAndShow();
        }
    }

    private void handleInput() {
        String raw = inputField.getText().trim();
        if (raw.isEmpty()) return;
        inputField.clear();
        user(raw);

        switch (state) {
            case MAJOR -> {
                String matchedMajor = findMajor(raw);
                if (matchedMajor == null) {
                    bot("I don't recognize \"" + raw + "\".\nAvailable: " + String.join(", ", majors.keySet()));
                    return;
                }
                majorId = matchedMajor;
                askFor(State.MINOR);
            }
            case MINOR -> {
                if (isDone(raw)) { askFor(State.DESIRED); return; }
                String matchedMinor = findMinor(raw);
                if (matchedMinor == null) {
                    bot("\"" + raw + "\" not found.\nAvailable: " +
                        minors.keySet().stream().map(ChatbotApp::stripMinor).collect(Collectors.joining(", ")) +
                        "\n\nTry again or type  done.");
                } else if (minorIds.contains(matchedMinor)) {
                    bot("Already added. Enter another, or type  done.");
                } else {
                    minorIds.add(matchedMinor);
                    bot("✓  Added " + stripMinor(matchedMinor) + ". Add another, or type  done.");
                }
            }
            case DESIRED -> {
                if (isDone(raw)) { askFor(State.EXCLUDED); return; }
                if (!courses.containsKey(raw)) {
                    bot("\"" + raw + "\" not found in catalog. Check the ID and try again, or type  done.");
                } else {
                    desiredElectiveIds.add(raw);
                    bot("✓  Added " + raw + ". Add another, or type  done.");
                }
            }
            case EXCLUDED -> {
                if (isDone(raw)) { askFor(State.INTERESTS); return; }
                if (!courses.containsKey(raw)) {
                    bot("\"" + raw + "\" not found. Try again, or type  done.");
                } else {
                    excludedCourseIds.add(raw);
                    bot("✓  Excluded " + raw + ". Add another, or type  done.");
                }
            }
            case INTERESTS -> {
                if (isDone(raw)) { askFor(State.MAX_CU); return; }
                interests.add(raw.toLowerCase());
                bot("✓  Added \"" + raw + "\". Add another, or type  done.");
            }
            case MAX_CU -> {
                try {
                    double v = Double.parseDouble(raw);
                    if (v <= 0) throw new NumberFormatException();
                    maxCUsPerSemester = v;
                    askFor(State.START_SEASON);
                } catch (NumberFormatException e) {
                    bot("Please enter a positive number, e.g.  4.0");
                }
            }
            case START_SEASON -> {
                if (raw.equalsIgnoreCase("fall")) {
                    startInFall = true;  askFor(State.START_YEAR);
                } else if (raw.equalsIgnoreCase("spring")) {
                    startInFall = false; askFor(State.START_YEAR);
                } else {
                    bot("Please type  Fall  or  Spring.");
                }
            }
            case START_YEAR -> {
                try {
                    int v = Integer.parseInt(raw);
                    if (v < 2000 || v > 2100) throw new NumberFormatException();
                    startYear = v;
                    askFor(State.DONE);
                } catch (NumberFormatException e) {
                    bot("Please enter a valid year, e.g.  2024");
                }
            }
            case DONE -> { /* input is disabled at this point */ }
        }
    }

    // ── Schedule building ─────────────────────────────────────────────────────

    private void buildAndShow() {
        inputField.setDisable(true);
        sendBtn.setDisable(true);
        bot("⏳  Crunching the numbers…");

        new Thread(() -> {
            try {
                StudentProfile profile = new StudentProfile(
                    majorId,
                    List.copyOf(minorIds),
                    List.copyOf(desiredElectiveIds),
                    Set.copyOf(excludedCourseIds),
                    List.copyOf(interests),
                    maxCUsPerSemester,
                    8,
                    startInFall,
                    startYear,
                    "SEAS"
                );
                Schedule schedule = new ScheduleBuilder(courses, majors, minors, profile).build();
                Platform.runLater(() -> showSchedule(schedule.toString()));
            } catch (Exception e) {
                Platform.runLater(() -> bot("⚠️  Error: " + e.getMessage()));
            }
        }).start();
    }

    private void showSchedule(String text) {
        bot("🎓  Here's your personalized Penn course plan:");

        TextArea ta = new TextArea(text);
        ta.setEditable(false);
        ta.setFont(Font.font("Courier New", 12));
        ta.setWrapText(false);
        ta.setPrefHeight(520);
        ta.setPrefWidth(680);
        ta.setMaxWidth(Double.MAX_VALUE);
        ta.setStyle(
            "-fx-control-inner-background: #1E2030;" +
            "-fx-text-fill: #CDD6F4;" +
            "-fx-highlight-fill: #3C6DC9;" +
            "-fx-font-family: 'Courier New';" +
            "-fx-font-size: 12;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #313244;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.18));
        shadow.setRadius(8);
        shadow.setOffsetY(2);
        ta.setEffect(shadow);

        HBox row = new HBox(ta);
        row.setPadding(new Insets(4, 4, 4, 4));
        chatBox.getChildren().add(row);
        Platform.runLater(() -> scrollPane.setVvalue(1.0));

        bot("All done! Scroll up to review your plan.");
    }

    // ── Bubble helpers ────────────────────────────────────────────────────────

    private void bot(String text) {
        Label avatar = new Label("🤖");
        avatar.setMinSize(34, 34);
        avatar.setMaxSize(34, 34);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
            "-fx-background-color: " + PENN_BLUE + ";" +
            "-fx-background-radius: 17;" +
            "-fx-font-size: 15;"
        );

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(500);
        bubble.setFont(Font.font(14));
        bubble.setStyle(
            "-fx-background-color: " + BUBBLE_BOT + ";" +
            "-fx-text-fill: " + TEXT_DARK + ";" +
            "-fx-padding: 10 14 10 14;" +
            "-fx-background-radius: 4 18 18 18;"
        );
        DropShadow s = new DropShadow();
        s.setColor(Color.color(0, 0, 0, 0.07));
        s.setRadius(6);
        s.setOffsetY(1);
        bubble.setEffect(s);

        HBox row = new HBox(8, avatar, bubble);
        row.setAlignment(Pos.TOP_LEFT);
        chatBox.getChildren().add(row);
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void user(String text) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(500);
        bubble.setFont(Font.font(14));
        bubble.setStyle(
            "-fx-background-color: " + BUBBLE_USER + ";" +
            "-fx-text-fill: white;" +
            "-fx-padding: 10 14 10 14;" +
            "-fx-background-radius: 18 4 18 18;"
        );
        DropShadow s = new DropShadow();
        s.setColor(Color.color(0, 0, 0, 0.1));
        s.setRadius(6);
        s.setOffsetY(1);
        bubble.setEffect(s);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.TOP_RIGHT);
        chatBox.getChildren().add(row);
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private static boolean isDone(String s) {
        return s.equalsIgnoreCase("done") || s.equalsIgnoreCase("none");
    }

    /** Case-insensitive major lookup — matches on stripped name so user can type "Computer Science". */
    private String findMajor(String input) {
        return majors.keySet().stream()
            .filter(k -> k.equalsIgnoreCase(input) || stripDegree(k).equalsIgnoreCase(input))
            .findFirst().orElse(null);
    }

    /** Finds a minor key case-insensitively; user can type "CIS" to match "CIS Minor" or "CIS, Minor". */
    private String findMinor(String input) {
        return minors.keySet().stream()
            .filter(k -> k.equalsIgnoreCase(input)
                      || k.equalsIgnoreCase(input + " Minor")
                      || k.equalsIgnoreCase(input + ", Minor")
                      || stripMinor(k).equalsIgnoreCase(input))
            .findFirst().orElse(null);
    }

    /** Strips trailing degree suffix like ", BSE" / ", BAS" / ", BS" etc. for display. */
    private static String stripDegree(String name) {
        return name.replaceAll(",\\s*[A-Z]{2,4}$", "").trim();
    }

    /** Strips trailing " Minor" or ", Minor" for display. */
    private static String stripMinor(String key) {
        if (key.endsWith(", Minor")) return key.substring(0, key.length() - 7).trim();
        if (key.endsWith(" Minor"))  return key.substring(0, key.length() - 6).trim();
        return key;
    }
}
