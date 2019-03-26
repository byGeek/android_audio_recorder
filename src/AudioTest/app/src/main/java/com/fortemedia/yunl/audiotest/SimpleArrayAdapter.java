package com.fortemedia.yunl.audiotest;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SimpleArrayAdapter extends RecyclerView.Adapter<SimpleArrayAdapter.MyViewHolder> {

    public class MyViewHolder extends RecyclerView.ViewHolder{
        public TextView textView;
        public MyViewHolder(View v){
            super(v);
            textView = (TextView)v.findViewById(R.id.filename);
            v.setTag(this);
            v.setOnClickListener(mOnItemClickListener);
        }
    }


    private List<String> mDataset;
    private View.OnClickListener mOnItemClickListener;
    public SimpleArrayAdapter(List<String> dataset){
        mDataset = dataset;
    }

    public void setOnItemClickListener(View.OnClickListener listener){
        mOnItemClickListener = listener;
    }

    @Override
    public SimpleArrayAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_text_layout, parent, false);

        MyViewHolder viewHolder = new MyViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder viewHolder, int position){
        String filename = mDataset.get(position);

        TextView textView = viewHolder.textView;
        textView.setText(filename);
    }

    @Override
    public int getItemCount(){
        return mDataset.size();
    }
}
