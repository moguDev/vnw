package proj.vipdecardgame;

import java.util.List;

import proj.vipdecardgame.model.Card;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

public class CardGridAdapter extends ArrayAdapter<Card> {
	
	class ViewHolder {
		ImageView iv;
	}

	Context context;
	LayoutInflater inflater;

	CardGridAdapter(Context context, List<Card> items) {
		super(context, 0, items);
		this.context = context;
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Card item = getItem(position);

		ViewHolder holder;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.card_item, null);
			holder = new ViewHolder();
			holder.iv = (ImageView) convertView;
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.iv.setImageResource(Card.getIconImgId(item.id, context));

		return convertView;
	}
}
