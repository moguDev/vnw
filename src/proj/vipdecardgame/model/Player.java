package proj.vipdecardgame.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
	public static final int ME = 1, OPPONENT = 2;

	public String name;
	private int life, weed;
	private List<Card> deck, hand, grave;

	// コンストラクタ
	public Player(int lifeVal, ArrayList<Card> deck, int handVal, int weedVal) {
		// ライフ
		this.life = 0;
		this.addLife(lifeVal);
		// デッキ
		this.deck = deck;
		this.shuffleDeck();
		// 手札
		this.hand = new ArrayList<Card>();
		this.drawCard(handVal);
		// 草
		this.weed = 0;
		this.addWeed(weedVal);
		// 墓地
		this.grave = new ArrayList<Card>();
	}
	public Player(String playerName, int lifeVal, ArrayList<Card> deck, int handVal, int weedVal) {
		this.name = playerName;
		new Player(lifeVal, deck, handVal, weedVal);
	}

	// ライフ計算メソッド
	public void addLife(int value) {
		this.life += value;
		if (this.life < 0)
			this.life = 0;
	}

	public void damaged(int value) {
		this.life -= value;
		if (this.life < 0)
			this.life = 0;
	}

	// ドローメソッド
	public void drawCard(int n) {
		for (int i = 0; i < n; i++) {
			hand.add(deck.get(deck.size() - 1));
			deck.remove(deck.size() - 1);
		}
	}

	// デッキサイズを返す
	public int deckSize() {
		return this.deck.size();
	}

	// デッキシャッフルメソッド
	public void shuffleDeck() {
		Collections.shuffle(this.deck);
	}

	// 草追加メソッド
	public void addWeed(int value) {
		this.weed += value;
		if (this.weed < 0)
			this.weed = 0;
	}

	// 草追加メソッド
	public boolean payWeed(int value) {
		if (this.weed < value)
			return false;
		else {
			this.weed -= value;
			return true;
		}

	}

	// 墓地にカード追加メソッド
	public void addGrave(Card card) {
		this.grave.add(card);
	}

	public boolean isDead() {
		if (this.life <= 0)
			return true;
		else
			return false;
	}

	// 各フィールドゲッターメソッド
	public int getLife() {
		return this.life;
	}

	public int getWeed() {
		return this.weed;
	}

	public List<Card> getHand() {
		return this.hand;
	}

	public List<Card> getGrave() {
		return this.grave;
	}
}
