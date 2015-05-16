package proj.vipdecardgame;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import proj.vipdecardgame.model.Card;
import proj.vipdecardgame.model.GameModel;
import proj.vipdecardgame.model.GameModel.ChargeOrDrawItem;
import proj.vipdecardgame.model.GameModel.TimeItem;
import proj.vipdecardgame.model.Player;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SoloBattleActivity extends ActionBarActivity {
	// デッキデータファイル名
	private static final String DECK_FILE_NAME = "deck_data.dat";
	// モデル
	GameModel gm;
	// ターンプレイヤー
	Player turnPlayer;
	// 対戦ログ
	ArrayList<String> logList;
	// 選択された手札のポジション、選択されたマス、行動を起こすマス
	int focusHandPosition, selectRow, selectColumn, actionRow, actionColumn;
	// フラグ
	boolean smnFlag = false, mgcFlag = false, movFlag = false, atkFlag = false;
	// キャンセルボタン
	Button canselButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_game);
		this.canselButton = (Button) this.findViewById(R.id.negative_button);
		// ゲーム開始
		this.newGame();
	}

	/*** ゲーム開始 ***/
	public void newGame() {
		// 自分のデッキ
		ArrayList<Card> deck1 = new ArrayList<Card>(), deck2 = new ArrayList<Card>();
		// 保存されているデッキデータの取得
		if (getFileStreamPath(DECK_FILE_NAME).exists()) {
			try {
				FileInputStream fis = openFileInput(DECK_FILE_NAME);
				ObjectInputStream ois = new ObjectInputStream(fis);
				DeckData deckData = (DeckData) ois.readObject();
				ois.close();
				for (int i = 0; i < deckData.cardIDStrings.size(); i++) {
					deck1.add(new Card(Integer.parseInt(deckData.cardIDStrings
							.get(i)), this));
					deck2.add(new Card(Integer.parseInt(deckData.cardIDStrings
							.get(i)), this));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// 保存されたデータが存在しない場合サンプルデッキを生成
			deck1 = Card.createTestDeck(this);
			deck2 = Card.createTestDeck(this);
		}

		// ゲームモデルの初期化
		gm = new GameModel(this, 1);
		// 自分のプレイヤー情報などの初期化
		gm.setMe(new Player(10, deck1, 3, 3));
		gm.getPlayer(Player.ME).name = "プレイヤー1";
		// 対戦相手情報の初期化
		gm.setOpponent(new Player(10, deck2, 3, 3));
		gm.getPlayer(Player.OPPONENT).name = "プレイヤー2";
		gm.getPlayer(Player.OPPONENT).shuffleDeck();

		this.turnPlayer = gm.getPlayer(Player.ME);
		logList = new ArrayList<String>();
		logList.add("対戦を開始しました。");
		gm.toUptime();
		this.updateView();
	}

	/*** ゲーム終了 ***/
	public void endGame(String winnerName) {
		final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("結果")
				.setMessage(winnerName + " の勝ち")
				.setCancelable(false)
				.setNegativeButton("戻る", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})
				.setPositiveButton("もう1回",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								newGame();
							}
						});
		dialog.show();
	}

	/*** 画面の更新 ***/
	public void updateView() {
		// アクションバーの表示設定
		ActionBar ab = getSupportActionBar();
		ab.setTitle("ぼっち対戦 (TURN: " + gm.getTurnCount() + ")");
		// タイム表示view
		TextView timeLabel = (TextView) findViewById(R.id.current_time);
		timeLabel.setTextColor(0xff0000ff);
		// タイム移動ボタン
		Button nextBtn = (Button) findViewById(R.id.next_time);
		nextBtn.setVisibility(View.VISIBLE);
		// 現在のタイム表示
		switch (gm.getTime()) {
		case GameModel.TimeItem.UP:
			timeLabel.setText(turnPlayer.name + "のアップタイム");
			nextBtn.setText("ドロータイムへ");
			break;
		case GameModel.TimeItem.DRAW:
			timeLabel.setText(turnPlayer.name + "のドロータイム");
			nextBtn.setText("メインタイムへ");
			break;
		case GameModel.TimeItem.MAIN:
			timeLabel.setText(turnPlayer.name + "のメインタイム");
			nextBtn.setText("メインタイム終了");
			break;
		case GameModel.TimeItem.ACTION:
			timeLabel.setText(turnPlayer.name + "のアクションタイム");
			nextBtn.setText("エンドタイムへ");
			break;
		case GameModel.TimeItem.END:
			timeLabel.setText(turnPlayer.name + "のエンドタイム");
			nextBtn.setText("ターンエンド");
			break;
		case GameModel.TimeItem.COUNTER:
			timeLabel.setText(turnPlayer.name + "のカウンタータイム");
			nextBtn.setText("カウンタータイムを終了");
			break;
		case GameModel.TimeItem.OPPONENT_UP:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(turnPlayer.name + "のアップタイム");
			nextBtn.setText("ドロータイムへ");
			break;
		case GameModel.TimeItem.OPPONENT_DRAW:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(turnPlayer.name + "のドロータイム");
			nextBtn.setText("メインタイムへ");
			break;
		case GameModel.TimeItem.OPPONENT_MAIN:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(turnPlayer.name + "のメインタイム");
			nextBtn.setText("メインタイム終了");
			break;
		case GameModel.TimeItem.OPPONENT_ACTION:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(turnPlayer.name + "のアクションタイム");
			nextBtn.setText("エンドタイムへ");
			break;
		case GameModel.TimeItem.OPPONENT_END:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(turnPlayer.name + "のエンドタイム");
			nextBtn.setText("ターン終了");
			break;
		case GameModel.TimeItem.OPPONENT_COUNTER:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(turnPlayer.name + "のカウンタータイム");
			nextBtn.setText("カウンタータイム終了");
			break;
		}
		// ログ表示view
		((TextView) findViewById(R.id.message)).setText(this.logList
				.get(logList.size() - 1));
		// 自分のプレイヤーデータの取得
		Player p1 = gm.getPlayer(Player.ME);
		((TextView) findViewById(R.id.p1_life)).setText("ライフ：" + p1.getLife()); // 自分のライフ
		((TextView) findViewById(R.id.p1_cost)).setText("草：" + p1.getWeed()); // 自分のコスト
		((TextView) findViewById(R.id.p1_deck)).setText("デッキ：" + p1.deckSize());// 自分のデッキ枚数
		((TextView) findViewById(R.id.p1_grave)).setText("墓地："
				+ p1.getGrave().size()); // 自分の墓地の枚数
		((TextView) findViewById(R.id.p1_hand_num)).setText("手札："
				+ p1.getHand().size()); // 自分の手札の枚数
		// 自分の手札の表示
		LinearLayout handView = (LinearLayout) findViewById(R.id.hand_list);
		handView.removeAllViews();
		for (int i = 0; i < this.turnPlayer.getHand().size(); i++) {
			Card card = this.turnPlayer.getHand().get(i);
			ImageView iv = new ImageView(this);
			iv.setPadding(5, 5, 5, 5);
			iv.setScaleType(ScaleType.CENTER_INSIDE);
			iv.setAdjustViewBounds(true);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			iv.setLayoutParams(params);
			iv.setImageResource(Card.getIconImgId(card.id, this));
			iv.setTag("" + i);
			iv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View sender) {
					int position = Integer.parseInt(sender.getTag().toString());
					onHandCardClick(position);
				}
			});
			iv.setClickable(true);
			handView.addView(iv);
		}

		// 相手のプレイヤー情報の取得
		Player p2 = gm.getPlayer(Player.OPPONENT);
		// p2のライフの表示
		((TextView) findViewById(R.id.p2_life)).setText("ライフ：" + p2.getLife());
		// p2のコストの表示
		((TextView) findViewById(R.id.p2_cost)).setText("草：" + p2.getWeed());
		// p2のデッキ枚数の表示
		((TextView) findViewById(R.id.p2_deck)).setText("デッキ：" + p2.deckSize());
		// p2の墓地の枚数の表示
		((TextView) findViewById(R.id.p2_grave)).setText("墓地："
				+ p2.getGrave().size());
		// 相手の手札の枚数
		((TextView) findViewById(R.id.p2_hand_num)).setText("手札："
				+ p2.getHand().size());

		for (int i = 0; i < 5; i++)
			for (int j = 0; j < 5; j++) {
				String str = "field_";
				str += GameModel.numToRowCode(i);
				str += j + 1;
				if (!gm.isBlankCell(i, j)) {
					Card card = gm.getCard(i, j);
					// 指定のセルのviewIdの作成
					int vId = getResources().getIdentifier(str, "id",
							getPackageName());
					ImageView iv = (ImageView) findViewById(vId);
					iv.setImageResource(Card.getIconImgId(card.id, this));
					if (card.tapped || card.controllable(p2)) {
						int kakudo = 0;
						if (card.tapped)
							kakudo -= 90;
						if (card.controllable(p2))
							kakudo -= 180;

						// getDrawableメソッドで取り戻したものを、BitmapDrawable形式にキャストする
						BitmapDrawable bd = (BitmapDrawable) iv.getDrawable();
						// getBitmapメソッドでビットマップファイルを取り出す
						Bitmap bmp = bd.getBitmap();
						// 回転させる
						Matrix matrix = new Matrix();
						matrix.postRotate(kakudo);
						// Bitmap回転させる
						Bitmap flippedBmp = Bitmap.createBitmap(bmp, 0, 0,
								bmp.getWidth(), bmp.getHeight(), matrix, false);
						// 加工したBitmapを元のImageViewにセットする
						iv.setImageDrawable(new BitmapDrawable(flippedBmp));
					}
					if (card.controllable(p1))
						iv.setBackgroundResource(R.xml.p1_cell);
					else
						iv.setBackgroundResource(R.xml.p2_cell);
					iv.setClickable(true);
				} else {
					// 指定のセルのviewIdの作成
					int vId = getResources().getIdentifier(str, "id",
							getPackageName());
					ImageView iv = (ImageView) findViewById(vId);
					iv.setImageBitmap(null);
					iv.setBackgroundResource(R.xml.cell);
					iv.setClickable(true);
				}
			}
	}

	/*** フラグの初期化 ***/
	public void initFlag() {
		this.canselButton.setVisibility(View.GONE);
		this.smnFlag = false;
		this.mgcFlag = false;
		this.movFlag = false;
		this.atkFlag = false;
		this.updateView();
	}

	/** ターンプレイヤーチェック **/
	public void checkTurnPlayer() {
		switch (gm.getTime()) {
		case GameModel.TimeItem.UP:
		case GameModel.TimeItem.DRAW:
		case GameModel.TimeItem.MAIN:
		case GameModel.TimeItem.ACTION:
		case GameModel.TimeItem.END:
		case GameModel.TimeItem.COUNTER:
			this.turnPlayer = gm.getPlayer(Player.ME);
			break;
		case GameModel.TimeItem.OPPONENT_UP:
		case GameModel.TimeItem.OPPONENT_DRAW:
		case GameModel.TimeItem.OPPONENT_MAIN:
		case GameModel.TimeItem.OPPONENT_ACTION:
		case GameModel.TimeItem.OPPONENT_END:
		case GameModel.TimeItem.OPPONENT_COUNTER:
		case GameModel.TimeItem.OPPONENT_COUNTER_END:
			this.turnPlayer = gm.getPlayer(Player.OPPONENT);
			break;
		}
	}

	/** キャンセルボタンクリックイベント **/
	public void onNegativeClick(View sender) {
		this.initFlag();
		sender.setVisibility(View.GONE);
	}

	/** タイム移動ボタンクリックイベント **/
	public void onClickNextTime(View v) {
		// 確認ダイアログ
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("確認").setNegativeButton("キャンセル", null);
		// 現在タイム毎の処理
		switch (gm.getTime()) {
		// アップタイム時
		case GameModel.TimeItem.UP:
			gm.toDrawtime();
			this.showChargeOrDrawDialog();
			break;
		// ドロータイム時
		case GameModel.TimeItem.DRAW:
			gm.toMaintime();
			break;
		case GameModel.TimeItem.MAIN:
			gm.toOpponentCountertime();
			break;
		case GameModel.TimeItem.COUNTER:
			gm.toOpponentActiontime();
			break;
		case GameModel.TimeItem.ACTION:
			gm.toOpponentUptime();
			break;
		case GameModel.TimeItem.END:
			gm.toOpponentUptime();
			break;
		case GameModel.TimeItem.OPPONENT_UP:
			gm.toOpponentDrawtime();
			this.showChargeOrDrawDialog();
			break;
		case GameModel.TimeItem.OPPONENT_DRAW:
			gm.toOpponentMaintime();
			break;
		case GameModel.TimeItem.OPPONENT_MAIN:
			gm.toCountertime();
			break;
		case GameModel.TimeItem.OPPONENT_ACTION:
			gm.toOpponentEndtime();
			break;
		case GameModel.TimeItem.OPPONENT_END:
			gm.toUptime();
			break;
		case GameModel.TimeItem.OPPONENT_COUNTER:
			gm.toActiontime();
			break;
		}
		// ターンプレイヤー確認
		this.checkTurnPlayer();
		// フラグの初期化
		this.initFlag();
	}

	//
	public void showChargeOrDrawDialog() {
		final String[] items1 = { "カードを２枚引く", "草を１生やしてカードを１枚引く", "草を２生やす。" };
		final String[] items2 = { "草を１生やしてカードを１枚引く", "草を２生やす。" };
		// 選択ダイアログ
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("チャージオアドロータイム");
		// 先攻時
		if (gm.getTurnCount() == 1
				&& turnPlayer.equals(gm.getPlayer(Player.ME))) {
			// ログ追加
			logList.add(turnPlayer.name + "がカードを1枚ドローしました。");
			gm.draw(turnPlayer, 4);
			initFlag();
			gm.toMaintime();
			return;
		}
		// デッキが0枚
		else if (this.turnPlayer.deckSize() == 0) {
			logList.add(turnPlayer.name + "が草を2つ生やしました。");
			gm.draw(turnPlayer, 3);
			initFlag();
			if (turnPlayer.equals(gm.getPlayer(Player.ME)))
				gm.toMaintime();
			else
				gm.toOpponentMaintime();
			return;
		}
		// デッキが1枚
		else if (this.turnPlayer.deckSize() == 1) {
			dialog.setItems(items2, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					gm.draw(turnPlayer, which + 2);
					switch (which + 2) {
					case ChargeOrDrawItem.DRAW2:
						logList.add(turnPlayer.name + "がカードを2枚ドローしました。");
						break;
					case ChargeOrDrawItem.DRAW1_WEED1:
						logList.add(turnPlayer.name + "が草を1つ生やし、カードを1枚ドローしました。");
						break;
					case ChargeOrDrawItem.WEED2:
						logList.add(turnPlayer.name + "が草を2つ生やしました。");
						break;
					case ChargeOrDrawItem.DRAW1:
						logList.add(turnPlayer.name + "がカードを1枚ドローしました。");
						break;
					}
					initFlag();
					if (turnPlayer.equals(gm.getPlayer(Player.ME)))
						gm.toMaintime();
					else
						gm.toOpponentMaintime();
					return;
				}
			});
		}
		// デッキが2枚以上
		else {
			dialog.setItems(items1, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					gm.draw(turnPlayer, which + 1);
					switch (which + 1) {
					case ChargeOrDrawItem.DRAW2:
						logList.add(turnPlayer.name + "がカードを2枚ドローしました。");
						break;
					case ChargeOrDrawItem.DRAW1_WEED1:
						logList.add(turnPlayer.name + "が草を1つ生やし、カードを1枚ドローしました。");
						break;
					case ChargeOrDrawItem.WEED2:
						logList.add(turnPlayer.name + "が草を2つ生やしました。");
						break;
					case ChargeOrDrawItem.DRAW1:
						logList.add(turnPlayer.name + "がカードを1枚ドローしました。");
						break;
					}
					initFlag();
					if (turnPlayer.equals(gm.getPlayer(Player.ME)))
						gm.toMaintime();
					else
						gm.toOpponentMaintime();
					return;
				}
			});
		}
		dialog.setCancelable(false).show();
	}

	/** 手札カードのクリックイベント **/
	public void onHandCardClick(int position) {
		focusHandPosition = position;
		// 選択されたカードの取得
		final Card card = this.turnPlayer.getHand().get(position);
		// 表示するカード画像Viewの生成
		ImageView imageView = new ImageView(this);
		imageView.setImageResource(card.getCardImgId(this));
		// カード情報表示ダイアログ
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("カード詳細");
		dialog.setView(imageView);
		dialog.setNegativeButton("閉じる", null);
		// 召喚ボタンのイベントリスナー
		DialogInterface.OnClickListener onSummonListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 召喚可能判定
				if (card.cost > turnPlayer.getWeed())
					Toast.makeText(SoloBattleActivity.this, "召喚に必要な草が足りません。",
							Toast.LENGTH_SHORT).show();
				// 召喚イベントへ
				else
					onSummonMonsterClick();
			}
		};
		// マジック発動ボタンのイベントリスナー
		DialogInterface.OnClickListener onMagicListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 召喚可能判定
				if (card.cost > turnPlayer.getWeed())
					Toast.makeText(SoloBattleActivity.this, "召喚に必要な草が足りません。",
							Toast.LENGTH_SHORT).show();
				// マジック発動イベントへ
				else
					onPlayMagicClick();
			}
		};

		// メインタイムかつ選択されたカードがモンスター
		if (card.isMonster()) {
			if ((turnPlayer.equals(gm.getPlayer(Player.ME)) && gm.getTime() == TimeItem.MAIN)
					|| (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)) && gm
							.getTime() == TimeItem.OPPONENT_MAIN))
				dialog.setPositiveButton("召喚", onSummonListener);
		}
		// メインタイムまたはカウンタータイム,かつ選択されたカードがマジック
		else {
			if ((turnPlayer.equals(gm.getPlayer(Player.ME)) && (gm.getTime() == TimeItem.COUNTER || gm
					.getTime() == TimeItem.MAIN))
					|| (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)) && (gm
							.getTime() == TimeItem.OPPONENT_COUNTER || gm
							.getTime() == TimeItem.OPPONENT_MAIN)))
				dialog.setPositiveButton("発動", onMagicListener);
		}
		// ダイアログの表示
		dialog.show();
	}

	/*** フィールドクリックイベント ***/
	public void onFieldClick(View sender) {
		int row = 0, column = 0;
		int vId = sender.getId();
		switch (vId) {
		case R.id.field_a1:
			row = 0;
			column = 0;
			break;
		case R.id.field_b1:
			row = 1;
			column = 0;
			break;
		case R.id.field_c1:
			row = 2;
			column = 0;
			break;
		case R.id.field_d1:
			row = 3;
			column = 0;
			break;
		case R.id.field_e1:
			row = 4;
			column = 0;
			break;
		case R.id.field_a2:
			row = 0;
			column = 1;
			break;
		case R.id.field_b2:
			row = 1;
			column = 1;
			break;
		case R.id.field_c2:
			row = 2;
			column = 1;
			break;
		case R.id.field_d2:
			row = 3;
			column = 1;
			break;
		case R.id.field_e2:
			row = 4;
			column = 1;
			break;
		case R.id.field_a3:
			row = 0;
			column = 2;
			break;
		case R.id.field_b3:
			row = 1;
			column = 2;
			break;
		case R.id.field_c3:
			row = 2;
			column = 2;
			break;
		case R.id.field_d3:
			row = 3;
			column = 2;
			break;
		case R.id.field_e3:
			row = 4;
			column = 2;
			break;
		case R.id.field_a4:
			row = 0;
			column = 3;
			break;
		case R.id.field_b4:
			row = 1;
			column = 3;
			break;
		case R.id.field_c4:
			row = 2;
			column = 3;
			break;
		case R.id.field_d4:
			row = 3;
			column = 3;
			break;
		case R.id.field_e4:
			row = 4;
			column = 3;
			break;
		case R.id.field_a5:
			row = 0;
			column = 4;
			break;
		case R.id.field_b5:
			row = 1;
			column = 4;
			break;
		case R.id.field_c5:
			row = 2;
			column = 4;
			break;
		case R.id.field_d5:
			row = 3;
			column = 4;
			break;
		case R.id.field_e5:
			row = 4;
			column = 4;
			break;
		}
		// 移動攻撃フラグが立っていれば
		if (movFlag || atkFlag) {
			actionRow = row;
			actionColumn = column;
		} else {
			selectRow = row;
			selectColumn = column;
		}

		Card card = null;
		// 召喚、発動なら選択された手札からカード取得
		if (smnFlag || mgcFlag)
			card = turnPlayer.getHand().get(this.focusHandPosition);
		// 移動、攻撃なら選択されたマスからカード取得
		else if (!gm.isBlankCell(selectRow, selectColumn))
			card = gm.getCard(selectRow, selectColumn);
		// 召喚フラグチェック
		if (smnFlag) {
			int[][] range = gm.summonableRange;
			if (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)))
				range = GameModel.rangeConvertToEnemies(range);
			// 召喚可能なマスか判定
			if (range[row][column] == 1 && gm.isBlankCell(row, column)) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle("確認")
						.setMessage(
								"草を " + card.cost + " 支払って " + card.name
										+ " を召喚しますか？")
						.setCancelable(false)
						.setNegativeButton("キャンセル", null)
						.setPositiveButton("召喚",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										int pNum;
										if (turnPlayer.equals(gm
												.getPlayer(Player.ME)))
											pNum = Player.ME;
										else
											pNum = Player.OPPONENT;
										// ログ追加
										logList.add(turnPlayer.name
												+ "が"
												+ turnPlayer.getHand().get(
														focusHandPosition).name
												+ "を召喚しました。");
										gm.playCard(pNum, selectRow,
												selectColumn, focusHandPosition); // 場にプレイ処理
										initFlag();
										// 勝敗判定
										checkGameEnd();
									}
								}).show();
			} else
				Toast.makeText(this, "召喚可能なマスを選択してください。", Toast.LENGTH_SHORT)
						.show();
		}
		// マジックフラグチェック
		else if (mgcFlag) {
			int[][] range = gm.magicPlayableRange;
			if (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)))
				range = GameModel.rangeConvertToEnemies(range);
			// 発動可能なマスか判定
			if (range[row][column] == 1 && gm.isBlankCell(row, column)) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle("確認")
						.setMessage(
								"草を " + card.cost + " 支払って " + card.name
										+ " を発動しますか？")
						.setCancelable(false)
						.setNegativeButton("キャンセル", null)
						.setPositiveButton("発動",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										int pNum;
										if (turnPlayer.equals(gm
												.getPlayer(Player.ME)))
											pNum = Player.ME;
										else
											pNum = Player.OPPONENT;
										// ログ追加
										logList.add(turnPlayer.name
												+ "が"
												+ turnPlayer.getHand().get(
														focusHandPosition).name
												+ "を発動しました。");
										gm.playCard(pNum, selectRow,
												selectColumn, focusHandPosition); // 場にプレイ処理
										initFlag();
										// 勝敗判定
										checkGameEnd();
									}
								}).show();
			} else
				Toast.makeText(this, "発動可能なマスを選択してください。", Toast.LENGTH_SHORT)
						.show();
		}
		// 移動フラグチェック
		else if (movFlag) {
			int[][] range = GameModel.rangeStringToIntArray(card.movRange); // 移動モンスターの移動範囲を取得
			if (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)))
				range = GameModel.rangeConvertToEnemies(range); // 相手側から見た範囲に変換
			range = GameModel.rangeApplyCell(range, selectRow, selectColumn); // 移動範囲を存在するマスに適用変換
			// 移動可能なマスか判定
			if (range[actionRow][actionColumn] == 1
					&& gm.isBlankCell(actionRow, actionColumn)) {
				logList.add(turnPlayer.name + "が" + card.name + "を移動しました。");
				boolean arrival = gm.move(selectRow, selectColumn, actionRow,
						actionColumn); // 移動処理
				if (arrival)
					logList.add(card.name + "がバックラインに到達し、" + turnPlayer.name
							+ "の草が１つ生えました。");
				initFlag();
				// 勝敗判定
				checkGameEnd();
			} else
				Toast.makeText(this, "移動可能なマスを選択してください。", Toast.LENGTH_SHORT)
						.show();
		}
		// 攻撃フラグチェック
		else if (atkFlag) {
			int[][] range = GameModel.rangeStringToIntArray(card.atkRange); // 移動モンスターの移動範囲を取得
			if (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)))
				range = GameModel.rangeConvertToEnemies(range); // 相手側から見た範囲に変換
			range = GameModel.rangeApplyCell(range, selectRow, selectColumn); // 移動範囲を存在するマスに適用変換
			// 攻撃可能なマスか判定
			if (range[actionRow][actionColumn] == 1
					&& !gm.getCard(actionRow, actionColumn).controllable(
							turnPlayer)) {
				String blockerName = gm.getCard(actionRow, actionColumn).name;
				// 攻撃処理
				gm.attackToMonster(selectRow, selectColumn, actionRow,
						actionColumn);
				// ログ追加
				if (gm.isBlankCell(actionRow, actionColumn))
					logList.add(turnPlayer.name + "が" + card.name + "で"
							+ blockerName + "を破壊しました。");
				else
					logList.add(turnPlayer.name + "が" + card.name + "で"
							+ blockerName + "を攻撃しました。");
				initFlag();
				// 勝敗判定
				checkGameEnd();
			} else
				Toast.makeText(this, "攻撃可能なマスを選択してください。", Toast.LENGTH_SHORT)
						.show();
		} else if (card != null) {
			this.initFlag();
			this.showCardPopup(sender, card);
		}
	}

	public void onGraveClick(View v) {
		final int vId = v.getId();
		ArrayList<Card> graveList = null;
		String title = null;
		switch (vId) {
		case R.id.p1_grave:
			graveList = (ArrayList<Card>) gm.getPlayer(Player.ME).getGrave();
			title = "あなたの墓地";
			break;
		case R.id.p2_grave:
			graveList = (ArrayList<Card>) gm.getPlayer(Player.OPPONENT)
					.getGrave();
			title = "対戦相手の墓地";
			break;
		}
		GridView gv = new GridView(this);
		gv.setNumColumns(5);
		gv.setAdapter(new CardGridAdapter(this, graveList));
		gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ArrayList<Card> graveList = null;
				switch (vId) {
				case R.id.p1_grave:
					graveList = (ArrayList<Card>) gm.getPlayer(Player.ME)
							.getGrave();
					break;
				case R.id.p2_grave:
					graveList = (ArrayList<Card>) gm.getPlayer(Player.OPPONENT)
							.getGrave();
					break;
				}
				Card card = graveList.get(position);
				showCardDetails(card);
			}
		});

		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(title).setView(gv).setNegativeButton("閉じる", null)
				.show();
	}

	/*** 場のカードのボップアップ表示 ***/
	public void showCardPopup(View v, Card c) {
		final Card card = c;
		PopupMenu popup = new PopupMenu(this, v);
		// ATK,HPの表示
		if (card.isMonster())
			popup.getMenu().add(
					String.format("ATK : %d, HP : %d", card.atk, card.hp));
		// アクションタイムかつ自分のモンスターなら移動攻撃メニュー表示
		if ((gm.getTime() == TimeItem.ACTION || gm.getTime() == TimeItem.OPPONENT_ACTION)
				&& gm.getCard(selectRow, selectColumn).controllable(turnPlayer)
				&& !gm.getCard(selectRow, selectColumn).tapped) {
			popup.getMenu().add(1, 2, 2, "移動");
			popup.getMenu().add(1, 3, 3, "攻撃");
			if ((this.selectColumn == 0 && turnPlayer.equals(gm
					.getPlayer(Player.ME)))
					|| (this.selectColumn == 4 && turnPlayer.equals(gm
							.getPlayer(Player.OPPONENT))))
				popup.getMenu().add(1, 4, 4, "プレイヤーに攻撃");
		} else {
			if (card.isMonster()) {
				popup.getMenu().add(1, 5, 5, "移動範囲表示");
				popup.getMenu().add(1, 6, 6, "攻撃範囲表示");
			} else
				popup.getMenu().add(1, 5, 5, "範囲表示");
		}
		popup.getMenu().add(1, 7, 7, "カード詳細");
		popup.show();
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
				case 2:
					onMoveClick(card);
					break;
				case 3:
					onAttackToMonsterClick(card);
					break;
				case 4:
					onAttackToPlayerClick();
					break;
				case 5:
					int[][] movRange = GameModel
							.rangeStringToIntArray(card.movRange);
					if (card.controllable(gm.getPlayer(Player.OPPONENT)))
						movRange = GameModel.rangeConvertToEnemies(movRange);
					movRange = GameModel.rangeApplyCell(movRange, selectRow,
							selectColumn);
					showRangeCell(1, movRange);
					break;
				case 6:
					int[][] atkRange = GameModel
							.rangeStringToIntArray(card.atkRange);
					if (card.controllable(gm.getPlayer(Player.OPPONENT)))
						atkRange = GameModel.rangeConvertToEnemies(atkRange);
					atkRange = GameModel.rangeApplyCell(atkRange, selectRow,
							selectColumn);
					showRangeCell(2, atkRange);
					break;
				case 7:
					showCardDetails(card);
					break;
				}
				return false;
			}
		});
	}

	/*** 範囲表示メソッド ***/
	public void showRangeCell(int mode, int[][] rangeArr) {
		for (int i = 0; i < GameModel.BOARD_SIZE; i++)
			for (int j = 0; j < GameModel.BOARD_SIZE; j++)
				if (rangeArr[j][i] == 1) {
					String string = "field_" + GameModel.numToRowCode(j);
					string += (i + 1);
					int vId = getResources().getIdentifier(string, "id",
							getPackageName());
					int color = 0x55ffff00;
					switch (mode) {
					case 1:
						color = 0x5500ff00;
						break;
					case 2:
						color = 0x55ff5500;
						break;
					}
					((ImageView) findViewById(vId)).setBackgroundColor(color);
				}
	}

	/*** 場のカード詳細表示メソッド ***/
	public void showCardDetails(Card card) {
		// ダイアログタイトル部分に表示するカード情報
		String info;
		if (card.isMonster())
			info = String.format("ATK : %d, HP : %d", card.atk, card.hp);
		else
			info = "マジック";
		// 表示するカード画像Viewの生成
		ImageView iv = new ImageView(this);
		iv.setImageResource(card.getCardImgId(this));
		// カード詳細表示ダイアログ
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(info).setView(iv).setNegativeButton("とじる", null).show();
	}

	/*** モンスター召喚イベント ***/
	public void onSummonMonsterClick() {
		this.smnFlag = true;
		canselButton.setVisibility(View.VISIBLE);
		canselButton.setText("召喚をキャンセル");
		// 範囲を表示
		int[][] range = gm.summonableRange;
		if (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)))
			range = GameModel.rangeConvertToEnemies(range);
		showRangeCell(0, range);
		// メッセージトースト
		Toast.makeText(this, "召喚するマスを選択してください", Toast.LENGTH_SHORT).show();
	}

	/*** マジック発動イベント ***/
	public void onPlayMagicClick() {
		this.mgcFlag = true;
		canselButton.setVisibility(View.VISIBLE);
		canselButton.setText("発動をキャンセル");
		// 範囲を表示
		int[][] range = gm.magicPlayableRange;
		if (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)))
			range = GameModel.rangeConvertToEnemies(range);
		showRangeCell(0, range);
		// メッセージトースト
		Toast.makeText(this, "発動するマスを選択してください", Toast.LENGTH_SHORT).show();
	}

	/*** モンスター移動イベント ***/
	public void onMoveClick(Card card) {
		// 移動フラグを立てる
		this.movFlag = true;
		canselButton.setVisibility(View.VISIBLE);
		canselButton.setText("移動をキャンセル");
		// 移動可能範囲を表示
		int[][] range = GameModel.rangeStringToIntArray(card.movRange);
		if (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)))
			range = GameModel.rangeConvertToEnemies(range);
		range = GameModel.rangeApplyCell(range, selectRow, selectColumn);
		showRangeCell(0, range);
		// メッセージトースト
		Toast.makeText(this, "移動するマスを選択してください", Toast.LENGTH_SHORT).show();
	}

	/*** モンスター攻撃イベント ***/
	public void onAttackToMonsterClick(Card card) {
		// 攻撃フラグを立てる
		this.atkFlag = true;
		canselButton.setVisibility(View.VISIBLE);
		canselButton.setText("攻撃をキャンセル");
		// 攻撃可能範囲を表示
		int[][] range = GameModel.rangeStringToIntArray(card.atkRange);
		if (turnPlayer.equals(gm.getPlayer(Player.OPPONENT)))
			range = GameModel.rangeConvertToEnemies(range);
		range = GameModel.rangeApplyCell(range, selectRow, selectColumn);
		showRangeCell(0, range);
		// メッセージダイアログ
		Toast.makeText(this, "攻撃するモンスターを選択してください", Toast.LENGTH_SHORT).show();
	}

	/*** プレイヤー攻撃イベント ***/
	public void onAttackToPlayerClick() {
		// ログ追加
		logList.add(turnPlayer.name + "が"
				+ gm.getCard(selectRow, selectColumn).name + "で直接攻撃しました。");
		gm.attackToPlayer(selectRow, selectColumn);
		initFlag();
		// 勝敗判定
		this.checkGameEnd();
	}

	public void checkGameEnd() {
		if (gm.getPlayer(Player.ME).isDead())
			endGame(gm.getPlayer(Player.OPPONENT).name);
		else if (gm.getPlayer(Player.OPPONENT).isDead())
			endGame(gm.getPlayer(Player.ME).name);
	}

}
