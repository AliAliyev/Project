package media.apis.android.example.packagecom.recorder;

import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Ali on 12/02/2017.
 */

public class TrackItemAdapter extends ArrayAdapter<TrackItem> implements View.OnClickListener{

    private Context mContext;
    private ArrayList<TrackItem> mTrackItems;

    // View lookup cache
    private static class ViewHolder {
        TextView sizeText;
        TextView lengthText;
        TextView nameText;
        TextView descriptionText;
        TextView wholeItem;
        TextView optionItem;
        ImageView option;
    }

    // Constructor
    public TrackItemAdapter(Context mContext, ArrayList<TrackItem> mTrackItems) {
        super(mContext, R.layout.tracks_item_view, mTrackItems);
        this.mContext = mContext;
        this.mTrackItems = mTrackItems;
    }

    @Override
    public void onClick(View v) {
        int position = (Integer) v.getTag();
        Object object = getItem(position);
        TrackItem trackItem = (TrackItem) object;

        switch (v.getId())
        {
            case R.id.trackTextView:
                Toast.makeText(mContext, "hallelujah", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private int lastPosition = -1;

    public View getView(final int position, View convertView, ViewGroup parent) {

        TrackItem trackItem = getItem(position);
        ViewHolder viewHolder;

        final View result;

        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.tracks_item_view, parent, false);
            viewHolder.nameText = (TextView) convertView.findViewById(R.id.trackTextView);
            viewHolder.descriptionText = (TextView) convertView.findViewById(R.id.track_description);
            viewHolder.lengthText = (TextView) convertView.findViewById(R.id.length);
            viewHolder.sizeText = (TextView) convertView.findViewById(R.id.size);
            viewHolder.wholeItem = (TextView) convertView.findViewById(R.id.wholeItem);
            viewHolder.optionItem = (TextView) convertView.findViewById(R.id.optionItem);
            viewHolder.option = (ImageView) convertView.findViewById(R.id.options);

            result = convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        lastPosition = position;

        if (trackItem != null) {
            viewHolder.nameText.setText(trackItem.getTrackName());
        }
        if (trackItem != null) {
            viewHolder.descriptionText.setText(trackItem.getTrackDescription());
        }
        if (trackItem != null) {
            viewHolder.lengthText.setText(trackItem.getTrackDuration());
        }
        if (trackItem != null) {
            viewHolder.sizeText.setText(trackItem.getTrackSize());
        }
        viewHolder.optionItem.setOnClickListener(this);
        viewHolder.optionItem.setTag(position);

        viewHolder.wholeItem.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                //onListItemClick(listView, v, position);
            }
        });

        return convertView;
    }
}
