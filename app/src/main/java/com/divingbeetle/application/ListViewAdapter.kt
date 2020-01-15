package com.divingbeetle.application

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class ListViewAdapter(
    private val context: Context,
    private val arrayList: ArrayList<HashMap<String, Any>>
) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view: View?
        val vh: ViewHolder

        if (convertView == null) {
            val layoutInflater = LayoutInflater.from(context)
            view = layoutInflater.inflate(R.layout.list_view_item, parent, false)
            vh = ViewHolder(view)
            view.tag = vh
        } else {
            view = convertView
            vh = view.tag as ViewHolder
        }

        vh.tvName.text = arrayList[position]["화장실명"].toString()

        vh.tvContent.text = if (arrayList[position]["남녀공용화장실여부"].toString() == "Y") {
            arrayList[position]["거리Text"].toString() + " (" + context.resources.getString(R.string.unisex_toilet) + ")"
        } else {
            arrayList[position]["거리Text"].toString()
        }

        vh.tvGoButton.setOnClickListener {
            val toiletDataMap = arrayList[position]
            val dLatitude: Double = toiletDataMap["위도"].toString().toDouble()
            val dLongitude: Double = toiletDataMap["경도"].toString().toDouble()

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?daddr=$dLatitude,$dLongitude")
            )

            intent.setClassName(
                "com.google.android.apps.maps",
                "com.google.android.maps.MapsActivity"
            )

            context.startActivity(intent)
        }

        return view
    }

    override fun getItem(position: Int): Any {
        return arrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return arrayList.size
    }
}

private class ViewHolder(view: View?) {
    val tvName: TextView = view?.findViewById(R.id.tvName) as TextView
    val tvContent: TextView = view?.findViewById(R.id.tvContent) as TextView
    val tvGoButton: TextView = view?.findViewById(R.id.tvGo) as TextView
}