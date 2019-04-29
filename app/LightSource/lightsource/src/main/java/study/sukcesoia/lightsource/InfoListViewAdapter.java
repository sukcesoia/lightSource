package study.sukcesoia.lightsource;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InfoListViewAdapter extends BaseAdapter {

    public static final String[][] ITEM_LIST = {
            {"BLE State", ""},
            {"Led PWM", ""},
            {"Voltage", "V"},
            {"Current", "A"},
            {"Power", "W"},
            {"Battery", ""},
            {"Temperature", "°C"},
    };

    private String itemValue[] = new String[ITEM_LIST.length];
    private LayoutInflater inflater;

    public static class ViewHolder {
        LinearLayout rlBorder;
        TextView mTextViewName;
        TextView mTextViewValue;
    }

    public InfoListViewAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
        for (int i = 0; i < itemValue.length; i++)
            itemValue[i] = "";
    }

    @Override
    public int getCount() {
        return ITEM_LIST.length;
    }

    @Override
    public Object getItem(int position) {
        return ITEM_LIST[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.style_listview_info, null);
            holder.mTextViewName = convertView.findViewById(R.id.tv_name);
            holder.mTextViewValue = convertView.findViewById(R.id.tv_value);
            holder.rlBorder = convertView.findViewById(R.id.llBorder);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mTextViewName.setText(ITEM_LIST[position][0]);
        holder.mTextViewValue.setText(itemValue[position] + "\t "+ ITEM_LIST[position][1]);
        return convertView;
    }

    public String[] setItemValue(int position, String value) {
        if (position < itemValue.length) {
            itemValue[position] = value;
            this.notifyDataSetChanged();
        }
        return itemValue;
    }
}
