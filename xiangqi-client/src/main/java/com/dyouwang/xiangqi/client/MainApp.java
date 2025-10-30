package com.dyouwang.xiangqi.client;

// --- JavaFX Imports ---
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets; // 【新】用于布局间距
import javafx.geometry.Pos;
import javafx.scene.Node; // 【新】用于返回视图
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button; // 【新】需要按钮
import javafx.scene.control.Label;
import javafx.scene.layout.*;     // 导入所有 layout
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

// --- Core 模块导入 ---
import com.dyouwang.xiangqi.*; // 导入所有核心类
import com.dyouwang.xiangqi.messages.*; // 导入所有消息类

// --- 其他 Java 导入 ---
import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {

    // --- 常量 (不变) ---
    private static final int BOARD_ROWS = 10;
    private static final int BOARD_COLS = 9;
    private static final double CELL_SIZE = 60.0;
    private static final double BOARD_WIDTH = BOARD_COLS * CELL_SIZE;
    private static final double BOARD_HEIGHT = BOARD_ROWS * CELL_SIZE;
    
    // --- 游戏模式枚举 ---
    private enum GameMode {
        VS_AI,
        ONLINE,
        NONE // 初始状态
    }

    // --- 成员变量 ---
    private Stage stage; // 主窗口

    
    private Game game;
    private AIEngine aiEngine; // AI 引擎
    private ServerConnector serverConnector;
    private Player myPlayer = null;
    private GameMode currentGameMode = GameMode.NONE;

    // --- 棋盘相关 UI 节点 ---
    private Pane pieceLayer;
    private Pane highlightLayer;
    private Position selectedPiecePosition = null;
    private List<Position> availableMovePositions = new ArrayList<>();

    
    /**
     * JavaFX 主入口
     */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.game = new Game(); // 初始化一个 Game 实例
        
        // 1. 【修改】直接显示初始的欢迎视图
        //    showWelcomeView() 会负责创建并设置第一个 Scene
        showWelcomeView();

        // 2. 【修改】只管显示窗口
        stage.show();
    }

    // =========================================================================
    // --- 视图切换方法 ---
    // =========================================================================

    /**
     * 【修改】显示欢迎视图
     */
    private void showWelcomeView() {
        Label titleLabel = new Label("中国象棋");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 50));
        titleLabel.setTextFill(Color.WHITE); // 移除, 在 VBox 上设置背景

        Button startButton = new Button("开始游戏");
        startButton.setFont(Font.font("System", FontWeight.NORMAL, 24));
        startButton.setPrefSize(200, 60);
        startButton.setOnAction(event -> showModeSelectionView());

        VBox welcomeLayout = new VBox(50);
        welcomeLayout.getChildren().addAll(titleLabel, startButton);
        welcomeLayout.setAlignment(Pos.CENTER);
        welcomeLayout.setStyle("-fx-background-color: #333333;"); // 深色背景

        // 【新】为这个视图创建并设置 Scene
        Scene scene = new Scene(welcomeLayout, 600, 700); // 菜单大小
        stage.setScene(scene);
        stage.setTitle("欢迎");
    }

    /**
     * 【修改】显示模式选择视图
     */
    private void showModeSelectionView() {
        Label titleLabel = new Label("选择模式");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 40));
        titleLabel.setTextFill(Color.WHITE);

        Button vsAiButton = new Button("人机对战");
        vsAiButton.setFont(Font.font("System", 20));
        vsAiButton.setPrefSize(250, 60);
        vsAiButton.setOnAction(event -> startGame(GameMode.VS_AI));

        Button onlineButton = new Button("联网对战");
        onlineButton.setFont(Font.font("System", 20));
        onlineButton.setPrefSize(250, 60);
        onlineButton.setOnAction(event -> startGame(GameMode.ONLINE));

        Button backButton = new Button("返回");
        backButton.setFont(Font.font("System", 16));
        backButton.setOnAction(event -> showWelcomeView());
        VBox.setMargin(backButton, new Insets(40, 0, 0, 0));

        VBox modeLayout = new VBox(30);
        modeLayout.getChildren().addAll(titleLabel, vsAiButton, onlineButton, backButton);
        modeLayout.setAlignment(Pos.CENTER);
        modeLayout.setStyle("-fx-background-color: #333333;"); // 深色背景

        // 【新】为这个视图创建并设置 Scene
        Scene scene = new Scene(modeLayout, 600, 700); // 菜单大小
        stage.setScene(scene);
        stage.setTitle("选择模式");
    }

    /**
     * 【修改】根据选择的模式开始游戏 (加载棋盘视图)
     */
    private void startGame(GameMode mode) {
        // ... (重置 game, myPlayer 等逻辑不变) ...
        this.currentGameMode = mode;
        this.game = new Game();
        this.myPlayer = null;
        this.selectedPiecePosition = null;
        this.availableMovePositions.clear();
        
        System.out.println("开始游戏, 模式: " + mode);

        // 1. 创建游戏视图 (这是一个 StackPane)
        Node gameView = createGameView(); 

        // 2. 【修改】为游戏视图创建 *新的*、*精确大小* 的 Scene
        Scene gameScene = new Scene((StackPane) gameView, BOARD_WIDTH, BOARD_HEIGHT);
        
        // 3. 【修改】将新 Scene 设置到 Stage
        stage.setScene(gameScene);
        // stage.setWidth/Height 不再需要, Scene 会自动调整 Stage

        // 4. 根据模式进行初始化
        if (mode == GameMode.ONLINE) {
            stage.setTitle("象棋 - 联网对战 (连接中...)");
            if (serverConnector == null) {
                String serverIp = "localhost";
                serverConnector = new ServerConnector("ws://" + serverIp + ":8080/game", this);
            }
            if (!serverConnector.isConnected()) {
                 serverConnector.connect();
            }
            pieceLayer.setDisable(true); // 等待服务器消息
        } else { // VS_AI
            stage.setTitle("象棋 - 人机对战 (你是红方)");
            this.myPlayer = Player.RED;
            this.aiEngine = new AIEngine();
            drawPieces(game.getBoard()); // 立即绘制开局棋盘
            pieceLayer.setDisable(false); // 启用点击
            System.out.println("轮到你了 (RED)");
        }
    }

    // =========================================================================
    // --- 游戏棋盘视图 (Game View) 创建与逻辑 ---
    // (这是我们之前的所有 GUI 代码)
    // =========================================================================

    /**
     * 【重构】创建包含棋盘所有视觉元素的主游戏视图 Node
     */
    private Node createGameView() {
        GridPane boardGrid = createBoardGrid();
        boardGrid.setStyle("-fx-background-color: burlywood;");

        Canvas canvas = new Canvas(BOARD_WIDTH, BOARD_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawBoardLines(gc);

        // 初始化棋盘层 (使用成员变量)
        pieceLayer = createPieceLayer();
        highlightLayer = createHighlightLayer();

        // 绑定背景点击事件 (用于移动到空位)
        pieceLayer.setOnMouseClicked(event -> {
            double clickX = event.getX();
            double clickY = event.getY();
            double margin = CELL_SIZE / 2.0;

            int col = (int) Math.round((clickX - margin) / CELL_SIZE);
            int row = (int) Math.round((clickY - margin) / CELL_SIZE);
            Position targetPos = new Position(row, col);

            if (targetPos.isValid()) {
                Piece pieceAtTarget = game.getBoard().getPiece(targetPos);
                if (pieceAtTarget == null) {
                    handleEmptySquareClick(targetPos);
                }
            } else {
                 System.out.println("点击在棋盘外");
                 clearLocalSelection();
            }
        });

        StackPane gameStackPane = new StackPane();
        // 顺序: 背景, 线条, 棋子, 提示
        gameStackPane.getChildren().addAll(boardGrid, canvas, pieceLayer, highlightLayer);
        gameStackPane.setAlignment(Pos.CENTER);
        
        return gameStackPane;
    }

    // --- createBoardGrid() (不变) ---
    private GridPane createBoardGrid() {
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setMinSize(BOARD_WIDTH, BOARD_HEIGHT);
        gridPane.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        gridPane.setMaxSize(BOARD_WIDTH, BOARD_HEIGHT);
        return gridPane;
    }

    // --- drawBoardLines() (不变 - 使用 margin 的版本) ---
    private void drawBoardLines(GraphicsContext gc) {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        double margin = CELL_SIZE / 2.0;
        for (int row = 0; row < BOARD_ROWS; row++) {
            double y = margin + row * CELL_SIZE;
            gc.strokeLine(margin, y, BOARD_WIDTH - margin, y);
        }
        for (int col = 0; col < BOARD_COLS; col++) {
            double x = margin + col * CELL_SIZE;
            gc.strokeLine(x, margin, x, margin + 4 * CELL_SIZE);
            gc.strokeLine(x, margin + 5 * CELL_SIZE, x, BOARD_HEIGHT - margin);
        }
        gc.strokeLine(margin + 3 * CELL_SIZE, margin + 0 * CELL_SIZE, margin + 5 * CELL_SIZE, margin + 2 * CELL_SIZE);
        gc.strokeLine(margin + 3 * CELL_SIZE, margin + 2 * CELL_SIZE, margin + 5 * CELL_SIZE, margin + 0 * CELL_SIZE);
        gc.strokeLine(margin + 3 * CELL_SIZE, margin + 7 * CELL_SIZE, margin + 5 * CELL_SIZE, margin + 9 * CELL_SIZE);
        gc.strokeLine(margin + 3 * CELL_SIZE, margin + 9 * CELL_SIZE, margin + 5 * CELL_SIZE, margin + 7 * CELL_SIZE);
    }

    // --- createPieceLayer() (不变, 返回 Pane) ---
    private Pane createPieceLayer() {
        Pane pane = new Pane();
        pane.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        return pane;
    }

    // --- createHighlightLayer() (不变, 返回 Pane) ---
    private Pane createHighlightLayer() {
        Pane pane = new Pane();
        pane.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        pane.setMouseTransparent(true); // 点击穿透
        return pane;
    }

    /**
     * 绘制棋子 (不变)
     */
    private void drawPieces(Board board) {
        pieceLayer.getChildren().clear(); 
        double margin = CELL_SIZE / 2.0;
        double radius = CELL_SIZE * 0.4;

        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {
                final Position currentPos = new Position(row, col);
                final Piece piece = board.getPiece(currentPos);

                if (piece != null) {
                    Circle pieceCircle = new Circle(radius);
                    if (piece.getPlayer() == Player.RED) {
                        pieceCircle.setFill(Color.rgb(255, 90, 90));
                        pieceCircle.setStroke(Color.DARKRED);
                    } else {
                        pieceCircle.setFill(Color.rgb(90, 90, 90));
                        pieceCircle.setStroke(Color.BLACK);
                    }
                    pieceCircle.setStrokeWidth(2.0);
                    pieceCircle.setStrokeType(StrokeType.INSIDE);
                    
                    Label pieceLabel = new Label(piece.getName());
                    pieceLabel.setFont(Font.font("System", FontWeight.BOLD, radius * 1.2));
                    pieceLabel.setTextFill(Color.WHITE);

                    StackPane piecePane = new StackPane(pieceCircle, pieceLabel);
                    piecePane.setAlignment(Pos.CENTER);
                    
                    piecePane.setLayoutX(margin + col * CELL_SIZE - radius);
                    piecePane.setLayoutY(margin + row * CELL_SIZE - radius);
                    
                    piecePane.setUserData(currentPos); // 存储位置

                    piecePane.setOnMouseClicked(event -> {
                        handlePieceClick(currentPos); 
                        event.consume(); // 阻止事件冒泡到 pieceLayer
                    });

                    pieceLayer.getChildren().add(piecePane);
                }
            }
        }
    }
    
    // =========================================================================
    // --- 游戏交互逻辑 (Click Handlers) ---
    // =========================================================================

    /**
     * 【修改】处理棋子点击 (适配两种模式)
     */
    private void handlePieceClick(Position clickedPos) {
        Piece clickedPiece = game.getBoard().getPiece(clickedPos);
        StackPane clickedNode = getNodeForPosition(clickedPos); 
        if (clickedPiece == null || clickedNode == null) return;
        System.out.println("点击了: " + clickedPiece.getPlayer() + " " + clickedPiece.getName() + " 在 " + clickedPos);

        Player currentPlayer = game.getCurrentPlayer();
        
        // 检查是否是我的回合
        boolean isMyTurn = false;
        if (currentGameMode == GameMode.ONLINE) {
            if (myPlayer != null && currentPlayer == myPlayer) isMyTurn = true;
        } else if (currentGameMode == GameMode.VS_AI) {
            if (currentPlayer == Player.RED) isMyTurn = true; // 人类是 RED
        }
        
        if (!isMyTurn) {
            System.out.println("点击无效 (非本人回合)");
            clearLocalSelection();
            return;
        }

        // --- 逻辑 1: 点击的是当前玩家的棋子 (选择/切换选择) ---
        if (clickedPiece.getPlayer() == currentPlayer) { // 注意: 这里用 currentPlayer, 而不是 myPlayer
            if (selectedPiecePosition != null) {
                 StackPane oldSelectedNode = getNodeForPosition(selectedPiecePosition);
                 if (oldSelectedNode != null) removeHighlight(oldSelectedNode);
            }
            clearMoveHighlights();
            selectedPiecePosition = clickedPos;
            applyHighlight(clickedNode);
            System.out.println("选中了 " + clickedPiece.getName());

            List<Move> validMoves = game.getAllValidMoves(currentPlayer);
            availableMovePositions.clear();
            for (Move move : validMoves) {
                if (move.from().equals(selectedPiecePosition)) {
                    availableMovePositions.add(move.to());
                }
            }
            showMoveHighlights(availableMovePositions);
        }
        // --- 逻辑 2: 点击的是对方棋子 (尝试吃子) ---
        else if (selectedPiecePosition != null) { 
            Move move = new Move(selectedPiecePosition, clickedPos);
            if (availableMovePositions.contains(move.to())) {
                System.out.println("尝试从 " + selectedPiecePosition + " 移动到 " + clickedPos + " (吃子)");
                clearMoveHighlights();
                tryMove(move);
            } else {
                System.out.println("非法移动目标 (吃子): " + clickedPos);
                clearLocalSelection();
            }
        }
        // --- 逻辑 3: 点击无效 ---
        else {
             System.out.println("点击无效 (未选中棋子, 且点击了对方棋子)");
             clearLocalSelection();
        }
    }

    /**
     * 【修改】处理空位点击 (适配两种模式)
     */
    private void handleEmptySquareClick(Position targetPos) {
        Player currentPlayer = game.getCurrentPlayer();
        boolean isMyTurn = false;
        if (currentGameMode == GameMode.ONLINE) {
            if (myPlayer != null && currentPlayer == myPlayer) isMyTurn = true;
        } else if (currentGameMode == GameMode.VS_AI) {
            if (currentPlayer == Player.RED) isMyTurn = true;
        }

        if (selectedPiecePosition != null && isMyTurn) {
            Move move = new Move(selectedPiecePosition, targetPos);
            if (availableMovePositions.contains(move.to())) {
                System.out.println("尝试从 " + selectedPiecePosition + " 移动到空位 " + targetPos);
                clearMoveHighlights();
                tryMove(move);
            } else {
                System.out.println("非法移动目标 (空位): " + targetPos);
                clearLocalSelection();
            }
        } else {
             System.out.println("点击空位无效 (未选中棋子 或 非本人回合)");
             clearLocalSelection();
        }
    }

    /**
     * 【修改】尝试执行走法 (根据模式分发)
     */
    private void tryMove(Move move) {
        // 统一清除本地选中
        // (在播放动画/发送消息 *之前* 清除, 避免用户在动画期间再次点击)
        clearLocalSelection();
        
        if (currentGameMode == GameMode.ONLINE) {
            // --- 网络模式 ---
            if (serverConnector != null) {
                serverConnector.sendMove(move);
            }
            // 动画和状态更新将由服务器消息触发
        } 
        else if (currentGameMode == GameMode.VS_AI) {
            // --- 人机模式 ---
            // 1. 人类玩家走棋
            boolean success = game.makeMove(move); // 逻辑走棋
            if (!success) {
                 System.out.println("人机模式: 走法失败 (Bug!)");
                 return;
            }
            
            // 2. 播放人类玩家的动画
            StackPane nodeToMove = getNodeForPosition(move.from());
            playAnimationAndRedraw(move, nodeToMove, () -> {
                // 3. 动画结束后, 立即重绘棋盘 (显示吃子结果)
                drawPieces(game.getBoard()); 
                // 4. 检查游戏状态 (是否将军)
                checkGameEndConditions(Player.BLACK); // 检查 AI 是否被将军
                // 5. 如果游戏没结束, 触发 AI 回合
                if (game.getCurrentPlayer() == Player.BLACK) {
                    triggerAiTurn(); // AI 开始思考
                }
            });
        }
    }


    // =========================================================================
    // --- 动画与高亮 (Helper Methods) ---
    // (这部分代码保持不变)
    // =========================================================================

    /** 播放动画并在结束后执行回调 */
    private void playAnimationAndRedraw(Move move, StackPane nodeToMove, Runnable onFinished) {
        if (nodeToMove == null) {
            System.err.println("错误: 找不到要移动的节点! Pos: " + move.from());
            onFinished.run(); 
            return;
        }
        
        double margin = CELL_SIZE / 2.0;
        double radius = CELL_SIZE * 0.4;
        double targetLayoutX = margin + move.to().col() * CELL_SIZE - radius;
        double targetLayoutY = margin + move.to().row() * CELL_SIZE - radius;
        double deltaX = targetLayoutX - nodeToMove.getLayoutX();
        double deltaY = targetLayoutY - nodeToMove.getLayoutY();

        TranslateTransition animation = new TranslateTransition(Duration.millis(250), nodeToMove);
        animation.setToX(deltaX);
        animation.setToY(deltaY);

        animation.setOnFinished(e -> {
            onFinished.run(); 
        });
        animation.play();
    }

    private void applyHighlight(StackPane pieceNode) {
        if (pieceNode != null && pieceNode.getChildren().get(0) instanceof Circle circle) {
            circle.setStroke(Color.GOLD);
            circle.setStrokeWidth(3.0);
        }
    }

    private void removeHighlight(StackPane pieceNode) {
        if (pieceNode != null && pieceNode.getChildren().get(0) instanceof Circle circle) {
             Piece piece = findPieceFromNode(pieceNode);
             if (piece != null) {
                if (piece.getPlayer() == Player.RED) circle.setStroke(Color.DARKRED);
                else circle.setStroke(Color.BLACK);
                circle.setStrokeWidth(2.0);
             } else {
                 circle.setStroke(Color.BLACK);
                 circle.setStrokeWidth(2.0);
             }
        }
    }
    
    private void showMoveHighlights(List<Position> targetPositions) {
        clearMoveHighlights(); 
        double margin = CELL_SIZE / 2.0;
        double radius = CELL_SIZE * 0.15; 
        for (Position pos : targetPositions) {
            double centerX = margin + pos.col() * CELL_SIZE;
            double centerY = margin + pos.row() * CELL_SIZE;
            Circle highlight = new Circle(centerX, centerY, radius);
            highlight.setFill(Color.rgb(50, 200, 50, 0.7)); 
            highlightLayer.getChildren().add(highlight);
        }
    }

    private void clearMoveHighlights() {
        if (highlightLayer != null) {
             highlightLayer.getChildren().clear();
        }
    }

    private void clearLocalSelection() {
        clearMoveHighlights(); 
        if (selectedPiecePosition != null) {
            StackPane oldSelectedNode = getNodeForPosition(selectedPiecePosition);
            if (oldSelectedNode != null) {
                removeHighlight(oldSelectedNode);
            }
        }
        selectedPiecePosition = null;
        availableMovePositions.clear();
    }

    // =========================================================================
    // --- AI 与 网络 (Helper Methods) ---
    // =========================================================================

    /** 检查游戏状态 (适配两种模式) */
    private void checkGameEndConditions(Player playerToCheck) {
        boolean gameOver = false;
        
        if (game.isCheckmate(playerToCheck)) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            String winner = (playerToCheck == Player.RED) ? "BLACK" : "RED";
            System.out.println("      将死! " + winner + " 胜利!");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            pieceLayer.setDisable(true);
            gameOver = true;
        } else if (game.isStalemate(playerToCheck)) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("         逼和! (和棋)");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            pieceLayer.setDisable(true);
            gameOver = true;
        } else if (game.isKingInCheck(playerToCheck)) {
            System.out.println("*******************");
            System.out.println("      将 军 !");
            System.out.println("*******************");
        }
        
        if (!gameOver) {
             System.out.println("轮到: " + game.getCurrentPlayer());
        }
    }

    /** 触发 AI 回合 (仅在人机对战模式) */
    private void triggerAiTurn() {
        if (currentGameMode != GameMode.VS_AI || game.getCurrentPlayer() != Player.BLACK) {
            return; // 安全检查
        }
        
        System.out.println("轮到 AI (BLACK) 思考...");
        pieceLayer.setDisable(true); // 禁用人类输入

        // 在后台线程运行 AI
        new Thread(() -> {
            final int AI_DEPTH = 4;
            long startTime = System.currentTimeMillis();
            if (aiEngine == null) aiEngine = new AIEngine(); 
            
            Move aiMove = aiEngine.findBestMove(game, AI_DEPTH);
            
            long endTime = System.currentTimeMillis();
            System.out.println("AI 思考用时: " + (endTime - startTime) + " 毫秒");

            // 切换回 JavaFX 线程来执行移动和动画
            Platform.runLater(() -> {
                if (aiMove != null) {
                    System.out.println("AI 选择走法: " + aiMove);
                    StackPane aiNodeToMove = getNodeForPosition(aiMove.from());
                    
                    boolean success = game.makeMove(aiMove);
                    if (!success) {
                        System.err.println("AI 走法失败! (Bug)");
                        pieceLayer.setDisable(false); 
                        return;
                    }

                    // 播放 AI 动画, 结束后检查状态并重新启用人类玩家
                    playAnimationAndRedraw(aiMove, aiNodeToMove, () -> {
                        drawPieces(game.getBoard()); 
                        checkGameEndConditions(Player.RED); // 检查人类玩家状态
                        if (!game.isCheckmate(Player.RED) && !game.isStalemate(Player.RED)) {
                             pieceLayer.setDisable(false); // 重新启用人类输入
                             System.out.println("轮到你了 (RED)");
                        }
                    });
                } else {
                     System.err.println("AI 无法找到走法 (游戏应该已结束).");
                     checkGameEndConditions(Player.BLACK);
                }
            });
        }).start(); // 启动后台线程
    }

    /** 【网络版】 当收到服务器的 GameStateMessage 时调用 */
    public void updateGameFromState(GameStateMessage gameState) {
        System.out.println("收到游戏状态更新: 当前玩家 " + gameState.currentPlayer);

        Board newBoard = new Board(); 
        for (int r=0; r<10; r++) for (int c=0; c<9; c++) newBoard.clearPiece(new Position(r,c));
        for (GameStateMessage.SimplePieceInfo pieceInfo : gameState.pieces) {
             Piece piece = createPieceFromName(pieceInfo.name, pieceInfo.player, new Position(pieceInfo.row, pieceInfo.col));
             if (piece != null) newBoard.setPiece(piece);
        }

        // --- 动画逻辑 ---
        Move lastMove = gameState.lastMove;
        StackPane nodeToMove = (lastMove != null) ? getNodeForPosition(lastMove.from()) : null;
        
        if (lastMove != null && nodeToMove != null) {
            System.out.println("DEBUG: 收到带 lastMove 的状态, 准备播放动画: " + lastMove);
            playAnimationAndRedraw(lastMove, nodeToMove, () -> {
                updateBoardAndCheckStatus(newBoard, gameState);
            });
        } else {
             System.out.println("DEBUG: 收到无 lastMove 的状态 (或找不到节点), 立即重绘棋盘");
             updateBoardAndCheckStatus(newBoard, gameState);
        }
    }
    
    /** 【网络版】 更新本地 Game 状态, 重绘棋盘, 并检查结束条件 */
    private void updateBoardAndCheckStatus(Board newBoard, GameStateMessage gameState) {
        this.game.setBoard(newBoard);
        this.game.setCurrentPlayer(gameState.currentPlayer);
        drawPieces(this.game.getBoard());

        Player playerToCheck = this.game.getCurrentPlayer();
        boolean gameOver = false;
        if (gameState.isCheckmate) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            String winner = (playerToCheck == Player.RED) ? "BLACK" : "RED";
            System.out.println("      将死! " + winner + " 胜利!");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            pieceLayer.setDisable(true);
            gameOver = true;
        } else if (gameState.isStalemate) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("         逼和! (和棋)");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            pieceLayer.setDisable(true);
            gameOver = true;
        } else if (gameState.isCheck) {
            System.out.println("*******************");
            System.out.println("      将 军 !");
            System.out.println("*******************");
        }

        if (!gameOver) {
            if (this.game.getCurrentPlayer() == this.myPlayer) {
                pieceLayer.setDisable(false);
                System.out.println("轮到你了 (" + this.myPlayer + ")");
            } else {
                pieceLayer.setDisable(true);
                System.out.println("等待对方 (" + this.game.getCurrentPlayer() + ") 走棋...");
            }
        } else {
            pieceLayer.setDisable(true);
        }
    }

    /** 【辅助】根据名称创建 Piece 对象 */
    private Piece createPieceFromName(String name, Player player, Position position) {
         return switch (name) {
             case "车" -> new Che(player, position);
             case "马" -> new Ma(player, position);
             case "炮" -> new Pao(player, position);
             case "帅", "将" -> new Jiang(player, position);
             case "仕", "士" -> new Shi(player, position);
             case "相", "象" -> new Xiang(player, position);
             case "兵", "卒" -> new Bing(player, position);
             default -> null;
         };
    }

    /** 【辅助】由 ServerConnector 调用, 设置本客户端的角色 */
    public void setMyPlayer(Player player) {
        this.myPlayer = player;
        System.out.println("我的角色被分配为: " + player);
        if (stage != null) {
             stage.setTitle("Xiangqi Game - " + player);
        }
    }

    /** 【辅助】根据位置查找节点 */
    private StackPane getNodeForPosition(Position position) {
        if (position == null || pieceLayer == null) return null;
        for (Node node : pieceLayer.getChildren()) {
            if (node.getUserData() instanceof Position currentPos) {
                if (currentPos.equals(position)) {
                    return (StackPane) node;
                }
            }
        }
        return null;
    }
    
    /** 【辅助】根据节点查找 Piece (用于 removeHighlight) */
     private Piece findPieceFromNode(StackPane node) {
        if (node.getUserData() instanceof Position pos) {
             return game.getBoard().getPiece(pos);
        }
         // Fallback
         double layoutX = node.getLayoutX();
         double layoutY = node.getLayoutY();
         double radius = CELL_SIZE * 0.4;
         double margin = CELL_SIZE / 2.0;
         int approxCol = (int) Math.round((layoutX + radius - margin) / CELL_SIZE);
         int approxRow = (int) Math.round((layoutY + radius - margin) / CELL_SIZE);
         if (approxRow >= 0 && approxRow < BOARD_ROWS && approxCol >=0 && approxCol < BOARD_COLS) {
            Position posFallback = new Position(approxRow, approxCol);
            return game.getBoard().getPiece(posFallback);
         }
         return null;
     }

    public static void main(String[] args) {
        launch(args);
    }
}