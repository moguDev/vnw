package proj.vipdecardgame.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class EffectExecuter {

	// モデル
	private GameModel model;
	// 処理中の効果
	Effect focusEffect;

	// コンストラクタ
	public EffectExecuter(GameModel m) {
		this.model = m;
	}

	// 効果処理メソッド
	public void execute(int playId) {
		int row = model.getRowFromPlayId(playId); // playIdから座標取得
		int column = model.getColumnFromPlayId(playId); // playIdから座標取得
		Card card = model.getCard(row, column); // 座標からカード取得
		Player player = card.controller; // プレイヤーの取得
		this.focusEffect = card.effect; // 効果オブジェクトの取得
		int[][] range = GameModel.rangeStringToIntArray(card.atkRange); // 効果範囲を取得
		if (player.equals(model.getPlayer(Player.OPPONENT))) // コントロールが相手の場合
			range = GameModel.rangeConvertToEnemies(range);// 相手側からみた範囲に変換する
		range = GameModel.rangeApplyCell(range, row, column); // 範囲を座標に合わせる

		JSONArray script = focusEffect.triggered(playId, row, column); // スクリプトの取得
		try {
			for (int i = 0; i < script.length(); i++) {
				JSONObject effObj = script.getJSONObject(i); // 効果配列から順に要素を取り出す
				String scope = effObj.getString("scope"); // 効果が及ぶ範囲の取得
				JSONArray processArr = effObj.getJSONArray("process"); // 効果処理配列の取得

				/** 範囲内に及ぶ効果の処理 **/
				if (scope.equals("range")) {
					for (int r = 0; r < 5; r++)
						for (int c = 0; c < 5; c++)
							// 対象範囲かつカードが存在するならば
							if (range[r][c] == 1)
								processingScopeCell(player, r, c, processArr);
				}
				/** 盤面全体に及ぶ効果の処理 **/
				else if (scope.equals("board")) {
					for (int r = 0; r < 5; r++)
						for (int c = 0; c < 5; c++)
							processingScopeCell(player, r, c, processArr);
				}
				/** 墓地に及ぶ効果の処理 **/
				else if (scope.equals("grave")) {

				}
				/** プレイヤーに及ぶ効果の処理 **/
				else if (scope.equals("player"))
					processingScopePlayer(player, processArr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processingScopeCell(Player player, int row, int column,
			JSONArray processArr) {
		int next = 0;
		while (true) {
			try {
				JSONObject process = processArr.getJSONObject(next);
				String doing = process.getString("do");
				/** do check **/
				if (doing.equals("check")) {
					String param = process.getString("param");
					String val = process.getString("val");
					// チェック処理
					boolean check = false;
					check = model.check(player, row, column, param, val);
					// 次のプロセス番号の取得
					if (check)
						next = process.getInt("trueNext");
					else
						next = process.getInt("falseNext");
				}
				// パラメータ修正:value
				else if (doing.equals("alterValue")) {
					String param = process.getString("param");
					String val = process.getString("val");
					// 修正処理
					model.alterParam(row, column, param, val);
					// 次のプロセス番号の取得
					next = process.getInt("next");
				}
				// パラメータ修正:store[index]
				else if (doing.equals("alterIndex")) {
					String param = process.getString("param");
					int index = process.getInt("index");
					String val = "" + this.focusEffect.getStore(index);
					// 修正処理
					model.alterParam(row, column, param, val);
					// 次のプロセス番号の取得
					next = process.getInt("next");
				}
				// 座標移動
				else if (doing.equals("shift")) {
					int dRow, dColumn;
					dRow = process.getInt("dRow");
					dColumn = process.getInt("dColumn");
					// 座標修正処理
					model.shift(player, row, column, dRow, dColumn);
					// 次のプロセス番号の取得
					next = process.getInt("next");
				}
				// カード状態
				else if (doing.equals("switch")) {
					String param = process.getString("param");
					String val = process.getString("val");
					model.switchState(player, row, column, param, val);
					next = process.getInt("next");
				}
				// カード破壊
				else if (doing.equals("destroy")) {
					if (!model.isBlankCell(row, column))
						model.getCard(row, column).hp = 0;
					next = process.getInt("next");
				}
				// 値保持
				else if (doing.equals("store")) {
					String operation = process.getString("operation");
					int index = 0, value = 0;
					index = process.getInt("index");
					if (isNumber(process.getString("value"))) // val:即値
						value = Integer.parseInt(process.getString("value"));
					else if (!model.isBlankCell(row, column)) {
						if (process.getString("value").equals("cost")) // val:コスト
							value = model.getCard(row, column).cost;
						else if (process.getString("value").equals("atk")) // val:atk
							value = model.getCard(row, column).atk;
						else if (process.getString("value").equals("hp")) // val:hp
							value = model.getCard(row, column).hp;
					}
					/** 初期化 **/
					if (operation.equals("init")) {
						this.focusEffect.initStore(index, value);
					}
					/** 加算 **/
					else if (operation.equals("add")) {
						this.focusEffect.addStore(index, value);
					}
					/** 次の処理 **/
					next = process.getInt("next");
				}
				if (next == -1)
					break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (!model.isBlankCell(row, column))
			if (model.getCard(row, column).isDead())
				model.fromBoardToGrave(row, column);
	}

	public void processingScopePlayer(Player player, JSONArray processArr) {
		int next = 0;
		while (true) {
			try {
				JSONObject process = processArr.getJSONObject(next);
				String doing = process.getString("do");
				// 値保持
				if (doing.equals("store")) {
					String operation = process.getString("operation");
					int index, value = 0;
					index = process.getInt("index");
					if (EffectExecuter.isNumber(process.getString("value")))
						value = process.getInt("value");
					if (operation.equals("init"))
						this.focusEffect.initStore(index, value);
					else if (operation.equals("add"))
						this.focusEffect.addStore(index, value);
					next = process.getInt("next");
				}
				// パラメータ修正:value
				else if (doing.equals("alterValue")) {
					String target = process.getString("target");
					String param = process.getString("param");
					int val = process.getInt("val");
					model.alterParam(player, target, param, val);
					next = process.getInt("next");
				}
				// パラメータ修正:store[index]
				else if (doing.equals("alterIndex")) {
					String target = process.getString("target");
					String param = process.getString("param");
					int index = process.getInt("index");
					int val = this.focusEffect.getStore(index);
					model.alterParam(player, target, param, val);
					next = process.getInt("next");
				}
				// ドロー:value
				else if (doing.equals("drawValue")) {
				}
				// ドロー:index
				else if (doing.equals("drawIndex")) {
				}
				if (next == -1)
					break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void executeStockEffect() {

	}

	public static boolean isNumber(String val) {
		try {
			Integer.parseInt(val);
			return true;
		} catch (NumberFormatException nfex) {
			return false;
		}
	}
}
