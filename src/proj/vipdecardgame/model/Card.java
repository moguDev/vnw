package proj.vipdecardgame.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;

public class Card {
	public final static int FIRE = 0, WATER = 1, WIND = 2, DARK = 3, LIGHT = 4,
			NEUTRAL = 5;
	// カードパラメータ
	public String cardType;
	public int id;
	public String name;
	public int type, cost, atk, hp;
	public String movRange, atkRange, ft;
	public Effect effect;
	// プレイID
	public int pId = 0;
	// タップ状況
	public boolean tapped = false, controllable = true;
	// コントロールしているプレイヤー
	public Player controller = null;

	// モンスターか判定
	public boolean isMonster() {
		if (this.id < 20000)
			return true;
		else
			return false;
	}

	// コントローラかどうか判定
	public boolean controllable(Player p) {
		if (this.controller.equals(p))
			return true;
		else
			return false;
	}

	// 死んでいるか判定
	public boolean isDead() {
		if (this.hp <= 0)
			return true;
		return false;
	}

	// コンストラクタ
	public Card(int id, Context context) {
		AssetManager as = context.getResources().getAssets();

		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;

		StringBuilder sb = new StringBuilder();
		try {
			try {
				String fileName = String.format("%1$05d", id) + ".vnw";
				is = as.open(fileName);
				isr = new InputStreamReader(is, "SJIS");
				br = new BufferedReader(isr);

				String str;
				while ((str = br.readLine()) != null) {
					sb.append(str + "\n");
				}
			} finally {
				if (br != null)
					br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String json = sb.toString();
		try {
			JSONObject jsonObj = new JSONObject(json);
			this.cardType = jsonObj.getString("CardType");
			this.id = Integer.parseInt(jsonObj.getString("ID"));
			this.name = jsonObj.getString("Name");
			this.name = new String(this.name.getBytes(), "UTF-8");
			this.cost = Integer.parseInt(jsonObj.getString("COST"));
			this.ft = jsonObj.getString("FT");
			if (this.id < 20000) {
				this.type = Integer.parseInt(jsonObj.getString("Type"));
				this.atk = Integer.parseInt(jsonObj.getString("ATK"));
				this.hp = Integer.parseInt(jsonObj.getString("HP"));
				this.movRange = jsonObj.getString("MovRange");
				this.atkRange = jsonObj.getString("AtkRange");
			} else {
				this.effect = new Effect(jsonObj.getJSONArray("Effect"));
				this.hp = 1;
				this.movRange = jsonObj.getString("Range");
				this.atkRange = jsonObj.getString("Range");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getIconImgId(Context context) {
		String name = "ic" + String.format("%1$05d", this.id);
		int resId = context.getResources().getIdentifier(name, "drawable",
				context.getPackageName());
		return resId;
	}

	public static int getIconImgId(int id, Context context) {
		String name = "ic" + String.format("%1$05d", id);
		int resId = context.getResources().getIdentifier(name, "drawable",
				context.getPackageName());
		return resId;
	}

	public int getCardImgId(Context context) {
		String name = "no" + String.format("%1$05d", this.id);
		int resId = context.getResources().getIdentifier(name, "drawable",
				context.getPackageName());
		return resId;
	}

	public static int getCardImgId(int id, Context context) {
		String name = "no" + String.format("%1$05d", id);
		int resId = context.getResources().getIdentifier(name, "drawable",
				context.getPackageName());
		return resId;
	}

	// テスト用デッキ作成
	public static ArrayList<Card> createTestDeck(Context context) {
		ArrayList<Card> deck = new ArrayList<Card>();
		deck.add(new Card(1, context));
		deck.add(new Card(4, context));
		deck.add(new Card(6, context));
		deck.add(new Card(9, context));
		deck.add(new Card(13, context));
		deck.add(new Card(15, context));
		deck.add(new Card(16, context));
		deck.add(new Card(18, context));
		deck.add(new Card(20, context));
		deck.add(new Card(22, context));
		deck.add(new Card(23, context));
		deck.add(new Card(24, context));
		deck.add(new Card(29, context));
		deck.add(new Card(33, context));
		deck.add(new Card(38, context));
		deck.add(new Card(40, context));
		deck.add(new Card(42, context));
		deck.add(new Card(43, context));
		deck.add(new Card(44, context));
		deck.add(new Card(45, context));
		return deck;
	}
}
