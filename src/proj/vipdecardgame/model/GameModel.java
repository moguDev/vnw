package proj.vipdecardgame.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class GameModel implements Serializable {
	private static final long serialVersionUID = 1L;

	// チャージオアドローアイテム
	public static class ChargeOrDrawItem {
		public static final int SKIP = 0, DRAW2 = 1, DRAW1_WEED1 = 2,
				WEED2 = 3, DRAW1 = 4;
	}

	// タイムアイテム
	public static class TimeItem {
		public static final int UP = 0, DRAW = 1, MAIN = 2, ACTION = 3,
				END = 4, COUNTER = 5, OPPONENT_UP = 6, OPPONENT_DRAW = 7,
				OPPONENT_MAIN = 8, OPPONENT_ACTION = 9, OPPONENT_END = 10,
				OPPONENT_COUNTER = 11, OPPONENT_COUNTER_END = 12;
	}

	// フィールドのサイズ
	public static final int BOARD_SIZE = 5;
	// 召喚可能範囲、発動可能範囲文字列
	public static final String SUMMONABLE_RANGE = "0000000000000000000011111";
	public static final String MAGIC_PLAYABLE_RANGE = "0000000000111111111111111";
	// アクティビティ
	Context context;
	// 現在タイム
	private int time;
	// ターンカウント
	private int turnCnt = 0;
	public boolean first = true;
	// プレイかうんと
	private int playCnt = 1;
	// 召喚可能範囲、マジック発動可能範囲
	public int[][] summonableRange, magicPlayableRange;
	// プレイヤー
	private Player me, opponent;
	// ボード
	private Card[][] board;
	// 効果処理クラス
	private EffectExecuter effectExecuter;
	// ログ
	List<String> logList;

	// コンストラクタ
	public GameModel(Context context, int playerId) {
		switch (playerId) {
		case 1:
			this.first = true;
			break;
		case 2:
			this.first = false;
			break;
		}
		this.createNewBoard();
		// 効果処理クラスの初期化
		this.effectExecuter = new EffectExecuter(this);
		// 　ログの初期化
		logList = new ArrayList<String>();
		logList.add("対戦を開始しました。");
		// 召喚可能範囲、マジック発動可能範囲の初期化
		this.summonableRange = rangeStringToIntArray(SUMMONABLE_RANGE);
		this.magicPlayableRange = rangeStringToIntArray(MAGIC_PLAYABLE_RANGE);
	}

	// board の初期化メソッド
	private void createNewBoard() {
		this.board = new Card[BOARD_SIZE][BOARD_SIZE];
	}

	// me セッター
	public void setMe(Player p) {
		this.me = p;
	}

	// me セッター
	public void setOpponent(Player p) {
		this.opponent = p;
	}

	// me, opponent ゲッター
	public Player getPlayer(int pNum) {
		switch (pNum) {
		case Player.ME:
			return this.me;
		case Player.OPPONENT:
			return this.opponent;
		}
		return null;
	}

	// 引数からもう一人のプレイヤーを返す
	public Player getAnotherPlayer(int pNum) {
		switch (pNum) {
		case Player.ME:
			return this.opponent;
		case Player.OPPONENT:
			return this.me;
		}
		return null;
	}

	// 引数からもう一人のプレイヤーを返す
	public Player getAnotherPlayer(Player p) {
		if (p.equals(me))
			return this.opponent;
		else
			return this.me;
	}

	public Card getCard(int row, int column) {
		return this.board[row][column];
	}

	public int getRowFromPlayId(int pId) {
		for (int i = 0; i < BOARD_SIZE; i++)
			for (int j = 0; j < BOARD_SIZE; j++)
				if (!this.isBlankCell(i, j))
					if (this.board[i][j].pId == pId) {
						return i;
					}
		return -1;
	}

	public int getColumnFromPlayId(int pId) {
		for (int i = 0; i < BOARD_SIZE; i++)
			for (int j = 0; j < BOARD_SIZE; j++)
				if (!this.isBlankCell(i, j))
					if (this.board[i][j].pId == pId)
						return j;
		return -1;
	}

	// ターン数ゲッター
	public int getTurnCount() {
		return this.turnCnt;
	}

	// 現在タイムゲッター
	public int getTime() {
		return this.time;
	}

	// ログゲッター
	public String getLog() {
		return this.logList.get(this.logList.size() - 1);
	}

	// アップタイムへ
	public void toUptime() {
		if (this.first)
			this.turnCnt++;
		this.time = TimeItem.UP;
		this.untapBoardMonster(Player.ME);
		logList.add("ボード上のモンスターをアンタップしました。");
	}

	// ドロータイムへ
	public void toDrawtime() {
		this.time = TimeItem.DRAW;
	}

	// メインタイムへ
	public void toMaintime() {
		this.time = TimeItem.MAIN;
	}

	// アクションタイムへ
	public void toActiontime() {
		this.time = TimeItem.ACTION;
		this.removeMagicFromBoard();
	}

	// エンドタイムへ
	public void toEndtime() {
		this.time = TimeItem.END;
	}

	// カウンタータイムへ
	public void toCountertime() {
		this.time = TimeItem.COUNTER;
	}

	// 相手アップタイムへ
	public void toOpponentUptime() {
		if (!this.first)
			this.turnCnt++;
		this.time = TimeItem.OPPONENT_UP;
		this.untapBoardMonster(Player.OPPONENT);
		logList.add("相手のボード上のモンスターをアンタップしました。");
	}

	// 相手ドロータイムへ
	public void toOpponentDrawtime() {
		this.time = TimeItem.OPPONENT_DRAW;
	}

	// 相手メインタイムへ
	public void toOpponentMaintime() {
		this.time = TimeItem.OPPONENT_MAIN;
	}

	// 相手アクションタイムへ
	public void toOpponentActiontime() {
		this.time = TimeItem.OPPONENT_ACTION;
		this.removeMagicFromBoard();
	}

	// 相手アクションタイムへ
	public void toOpponentEndtime() {
		this.time = TimeItem.OPPONENT_END;
		// アップタイムへ
		this.toUptime();
	}

	// 相手カウンタータイムへ
	public void toOpponentCountertime() {
		this.time = TimeItem.OPPONENT_COUNTER;
	}

	// 相手カウンタータイム終了へ
	public void toOpponentCounterEndtime() {
		this.time = TimeItem.OPPONENT_COUNTER_END;
		logList.add("相手がカウンタータイムを終了しました。");
	}

	// 空いているマスか判定
	public boolean isBlankCell(int row, int column) {
		if (this.board[row][column] == null)
			return true;
		return false;
	}

	// 引数のプレイヤーのモンスターをアンタップ
	public void untapBoardMonster(int pNum) {
		Player p = this.getPlayer(pNum);
		for (int i = 0; i < BOARD_SIZE; i++)
			for (int j = 0; j < BOARD_SIZE; j++)
				if (!this.isBlankCell(i, j))
					if (board[i][j].controllable(p))
						board[i][j].tapped = false;
	}

	// 指定マスのカードを取り除く
	public void removeCard(int row, int column) {
		board[row][column] = null;
	}

	// 場から墓地へ
	public void fromBoardToGrave(int row, int column) {
		// 指定カードを取得
		Card card = this.board[row][column];
		// 指定カードのコントローラの墓地にカードを加える
		card.controller.addGrave(card);
		// 場からカードを取り除く
		this.removeCard(row, column);
	}

	public void removeMagicFromBoard() {
		for (int i = 0; i < BOARD_SIZE; i++)
			for (int j = 0; j < BOARD_SIZE; j++)
				if (!this.isBlankCell(i, j))
					if (!board[i][j].isMonster())
						this.fromBoardToGrave(i, j);
	}

	public void draw(int pNum, int val) {
		Player p = this.getPlayer(pNum);
		this.draw(p, val);
	}

	public void draw(Player p, int val) {
		String string;
		if (p.equals(me))
			string = "";
		else
			string = "相手が";
		// 選択されたアイテムから場合分け
		switch (val) {
		// カードと２枚ドロー
		case ChargeOrDrawItem.DRAW2:
			p.drawCard(2);
			logList.add(string + "カードを２枚ドローしました。");
			break;
		// カードを1枚ドロー + 草を1つ生やす
		case ChargeOrDrawItem.DRAW1_WEED1:
			p.drawCard(1);
			p.addWeed(1);
			logList.add(string + "草を１つ生やし、カードを１枚ドローしました。");
			break;
		// 草を2つ生やす
		case ChargeOrDrawItem.WEED2:
			p.addWeed(2);
			logList.add(string + "草を２つ生やしました。");
			break;
		case ChargeOrDrawItem.DRAW1:
			p.drawCard(1);
			logList.add(string + "カードを１枚ドローしました。");
			break;
		}
	}

	// 場にカードをプレイ
	public void playCard(int pNum, String coordinate, int handPosition) {
		int row = GameModel.rowCodeToNum(coordinate.substring(0, 1));
		int column = Integer.parseInt(coordinate.substring(1, 2));
		this.playCard(pNum, row, column, handPosition);
	}

	public void playCard(int pNum, int row, int column, int handPosition) {
		Player p = this.getPlayer(pNum);
		String string;
		if (p.equals(me))
			string = "";
		else
			string = "相手が";
		Card card = p.getHand().get(handPosition);
		card.pId = this.playCnt;
		card.controller = this.getPlayer(pNum);
		card.controller.payWeed(card.cost);
		this.board[row][column] = card;
		p.getHand().remove(handPosition);
		if (!card.isMonster()) {
			this.effectExecuter.execute(card.pId);
			logList.add(string + card.name + "を発動しました。");
		} else {
			logList.add(string + card.name + "を召喚しました。");
		}
		this.playCnt++;
	}

	public void playCard(int pNum, String coordinate, Card card) {
		int row = GameModel.rowCodeToNum(coordinate.substring(0, 1));
		int column = Integer.parseInt(coordinate.substring(1, 2));
		this.playCard(pNum, row, column, card);
	}

	public void playCard(int pNum, int row, int column, Card card) {
		Player p = this.getPlayer(pNum);
		String string;
		if (p.equals(me))
			string = "";
		else
			string = "相手が";
		card.pId = this.playCnt;
		card.controller = p;
		card.controller.payWeed(card.cost);
		this.board[row][column] = card;
		p.getHand().remove(p.getHand().size() - 1);
		if (!card.isMonster()) {
			this.effectExecuter.execute(card.pId);
			logList.add(string + card.name + "を発動しました。");
		} else {
			logList.add(string + card.name + "を召喚しました。");
		}
		this.playCnt++;
	}

	// モンスターの移動
	public boolean move(String coordinate1, String coordinate2) {
		int row1 = GameModel.rowCodeToNum(coordinate1.substring(0, 1));
		int column1 = Integer.parseInt(coordinate1.substring(1, 2));
		int row2 = GameModel.rowCodeToNum(coordinate2.substring(0, 1));
		int column2 = Integer.parseInt(coordinate2.substring(1, 2));
		return this.move(row1, column1, row2, column2);
	}

	public boolean move(int row1, int column1, int row2, int column2) {
		Player p = board[row1][column1].controller;
		String string;
		if (p.equals(me))
			string = "";
		else
			string = "相手が";
		this.board[row2][column2] = this.board[row1][column1];
		this.board[row2][column2].tapped = true;
		this.board[row1][column1] = null;
		logList.add(string + board[row2][column2].name + "を移動しました。");
		if (this.board[row2][column2].controllable(this.getPlayer(Player.ME))
				&& (column2 == 0 && column1 != 0)) {
			this.me.addWeed(1);
			logList.add(board[row2][column2].name + "がバックラインに到達し、草が１つ生えました。");
			return true;
		} else if (this.board[row2][column2].controllable(this
				.getPlayer(Player.OPPONENT))
				&& (column2 == BOARD_SIZE - 1 && column1 != BOARD_SIZE - 1)) {
			this.opponent.addWeed(1);
			logList.add(board[row2][column2].name + "がバックラインに到達し、"
					+ board[row2][column2].controller.name + "の草が１つ生えました。");
			return true;
		}
		return false;
	}

	// モンスターで攻撃
	public void attack(String aCoordinate, String bCoordinate) {
		int row1 = GameModel.rowCodeToNum(aCoordinate.substring(0, 1));
		int column1 = Integer.parseInt(aCoordinate.substring(1, 2));
		if (bCoordinate.equals("player"))
			this.attackToPlayer(row1, column1);
		else {
			int row2 = GameModel.rowCodeToNum(bCoordinate.substring(0, 1));
			int column2 = Integer.parseInt(bCoordinate.substring(1, 2));
			this.attackToMonster(row1, column1, row2, column2);
		}
	}

	// モンスターに攻撃
	public void attackToMonster(int row1, int column1, int row2, int column2) {
		this.board[row2][column2].hp -= this.board[row1][column1].atk;
		this.board[row1][column1].tapped = true;
		Player p = board[row1][column1].controller;
		// ログ
		if (p.equals(me))
			logList.add(board[row1][column1].name + "で"
					+ board[row2][column2].name + "に攻撃しました。");
		else
			logList.add(board[row2][column2].name + "が"
					+ board[row1][column1].name + "に攻撃されました。");
		// 破壊判定
		if (this.board[row2][column2].isDead()) {
			logList.add(board[row1][column1].name + "が"
					+ board[row2][column2].name + "を破壊しました。");
			this.fromBoardToGrave(row2, column2);
		}
	}

	// プレイヤーに攻撃
	public void attackToPlayer(int row, int column) {
		Player p = this.getAnotherPlayer(board[row][column].controller);
		this.board[row][column].tapped = true;
		p.damaged(board[row][column].atk);
		// ログ
		if (p.equals(me))
			logList.add(board[row][column].name + "に直接攻撃されました。");
		else
			logList.add(board[row][column].name + "で相手に直接攻撃しました。");
	}

	// 指定されたマスのカードのパラメータ修正 : 効果処理用
	public void alterParam(int row, int column, String param, String val) {
		if (this.isBlankCell(row, column))
			return;
		// ID
		if (param.equals("id"))
			board[row][column] = new Card(Integer.parseInt(val), context);
		// HP
		else if (param.equals("hp"))
			board[row][column].hp += Integer.parseInt(val);
		// Atk
		else if (param.equals("atk"))
			board[row][column].atk += Integer.parseInt(val);
		// 移動範囲
		else if (param.equals("movRange"))
			board[row][column].movRange = val;
		// 攻撃範囲
		else if (param.equals("atkRange"))
			board[row][column].atkRange = val;
		// タイプ
		else if (!param.equals("type"))
			board[row][column].type = Integer.parseInt(val);
	}

	// 指定されたプレイヤーのパラメータ修正 : 効果処理用
	public void alterParam(Player p1, String target, String param, int val) {
		Player p2 = this.getAnotherPlayer(p1);
		// ライフ
		if (param.equals("life")) {
			if (target.equals("me"))
				p1.addLife(val);
			else if (target.equals("opponent"))
				p2.addLife(val);
		}
		// 草
		else if (param.equals("weed")) {
			if (target.equals("me"))
				p1.addWeed(val);
			else if (target.equals("opponent"))
				p2.addWeed(val);
		}
	}

	// 指定されたマスのカードの移動処理 : 効果処理用
	public void shift(Player p, int row, int column, int dRow, int dColumn) {
		if (this.isBlankCell(row, column))
			return;
		int shiftedRow, shiftedColumn;
		// 移動後座標の計算
		if (p.equals(this.me)) {
			shiftedRow = row + dRow;
			shiftedColumn = column + dColumn;
		} else {
			shiftedRow = row - dRow;
			shiftedColumn = column - dColumn;
		}
		// フィールド外ならもどる
		if (shiftedRow < 0 || shiftedRow >= BOARD_SIZE)
			return;
		if (shiftedColumn < 0 || shiftedColumn >= BOARD_SIZE)
			return;
		if (this.isBlankCell(shiftedRow, shiftedColumn)) {
			board[row + dRow][column + dColumn] = board[row][column];
			board[row][column] = null;
		}
	}

	// 指定されたマスのタップ状態、コントローラの切り替え
	public void switchState(Player p, int row, int column, String param,
			String val) {
		if (this.isBlankCell(row, column))
			return;
		// タップ
		if (param.equals("tapped")) {
			if (val.equals("true"))
				board[row][column].tapped = true;
			else if (val.equals("false"))
				board[row][column].tapped = false;
			else if (val.equals(""))
				board[row][column].tapped = !board[row][column].tapped;
			// コントロール
		} else if (param.equals("controllable")) {
			if (val.equals("true"))
				board[row][column].controller = p;
			else if (val.equals("false"))
				board[row][column].controller = getAnotherPlayer(p);
		}
	}

	// 指定されたマスのカードのパラメータチェック判定 : 効果処理用
	public boolean check(Player p, int row, int column, String param, String val) {
		boolean check = false;
		if (param.equals("isBlank")) {
			if (this.isBlankCell(row, column))
				check = true;
		}
		// カード名
		else if (param.equals("name")) {
			if (board[row][column].name.contains(val))
				check = true;
		}
		// カードタイプ
		else if (param.equals("CardType")) {
			if (board[row][column].cardType.equals(val))
				check = true;
		}
		// IDチェック
		else if (param.equals("id")) {
			if (board[row][column].id == Integer.parseInt(val))
				check = true;
		}
		// タイプチェック
		else if (param.equals("type")) {
			if (board[row][column].type == Integer.parseInt(val))
				check = true;
		}
		// コストチェック
		else if (param.equals("costEqual")) {
			if (board[row][column].cost == Integer.parseInt(val))
				check = true;
		} else if (param.equals("costOver")) {
			if (board[row][column].cost >= Integer.parseInt(val))
				check = true;
		} else if (param.equals("costUnder")) {
			if (board[row][column].cost <= Integer.parseInt(val))
				check = true;
		}
		// ATKチェック
		else if (param.equals("atkEqual")) {
			if (board[row][column].atk == Integer.parseInt(val))
				check = true;
		} else if (param.equals("atkOver")) {
			if (board[row][column].atk >= Integer.parseInt(val))
				check = true;
		} else if (param.equals("atkUnder")) {
			if (board[row][column].atk <= Integer.parseInt(val))
				check = true;
		}
		// HPチェック
		else if (param.equals("hpEqual")) {
			if (board[row][column].hp == Integer.parseInt(val))
				check = true;
		} else if (param.equals("hpOver")) {
			if (board[row][column].hp >= Integer.parseInt(val))
				check = true;
		} else if (param.equals("hpUnder")) {
			if (board[row][column].hp <= Integer.parseInt(val))
				check = true;
		}
		// タップチェック
		else if (param.equals("tapped")) {
			if (val.equals("true") && board[row][column].tapped)
				check = true;
			else if (val.equals("false") && !board[row][column].tapped)
				check = true;
		}
		// コントローラチェック
		else if (param.equals("controllable") && val.equals("true")) {
			if (board[row][column].controllable(p))
				check = true;
		} else if (param.equals("controllable") && val.equals("false")) {
			if (!board[row][column].controllable(p))
				check = true;
		}
		// 座標チェック
		else if (param.equals("thisCell")) {
			int r = GameModel.rowCodeToNum((val.substring(0, 1)));
			int c = Integer.parseInt(val.substring(1, 2));
			if (row == r && column == c)
				check = true;
		}
		return check;
	}

	public static int rowCodeToNum(String s) {
		if (s.equals("a"))
			return 0;
		else if (s.equals("b"))
			return 1;
		else if (s.equals("c"))
			return 2;
		else if (s.equals("d"))
			return 3;
		else if (s.equals("e"))
			return 4;
		else
			return -1;
	}

	public static String numToRowCode(int i) {
		switch (i) {
		case 0:
			return "a";
		case 1:
			return "b";
		case 2:
			return "c";
		case 3:
			return "d";
		case 4:
			return "e";
		default:
			return null;
		}
	}

	public static String getCellCodeString(int row, int column) {
		String code = "";
		switch (row) {
		case 0:
			code += "a";
			break;
		case 1:
			code += "b";
			break;
		case 2:
			code += "c";
			break;
		case 3:
			code += "d";
			break;
		case 4:
			code += "e";
			break;
		}
		return code += column;
	}

	public static int[][] rangeConvertToEnemies(int[][] range) {
		int[][] returnRange = new int[5][5];
		for (int i = 0; i < 5; i++)
			for (int j = 0; j < 5; j++)
				returnRange[i][j] = range[4 - i][4 - j];
		return returnRange;
	}

	public static int[][] rangeStringToIntArray(String range) {
		int index = 0;
		int[][] rangeArr = new int[5][5];
		for (int i = 0; i < 5; i++) {
			// 1~5
			for (int j = 0; j < 5; j++) {// a~e
				rangeArr[j][i] = Integer.parseInt(range.substring(index,
						index + 1));
				index++;
			}
		}
		return rangeArr;
	}

	public static int[][] rangeApplyCell(int[][] rangeArr, int row, int column) {
		int[][] appliedRangeArr = new int[5][5];
		int centerRow = 0, centerColumn = 0;
		for (int i = 0; i < 5; i++)
			for (int j = 0; j < 5; j++)
				if (rangeArr[j][i] == 2) {
					centerRow = j;
					centerColumn = i;
				}
		int dr = centerRow - row, dc = centerColumn - column; // 差
		for (int i = 0; i < 5; i++)
			for (int j = 0; j < 5; j++)
				if (0 <= (i + dc) && (i + dc) < rangeArr.length
						&& 0 <= (j + dr) && (j + dr) < rangeArr.length)
					appliedRangeArr[j][i] = rangeArr[j + dr][i + dc];

		return appliedRangeArr;
	}
}
