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

import com.dyouwang.xiangqi.Board;
import com.dyouwang.xiangqi.Game;
import com.dyouwang.xiangqi.Piece;
import com.dyouwang.xiangqi.Player;
import com.dyouwang.xiangqi.Position;
import com.dyouwang.xiangqi.Move;

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

    // --- 【新】用于跟踪用户交互 ---
    private Position selectedPiecePosition = null; // 当前选中的棋子位置, null表示未选中
    private StackPane selectedPieceNode = null;   // 当前选中的棋子图形 (用于高亮)
    private List<Position> availableMovePositions = new ArrayList<>(); // 【新】存储当前选中棋子的合法走法
    
    @Override
    public void start(Stage stage) {
        game = new Game();
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
        drawPieces(board); // <--- 调用新的绘制方法
        
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
                 // 1. 【确认】final 变量在这里声明
                final int finalRow = row;
                final int finalCol = col;

                // 2. 【确认】currentPos 在这里声明 (在 if 语句 *外部*)
                Position currentPos = new Position(finalRow, finalCol);
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
     * 【新方法】 尝试执行走法并更新界面
     * @param move 要尝试的走法
     */
    private void tryMove(Move move) {
        // 1. 调用 Game 核心逻辑尝试走棋
        boolean success = game.makeMove(move);

        // 2. 如果走法成功
        if (success) {
            System.out.println("走法成功! 轮到 " + game.getCurrentPlayer());

            // 3. 重新绘制整个棋盘
            drawPieces(game.getBoard()); // 使用更新后的 board

            // 4. 检查是否将死或逼和 (游戏结束)
            Player nextPlayer = game.getCurrentPlayer();
            if (game.isCheckmate(nextPlayer)) {
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("      将死! " + (nextPlayer == Player.RED ? Player.BLACK : Player.RED) + " 胜利!");
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                // TODO: 显示游戏结束画面, 禁用棋盘点击
                pieceLayer.setDisable(true); // 简单禁用
            } else if (game.isStalemate(nextPlayer)) {
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("         逼和! (和棋)");
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                pieceLayer.setDisable(true); // 简单禁用
            }
            // 5. 如果游戏未结束, 检查是否将军了对方
            else if (game.isKingInCheck(nextPlayer)) {
                System.out.println("*******************");
                System.out.println("      将 军 !");
                System.out.println("*******************");
            }

            // 6. 清除选中状态 (因为已经走完了)
            selectedPiecePosition = null;
            selectedPieceNode = null; // 高亮会自动在 redraw 时消失

        }
        // 7. 如果走法失败 (Game 核心逻辑会打印原因)
        else {
            System.out.println("走法失败, 请重试.");
            // (可以选择取消选中, 或让用户重新选择目标)
            // removeHighlight(selectedPieceNode);
            // selectedPiecePosition = null;
            // selectedPieceNode = null;
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

    // ... (main 方法) ...

    public static void main(String[] args) {
        launch(args);
    }
}