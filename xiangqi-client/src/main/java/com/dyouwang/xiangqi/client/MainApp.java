package com.dyouwang.xiangqi.client;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label; // 需要 Label
import javafx.scene.layout.GridPane; // 背景层仍然用 GridPane
import javafx.scene.layout.Pane; // 【改】棋子层用 Pane
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

import com.dyouwang.xiangqi.*;
import com.dyouwang.xiangqi.messages.*;

import java.util.ArrayList;
import java.util.List;


public class MainApp extends Application {

    // --- 常量定义 (不变) ---
    private static final int BOARD_ROWS = 10;
    private static final int BOARD_COLS = 9;
    private static final double CELL_SIZE = 60.0;
    private static final double BOARD_WIDTH = BOARD_COLS * CELL_SIZE;
    private static final double BOARD_HEIGHT = BOARD_ROWS * CELL_SIZE;

    // ... (成员变量) ...
    private Game game;
    private Pane pieceLayer;
    private Pane highlightLayer; // 【新】用于显示合法走法提示
    private AIEngine aiEngine;
    private ServerConnector serverConnector;
    private Player myPlayer = null;
    private Stage stage;

    // --- 【新】用于跟踪用户交互 ---
    private Position selectedPiecePosition = null; // 当前选中的棋子位置, null表示未选中
    private StackPane selectedPieceNode = null;   // 当前选中的棋子图形 (用于高亮)
    private List<Position> availableMovePositions = new ArrayList<>(); // 【新】存储当前选中棋子的合法走法
    
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        game = new Game();
        //aiEngine = new AIEngine();

        serverConnector = new ServerConnector("ws://localhost:8080/game", this); // 把 this (MainApp 实例) 传进去
        serverConnector.connect();
        Board board = game.getBoard();

        // 1. 创建 GridPane (底层, 背景)
        GridPane boardGrid = createBoardGrid();
        boardGrid.setStyle("-fx-background-color: burlywood;");

        // 2. 创建 Canvas (中间层, 线条)
        Canvas canvas = new Canvas(BOARD_WIDTH, BOARD_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawBoardLines(gc); // 使用 margin 绘制

        // 3. 【改】创建 Pane (顶层, 用于手动放置棋子)
        pieceLayer = createPieceLayer(); // 现在返回 Pane
        highlightLayer = createHighlightLayer();
        //drawPieces(board); // <--- 调用新的绘制方法
        
        pieceLayer.setOnMouseClicked(event -> {
            // 只有在选中了棋子的情况下才处理背景点击
            if (selectedPiecePosition != null) {
                // 1. 计算点击位置对应的棋盘坐标 (row, col)
                double clickX = event.getX();
                double clickY = event.getY();
                double margin = CELL_SIZE / 2.0;

                // 反推行列号 (注意需要考虑 margin)
                int col = (int) Math.round((clickX - margin) / CELL_SIZE);
                int row = (int) Math.round((clickY - margin) / CELL_SIZE);
                Position targetPos = new Position(row, col);

                // 2. 检查计算出的坐标是否有效, 并且目标位置确实是空的
                if (targetPos.isValid() && game.getBoard().getPiece(targetPos) == null) {
                    handleEmptySquareClick(targetPos); // 调用新的处理方法
                } else {
                    // 点击在了棋盘外或者有棋子的地方 (棋子点击由棋子自己的事件处理)
                    // 可以选择取消选中
                    // removeHighlight(selectedPieceNode);
                    // selectedPiecePosition = null;
                    // selectedPieceNode = null;
                    // System.out.println("点击无效或已有棋子, 取消选中");
                }
            }
        });
        // --- 背景点击事件结束 ---
        // --- 【修改】使用 StackPane 将四层叠加 ---
        StackPane root = new StackPane();
        // 底层 boardGrid, 然后 canvas, 然后 pieceLayer, 最顶层 highlightLayer
        root.getChildren().addAll(boardGrid, canvas, pieceLayer, highlightLayer);
        root.setAlignment(Pos.CENTER);

        // --- Scene 和 Stage 设置 (不变) ---
        Scene scene = new Scene(root, BOARD_WIDTH, BOARD_HEIGHT);
        stage.setTitle("Xiangqi Game (Manual Positioning)"); // 改标题以区分
        stage.setScene(scene);
        stage.show();
    }

    // --- createBoardGrid() (不变) ---
    private GridPane createBoardGrid() { /* ... 代码不变 ... */
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setMinSize(BOARD_WIDTH, BOARD_HEIGHT);
        gridPane.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        gridPane.setMaxSize(BOARD_WIDTH, BOARD_HEIGHT);
        return gridPane;
    }

    // --- drawBoardLines() (不变 - 使用 margin 的版本) ---
    private void drawBoardLines(GraphicsContext gc) { /* ... 代码不变 ... */
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

    /**
     * 【改】创建用于放置棋子的 Pane 层
     */
    private Pane createPieceLayer() {
        Pane pane = new Pane(); // 直接创建 Pane
        pane.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT); // 确保大小一致
        // Pane 默认背景透明
        return pane;
    }

    /**
     * 【手动定位版】根据 Board 状态绘制棋子 Circle+Label
     */
    private void drawPieces(Board board) {
        pieceLayer.getChildren().clear(); // 清空

        // Canvas 绘制线条时使用的边距
        double margin = CELL_SIZE / 2.0;

        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {
                // 2. 【确认】currentPos 在这里声明 (在 if 语句 *外部*)
                Position currentPos = new Position(row, col);
                Piece piece = board.getPiece(new Position(row, col));

                if (piece != null) {
                    // 1. 创建 Circle
                    double radius = CELL_SIZE * 0.4;
                    Circle pieceCircle = new Circle(radius);
                    // ... 设置颜色和边框 (代码同前) ...
                    if (piece.getPlayer() == Player.RED) {
                        pieceCircle.setFill(Color.rgb(255, 90, 90));
                        pieceCircle.setStroke(Color.DARKRED);
                    } else {
                        pieceCircle.setFill(Color.rgb(90, 90, 90));
                        pieceCircle.setStroke(Color.BLACK);
                    }
                    pieceCircle.setStrokeWidth(2.0);
                    pieceCircle.setStrokeType(StrokeType.INSIDE);

                    // 2. 创建 Label
                    Label pieceLabel = new Label(piece.getName());
                    // ... 设置字体和颜色 (代码同前) ...
                    pieceLabel.setFont(Font.font("System", FontWeight.BOLD, radius * 1.2));
                    pieceLabel.setTextFill(Color.WHITE);

                    // 3. 用 StackPane 组合 Circle 和 Label
                    StackPane piecePane = new StackPane();
                    piecePane.getChildren().addAll(pieceCircle, pieceLabel);
                    // StackPane 会自动把 Label 放在 Circle 中心

                    // 4. 【核心】计算棋子在 Pane 中的绝对像素坐标 (x, y)
                    //    目标是让 piecePane 的中心点对准 Canvas 上的交叉点
                    double targetX = margin + col * CELL_SIZE; // 交叉点的 X 坐标
                    double targetY = margin + row * CELL_SIZE; // 交叉点的 Y 坐标

                    // 5. 【核心】设置 piecePane 的左上角坐标
                    //    因为 StackPane 默认大小是其内容的大小 (即 Circle 的直径 2*radius),
                    //    要使其中心在 (targetX, targetY), 左上角需要偏移 -radius
                    piecePane.setLayoutX(targetX - radius);
                    piecePane.setLayoutY(targetY - radius);

                    piecePane.setUserData(currentPos);
                    // --- 【新】添加鼠标点击事件监听器 ---
                    piecePane.setOnMouseClicked(event -> {
                        handlePieceClick(currentPos, piece, piecePane); // 调用处理方法
                    });
                    // --- 事件监听器结束 ---

                    // 6. 将 piecePane 添加到 Pane 中
                    pieceLayer.getChildren().add(piecePane);
                } else {

                }
            }
        }
    }

    // ... (drawPieces 方法结束) ...

    /**
     * 【新方法】 处理棋子点击事件
     * @param clickedPos  被点击的棋子的位置
     * @param clickedPiece 被点击的棋子对象
     * @param clickedNode  被点击的棋子的图形 (StackPane)
     */
    /**
     * 【修改版】 处理棋子点击事件
     */
    private void handlePieceClick(Position clickedPos, Piece clickedPiece, StackPane clickedNode) {
        System.out.println("点击了: " + clickedPiece.getPlayer() + " " + clickedPiece.getName() + " 在 " + clickedPos);

        // --- 逻辑 1: 点击的是当前玩家的棋子 ---
        if (clickedPiece.getPlayer() == game.getCurrentPlayer()) {
            if (selectedPieceNode != null) {
                removeHighlight(selectedPieceNode);
            }
            // 清除之前的走法提示
            clearMoveHighlights();

            selectedPiecePosition = clickedPos;
            selectedPieceNode = clickedNode;
            applyHighlight(selectedPieceNode);
            System.out.println("选中了 " + clickedPiece.getName());

            // --- 【新】获取并显示合法走法 ---
            List<Move> validMoves = game.getAllValidMoves(game.getCurrentPlayer()); // 获取当前玩家所有棋子的所有合法走法
            availableMovePositions.clear(); // 清空旧列表
            // 筛选出 *当前选中棋子* 的走法
            for (Move move : validMoves) {
                if (move.from().equals(selectedPiecePosition)) {
                    availableMovePositions.add(move.to()); // 只记录目标位置
                }
            }
            showMoveHighlights(availableMovePositions); // 调用新方法显示提示
            // --- 显示结束 ---

        }
        // --- 逻辑 2: 点击的是对方棋子 (尝试吃子) ---
        else if (selectedPiecePosition != null) {
            System.out.println("尝试从 " + selectedPiecePosition + " 移动到 " + clickedPos + " (吃子)");
            Move move = new Move(selectedPiecePosition, clickedPos);
            // 【修改】在尝试移动前清除提示
            clearMoveHighlights();
            tryMove(move);
        }
        // --- 逻辑 3: 点击无效 ---
        else {
             System.out.println("点击无效 (非当前玩家棋子, 或未选中棋子)");
             // 【修改】如果点击无效，也清除提示和选中
             if (selectedPieceNode != null) {
                 removeHighlight(selectedPieceNode);
                 selectedPieceNode = null;
             }
             selectedPiecePosition = null;
             clearMoveHighlights();
        }
    }

    /**
     * 【新方法】 应用高亮效果 (简单示例: 改变边框颜色)
     */
    private void applyHighlight(StackPane pieceNode) {
        if (pieceNode != null && pieceNode.getChildren().get(0) instanceof Circle circle) {
            circle.setStroke(Color.GOLD); // 用金色边框表示选中
            circle.setStrokeWidth(3.0);
        }
    }

    /**
     * 【新方法】 移除高亮效果 (恢复原始边框颜色)
     */
    private void removeHighlight(StackPane pieceNode) {
        if (pieceNode != null && pieceNode.getChildren().get(0) instanceof Circle circle) {
            // 我们需要知道这个棋子是红方还是黑方来恢复颜色
            // 这有点麻烦, 更好的方法是记录原始颜色, 或者使用 CSS 样式
            // 简单起见, 我们暂时都恢复成黑色边框
            // (找到 piece 对象会更好)

            // 临时的简单恢复:
             Piece piece = findPieceFromNode(pieceNode); // 需要实现这个辅助方法
             if (piece != null) {
                if (piece.getPlayer() == Player.RED) {
                    circle.setStroke(Color.DARKRED);
                } else {
                    circle.setStroke(Color.BLACK);
                }
                circle.setStrokeWidth(2.0);
             } else {
                 // 默认恢复
                 circle.setStroke(Color.BLACK);
                 circle.setStrokeWidth(2.0);
             }
        }
    }

    /**
     * 【新 - 辅助方法】 根据图形节点找到 Piece 对象 (效率不高, 仅示例)
     * 更好的方法是在创建 piecePane 时将其与 Piece 关联 (例如使用 UserData)
     */
     private Piece findPieceFromNode(StackPane node) {
         // 从 pieceLayer 中找到 node 对应的行列
         Integer colIndex = GridPane.getColumnIndex(node);
         Integer rowIndex = GridPane.getRowIndex(node); // 注意: GridPane 已弃用, 但 pieceLayer 之前是 GridPane...
         // 啊, pieceLayer 现在是 Pane 了! 我们需要不同的方法

         // 如果 pieceLayer 是 Pane, 我们需要从 LayoutX/Y 反推
         if (colIndex == null || rowIndex == null) {
             // 尝试从 LayoutX/Y 反推 (不精确)
             double layoutX = node.getLayoutX();
             double layoutY = node.getLayoutY();
             double radius = CELL_SIZE * 0.4;
             double margin = CELL_SIZE / 2.0;
             // targetX = margin + col * CELL_SIZE; -> col = (layoutX + radius - margin) / CELL_SIZE
             // targetY = margin + row * CELL_SIZE; -> row = (layoutY + radius - margin) / CELL_SIZE
             int approxCol = (int) Math.round((layoutX + radius - margin) / CELL_SIZE);
             int approxRow = (int) Math.round((layoutY + radius - margin) / CELL_SIZE);

             if (approxRow >= 0 && approxRow < BOARD_ROWS && approxCol >=0 && approxCol < BOARD_COLS) {
                Position pos = new Position(approxRow, approxCol);
                return game.getBoard().getPiece(pos);
             }
             return null; // 无法反推
         }

         // 如果 pieceLayer 是 GridPane (之前的代码)
         // Position pos = new Position(rowIndex, colIndex);
         // return game.getBoard().getPiece(pos);
         return null; // 应该不会到这里
    }

    // ... (在 findPieceFromNode 方法后面, main 方法前面) ...

    /**
     * 【新方法】 处理空位点击事件 (尝试移动)
     * @param targetPos 被点击的空位的位置
     */
    private void handleEmptySquareClick(Position targetPos) {
        // 只有在选中了棋子的情况下才处理
        if (selectedPiecePosition != null) {
            System.out.println("尝试从 " + selectedPiecePosition + " 移动到空位 " + targetPos);
            Move move = new Move(selectedPiecePosition, targetPos);
            clearMoveHighlights();
            tryMove(move); // 调用统一的移动处理方法
        } else {
            System.out.println("点击空位无效 (未选中棋子)");
            clearMoveHighlights();
        }
    }


    /**
     * 【新方法】 创建用于显示合法走法提示的 Pane 层
     */
    private Pane createHighlightLayer() {
        Pane pane = new Pane();
        pane.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        pane.setStyle("-fx-background-color: transparent;");
        // 【重要】禁用鼠标事件, 这样点击才能穿透到下面的 pieceLayer
        pane.setMouseTransparent(true);
        return pane;
    }

    // ... (在 findPieceFromNode 方法后面, main 方法前面) ...

    /**
     * 【新方法】 在 highlightLayer 上绘制合法走法提示 (例如: 半透明小圆点)
     * @param targetPositions 可以移动到的目标位置列表
     */
    private void showMoveHighlights(List<Position> targetPositions) {
        clearMoveHighlights(); // 先清除旧的

        double margin = CELL_SIZE / 2.0;
        double highlightRadius = CELL_SIZE * 0.15; // 提示圆点的大小

        for (Position pos : targetPositions) {
            double centerX = margin + pos.col() * CELL_SIZE;
            double centerY = margin + pos.row() * CELL_SIZE;

            Circle highlight = new Circle(centerX, centerY, highlightRadius);
            // 设置颜色和透明度
            highlight.setFill(Color.rgb(50, 200, 50, 0.7)); // 绿色, 70% 透明度
            // (可选) 添加边框
            // highlight.setStroke(Color.DARKGREEN);
            // highlight.setStrokeWidth(1);

            highlightLayer.getChildren().add(highlight);
        }
    }

    /**
     * 【新方法】 清除 highlightLayer 上的所有提示标记
     */
    private void clearMoveHighlights() {
        highlightLayer.getChildren().clear();
    }

    /**
     * 【动画版】 尝试执行走法并更新界面
     * @param move 要尝试的走法
     */
    // private void tryMove(Move move) {
    //     // --- 检查走法是否理论上可行 (但不真正执行) ---
    //     // 我们需要先获取所有合法走法来验证, Game.makeMove 会做这个验证
    //     // 为了避免重复计算, 我们先假设 move 合法, 让 game.makeMove 验证

    //     // 1. 【注意】先保存当前选中的节点, 因为 game.makeMove 会改变状态
    //     final StackPane nodeToMove = selectedPieceNode;
    //     final Position fromPos = selectedPiecePosition; // 起始位置

    //     // 2. 调用 Game 核心逻辑尝试走棋 (这会更新 board 和 currentPlayer)
    //     boolean success = game.makeMove(move);

    //     // 3. 如果走法成功
    //     if (success && nodeToMove != null) {
    //         System.out.println("走法成功! 轮到 " + game.getCurrentPlayer());

    //         // 4. 计算目标像素位置
    //         double margin = CELL_SIZE / 2.0;
    //         double radius = CELL_SIZE * 0.4;
    //         double targetLayoutX = margin + move.to().col() * CELL_SIZE - radius;
    //         double targetLayoutY = margin + move.to().row() * CELL_SIZE - radius;

    //         // 5. 创建平移动画
    //         TranslateTransition animation = new TranslateTransition(Duration.millis(250), nodeToMove); // 动画时长 250ms
            
    //         // 【重要】TranslateTransition 是 *相对* 偏移
    //         // 我们需要计算从当前 LayoutX/Y 到目标 LayoutX/Y 的 *差值*
    //         // 但是, 我们用的是 Pane 布局, nodeToMove 的 TranslateX/Y 默认是 0
    //         // 我们应该直接设置最终的 TranslateX/Y (相对于其原始 LayoutX/Y)
    //         // 不对, TranslateTransition 的 setToX/Y 就是设置最终的 *Translate* 值
            
    //         // 我们需要移动的距离 = 目标 Layout - 当前 Layout
    //         double deltaX = targetLayoutX - nodeToMove.getLayoutX();
    //         double deltaY = targetLayoutY - nodeToMove.getLayoutY();
            
    //         animation.setToX(deltaX);
    //         animation.setToY(deltaY);


    //         // 6. 【关键】设置动画结束事件
    //         animation.setOnFinished(e -> {
    //             // a. 动画结束, 此时棋子在视觉上已到达新位置
    //             // b. 【重要】现在才使用 game.getBoard() (已经是走棋后的状态) 重新绘制棋盘
    //             //    这会正确处理吃子 (被吃的棋子消失), 并将移动的棋子放在最终的正确位置
    //             drawPieces(game.getBoard()); 
                
    //             // c. 检查游戏结束条件
    //             checkGameEndConditions();

    //             // d. 清除选中状态
    //             selectedPiecePosition = null;
    //             selectedPieceNode = null;
    //         });

    //         // 7. 清除走法提示 (在动画开始前)
    //         clearMoveHighlights();
            
    //         // 8. 播放动画
    //         animation.play();

    //         // 注意: 我们 *不* 在这里清除 selectedPiecePosition / Node
    //         // 也不在这里检查游戏结束, 这些都推迟到动画结束后执行

    //     }
    //     // 9. 如果走法失败
    //     else {
    //         System.out.println("走法失败, 请重试.");
    //         // 清除选中和提示
    //         clearMoveHighlights();
    //         if(selectedPieceNode != null) {
    //             removeHighlight(selectedPieceNode);
    //         }
    //         selectedPiecePosition = null;
    //         selectedPieceNode = null;
    //     }
    // }

    /**
     * 【网络版】 尝试将走法发送给服务器
     */
    private void tryMove(Move move) {
        System.out.println("DEBUG: Entering tryMove for move: " + move);
        // 检查是否轮到当前玩家走棋 (客户端也做一次校验)
        // (需要知道自己是哪个玩家, 服务器连接成功后应该告诉我们)
// --- 【修改】使用 myPlayer 变量进行检查 ---
        if (this.myPlayer == null) {
            System.out.println("错误: 角色尚未分配!");
            return;
        }
        if (game.getCurrentPlayer() != this.myPlayer) {
             System.out.println("还未轮到你 (" + this.myPlayer + ") 走棋! 当前轮到: " + game.getCurrentPlayer());
             // 清除选中和提示
             // ... (清除代码不变) ...
             return;
        }
        // --- 回合检查结束 ---

        // 【修改】不再调用 game.makeMove, 而是发送消息
        if (serverConnector != null) {
            serverConnector.sendMove(move);
        }

        // 清除本地的选中和提示 (无论服务器是否接受, 先清除)
        clearMoveHighlights();
        if (selectedPieceNode != null) {
            removeHighlight(selectedPieceNode);
        }
        selectedPiecePosition = null;
        selectedPieceNode = null;

        // 【删除】动画逻辑移到接收服务器状态更新后处理
        // 【删除】本地 checkGameEndConditions 逻辑
    }

    /**
     * 【修改版】 检查游戏结束条件, 并在轮到 AI 时触发 AI 回合
     */
    private void checkGameEndConditions() {
        Player nextPlayer = game.getCurrentPlayer();
        boolean gameOver = false;

        if (game.isCheckmate(nextPlayer)) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("      将死! " + (nextPlayer == Player.RED ? Player.BLACK : Player.RED) + " 胜利!");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            pieceLayer.setDisable(true); // 禁用棋盘
            gameOver = true;
        } else if (game.isStalemate(nextPlayer)) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("         逼和! (和棋)");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            pieceLayer.setDisable(true);
            gameOver = true;
        } else if (game.isKingInCheck(nextPlayer)) {
            System.out.println("*******************");
            System.out.println("      将 军 !");
            System.out.println("*******************");
        }

        // --- 【新】如果游戏未结束, 并且轮到 AI (黑方) ---
        if (!gameOver && nextPlayer == Player.BLACK) {
            // 触发 AI 思考和走棋
            triggerAiTurn();
        }
        // --- AI 触发结束 ---
        else if (!gameOver && nextPlayer == Player.RED) {
             // 如果轮到人类玩家, 确保棋盘是可点击的
             pieceLayer.setDisable(false);
             System.out.println("轮到你了 (RED)");
        }
    }

    /**
     * 【新方法】 触发 AI 思考并执行走法 (在 JavaFX 线程上运行)
     */
    private void triggerAiTurn() {
        System.out.println("轮到 AI (BLACK) 思考...");
        // 暂时禁用用户输入, 防止在 AI 思考或动画期间点击
        pieceLayer.setDisable(true);

        // --- AI 思考 (会冻结 GUI) ---
        // (我们暂时不使用后台线程)
        final int AI_DEPTH = 3; // AI 思考深度
        long startTime = System.currentTimeMillis();
        Move aiMove = aiEngine.findBestMove(game, AI_DEPTH);
        long endTime = System.currentTimeMillis();
        System.out.println("AI 思考用时: " + (endTime - startTime) + " 毫秒");
        // --- AI 思考结束 ---

        if (aiMove != null) {
            System.out.println("AI 选择走法: " + aiMove);

            // 获取要移动的棋子图形节点
            StackPane aiNodeToMove = getNodeForPosition(aiMove.from());

            // 1. 先在逻辑上执行移动 (这会更新 board 和 currentPlayer 到 RED)
            boolean success = game.makeMove(aiMove); // 使用 game.makeMove 来处理吃子打印等

            if (success && aiNodeToMove != null) {
                // 2. 计算动画目标位置
                double margin = CELL_SIZE / 2.0;
                double radius = CELL_SIZE * 0.4;
                double targetLayoutX = margin + aiMove.to().col() * CELL_SIZE - radius;
                double targetLayoutY = margin + aiMove.to().row() * CELL_SIZE - radius;
                double deltaX = targetLayoutX - aiNodeToMove.getLayoutX();
                double deltaY = targetLayoutY - aiNodeToMove.getLayoutY();

                // 3. 创建并播放 AI 移动的动画
                TranslateTransition aiAnimation = new TranslateTransition(Duration.millis(250), aiNodeToMove);
                aiAnimation.setToX(deltaX);
                aiAnimation.setToY(deltaY);

                // 4. AI 动画结束后
                aiAnimation.setOnFinished(e -> {
                    // a. 重新绘制棋盘以反映 AI 移动和可能的吃子
                    drawPieces(game.getBoard());
                    // b. 再次检查游戏状态 (这次是检查 AI 走完后人类玩家的状态)
                    checkGameEndConditions(); // 这个调用现在会检查 RED 的状态
                    // c. 在 checkGameEndConditions 内部, 如果轮到 RED, 会自动 re-enable pieceLayer
                });
                aiAnimation.play();

            } else {
                 System.err.println("AI 走法失败或找不到节点? Move: " + aiMove + ", Node: " + aiNodeToMove);
                 // 如果 AI 走棋失败 (理论上不应发生), 重新启用输入
                 pieceLayer.setDisable(false);
            }

        } else {
            // AI 无法找到走法 (将死或逼和), checkGameEndConditions 应该已经处理了
            System.err.println("AI 无法找到走法 (游戏应该已结束).");
            // (安全起见)
             pieceLayer.setDisable(false);
        }
    }

    // ... (其他方法 tryMove, handlePieceClick 等保持不变) ...
    // ... (main 方法) ...

        /**
     * 【新辅助方法】 根据棋盘位置查找对应的棋子图形节点 (StackPane)
     * @param position 要查找的位置
     * @return 对应的 StackPane, 如果该位置为空或未找到则返回 null
     */
    private StackPane getNodeForPosition(Position position) {
        if (position == null) return null;
        // 遍历 pieceLayer 上的所有子节点 (棋子图形)
        for (javafx.scene.Node node : pieceLayer.getChildren()) {
            if (node.getUserData() instanceof Position currentPos) {
                // 检查存储在 UserData 中的位置是否匹配
                if (currentPos.equals(position)) {
                    return (StackPane) node;
                }
            }
        }
        return null; // 没有找到
    }

    // ... (在 main 方法上面) ...

    /**
     * 【修改版】 当收到服务器的 GameStateMessage 时调用
     */
    public void updateGameFromState(GameStateMessage gameState) {
        System.out.println("收到游戏状态更新: 当前玩家 " + gameState.currentPlayer);

        // 1. 【修改】根据消息内容构建新的 Board 对象
        Board newBoard = new Board(); // 创建一个空的 Board
        for (int r=0; r<10; r++) for (int c=0; c<9; c++) newBoard.clearPiece(new Position(r,c)); // 确保清空
        for (GameStateMessage.SimplePieceInfo pieceInfo : gameState.pieces) {
             Piece piece = createPieceFromName(pieceInfo.name, pieceInfo.player, new Position(pieceInfo.row, pieceInfo.col));
             if (piece != null) {
                 newBoard.setPiece(piece);
             }
        }

        // 2. 【修改】更新客户端本地 Game 对象的状态
        this.game.setBoard(newBoard);           // 更新棋盘
        this.game.setCurrentPlayer(gameState.currentPlayer); // 更新当前玩家

        // 3. 重新绘制棋子层 (使用更新后的 game.getBoard())
        drawPieces(this.game.getBoard());

        // 4. 检查游戏结束或将军状态 (现在可以使用本地 game 对象了)
        Player nextPlayer = this.game.getCurrentPlayer(); // 使用本地 game 对象
        boolean gameOver = false;
        // (检查 isCheckmate, isStalemate, isKingInCheck 的逻辑不变)
        if (this.game.isCheckmate(nextPlayer)) { // 使用本地 game 对象
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("      将死! " + (nextPlayer == Player.RED ? Player.BLACK : Player.RED) + " 胜利!");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            pieceLayer.setDisable(true);
            gameOver = true;
        } else if (this.game.isStalemate(nextPlayer)) { // 使用本地 game 对象
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("         逼和! (和棋)");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            pieceLayer.setDisable(true);
            gameOver = true;
        } else if (this.game.isKingInCheck(nextPlayer)) { // 使用本地 game 对象
            System.out.println("*******************");
            System.out.println("      将 军 !");
            System.out.println("*******************");
        }


        // 5. 根据当前玩家启用/禁用输入
        if (!gameOver) {
            if (this.game.getCurrentPlayer() == this.myPlayer) {
                pieceLayer.setDisable(false); // 轮到我, 启用
                System.out.println("轮到你了 (" + this.myPlayer + ")");
            } else {
                pieceLayer.setDisable(true); // 轮到对方, 禁用
                System.out.println("等待对方 (" + this.game.getCurrentPlayer() + ") 走棋...");
            }
        } else {
            pieceLayer.setDisable(true); // 游戏结束, 禁用
        }
    }

    /**
     * 【新辅助方法】 根据名称创建 Piece 对象 (不完整示例)
     */
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
        // 需要 import 所有 Piece 子类
    }

    // ... (在 main 方法上面) ...

    /**
     * 【新方法】 由 ServerConnector 调用, 设置本客户端的角色
     */
    public void setMyPlayer(Player player) {
        this.myPlayer = player;
        System.out.println("我的角色被分配为: " + player);
        // (可选) 更新窗口标题显示角色
        if (stage != null) { // 确保 stage 已初始化
            stage.setTitle("Xiangqi Game - " + player);
        }
    }
    // 需要将 Stage stage 提升为成员变量才能在 setMyPlayer 中访问
    // 在 MainApp 类顶部添加:
    // private Stage stage;
    // 在 start 方法开头添加:
    // this.stage = stage;

    // ... (main 方法) ...

    public static void main(String[] args) {
        launch(args);
    }
}