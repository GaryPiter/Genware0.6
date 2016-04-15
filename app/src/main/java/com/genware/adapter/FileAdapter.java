package com.genware.adapter;

import java.io.File;
import java.util.ArrayList;

import com.genware.activity.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private Bitmap pic_file_folder, pic_file_icon,pic_file_music,pic_file_viode,pic_file_text,pic_file_unknown;
	// 存储文件名称
	private ArrayList<String> names = null;
	// 存储文件路径
	private ArrayList<String> paths = null;

	// 参数初始化
	public FileAdapter(Context context, ArrayList<String> na, ArrayList<String> pa) {
		names = na;
		Log.i("names", names.toString());
		paths = pa;
		pic_file_folder = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.pic_file_folder);
		pic_file_icon = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.pic_file_icon);
		pic_file_music = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.pic_file_music);
		pic_file_viode = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.pic_file_viode);
		pic_file_text = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.pic_file_text);
		pic_file_unknown = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.pic_file_unknown);
		// 缩小图片
		pic_file_folder = small(pic_file_folder, 0.24f);
		pic_file_icon = small(pic_file_icon, 0.2f);
		pic_file_music = small(pic_file_music, 0.2f);
		pic_file_viode = small(pic_file_viode, 0.25f);
		pic_file_text = small(pic_file_text, 0.1f);
		pic_file_unknown = small(pic_file_unknown, 0.5f);
		inflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return names.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return names.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder holder;
		if (null == convertView) {
			convertView = inflater.inflate(R.layout.file_item, null);
			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.file_name);
			holder.image = (ImageView) convertView.findViewById(R.id.file_image);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		File f = new File(paths.get(position).toString());
		if (names.get(position).equals("@1")) {
			holder.text.setText("返回主目录");
			holder.image.setImageResource(android.R.drawable.ic_media_previous);
		} else if (names.get(position).equals("@2")) {
			holder.text.setText("上一级");
			holder.image.setImageResource(android.R.drawable.ic_media_previous);
		} else {
			holder.text.setText(f.getName());
			if (f.isDirectory()) {
				holder.image.setImageBitmap(pic_file_folder);
			} else if (f.isFile()) {
				String name = f.getName();
				// 文件扩展名
				String end = name.substring(name.lastIndexOf(".") + 1,
						name.length()).toLowerCase();
				if (end.equals("m4a") || end.equals("mp3") || end.equals("wav")) {
					holder.image.setImageBitmap(pic_file_music);
				} else if (end.equals("mp4") || end.equals("3gp")) {
					holder.image.setImageBitmap(pic_file_viode);
				} else if (end.equals("jpg") || end.equals("png")
						|| end.equals("jpeg") || end.equals("bmp")
						|| end.equals("gif")) {
					holder.image.setImageBitmap(pic_file_icon);
				} else if (end.equals("txt") || end.equals("so")) {
					holder.image.setImageBitmap(pic_file_text);
				} else {
					// 如果无法直接打开，跳出列表由用户选择
					holder.image.setImageBitmap(pic_file_unknown);
				}
			} else {
				System.out.println(f.getName());
			}
		}
		return convertView;
	}

	private class ViewHolder {
		private TextView text;
		private ImageView image;
	}

	private Bitmap small(Bitmap map, float num) {
		Matrix matrix = new Matrix();
		matrix.postScale(num, num);
		return Bitmap.createBitmap(map, 0, 0, map.getWidth(), map.getHeight(),
				matrix, true);
	}
}
